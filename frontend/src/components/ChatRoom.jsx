import { useState, useEffect, useRef, useCallback } from 'react'
import { getMessages, leaveRoom, deleteRoom } from '../api'
import { useAuth, useToast } from '../App'

// ── Format timestamp ──────────────────────────────────────────────────────

function formatTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  const now = new Date()
  const isToday =
    d.getDate() === now.getDate() &&
    d.getMonth() === now.getMonth() &&
    d.getFullYear() === now.getFullYear()
  if (isToday) {
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' }) +
    ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// ── Normalization Helper ──────────────────────────────────────────────────
const normalizeMessage = (m) => ({
  id: m.messageId || m.id,
  userId: m.userId,
  username: m.username,
  content: m.content,
  timestamp: m.sentAt || m.timestamp,
  removed: m.status === 'REMOVED' || m.removed || false,
  type: m.type || 'MESSAGE'
});

// ── Message bubble ────────────────────────────────────────────────────────

function Message({ msg, isOwn, prevMsg }) {
  const showHeader =
    !prevMsg ||
    prevMsg.userId !== msg.userId ||
    new Date(msg.timestamp) - new Date(prevMsg.timestamp) > 5 * 60 * 1000

  if (msg.removed) {
    return (
      <div style={{ padding: '3px 20px', display: 'flex', alignItems: 'center', gap: 8, opacity: 0.5 }}>
        <span style={{ fontSize: 11, color: 'var(--text-2)', fontStyle: 'italic' }}>
          ⚑ Message removed by moderation
        </span>
      </div>
    )
  }

  if (msg.type === 'system') {
    return (
      <div style={{ padding: '6px 20px', textAlign: 'center', fontSize: 12, color: 'var(--text-2)', fontStyle: 'italic' }}>
        {msg.content}
      </div>
    )
  }

  const initials = (msg.username ?? 'U')
    .split(/[_\s]/).map((s) => s[0]?.toUpperCase() ?? '').join('').slice(0, 2)

  const avatarColor = stringToColor(msg.userId ?? msg.username ?? '')

  return (
    <div style={{ padding: showHeader ? '12px 20px 3px' : '2px 20px', display: 'flex', gap: 10, alignItems: 'flex-start' }}>
      <div style={{ width: 32, flexShrink: 0, paddingTop: showHeader ? 2 : 0 }}>
        {showHeader && (
          <div style={{
            width: 32, height: 32, borderRadius: '50%',
            background: avatarColor.bg,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 12, fontWeight: 500, color: avatarColor.text,
          }}>
            {initials}
          </div>
        )}
      </div>

      <div style={{ flex: 1, minWidth: 0 }}>
        {showHeader && (
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 4 }}>
            <span style={{ fontSize: 13, fontWeight: 500, color: isOwn ? 'var(--accent)' : 'var(--text-0)' }}>
              {msg.username ?? 'Unknown'}
            </span>
            <span style={{ fontSize: 11, color: 'var(--text-2)' }}>
              {formatTime(msg.timestamp)}
            </span>
          </div>
        )}
        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 13, lineHeight: 1.6, color: 'var(--text-0)', wordBreak: 'break-word', whiteSpace: 'pre-wrap' }}>
          {msg.content}
        </p>
      </div>
    </div>
  )
}

const AVATAR_PALETTES = [
  { bg: 'rgba(99,102,241,0.2)',  text: '#818cf8' },
  { bg: 'rgba(52,211,153,0.15)', text: '#34d399' },
  { bg: 'rgba(251,191,36,0.15)', text: '#fbbf24' },
  { bg: 'rgba(248,113,113,0.15)',text: '#f87171' },
  { bg: 'rgba(167,139,250,0.15)',text: '#c4b5fd' },
  { bg: 'rgba(56,189,248,0.15)', text: '#38bdf8' },
]

function stringToColor(s) {
  let h = 0
  for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h)
  return AVATAR_PALETTES[Math.abs(h) % AVATAR_PALETTES.length]
}

function WsStatus({ status }) {
  const map = {
    connecting: { color: 'var(--yellow)', label: 'Connecting…' },
    connected:  { color: 'var(--green)',  label: 'Connected'   },
    error:      { color: 'var(--red)',    label: 'Disconnected' },
  }
  const s = map[status] ?? map.error
  return (
    <span style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12, color: 'var(--text-2)' }}>
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: s.color, display: 'inline-block' }} />
      {s.label}
    </span>
  )
}

export default function ChatRoom({ room, onRoomDeleted }) {
  const { user } = useAuth()
  const showToast = useToast()

  const [messages, setMessages] = useState([])
  const [historyLoading, setHistoryLoading] = useState(true)
  const [input, setInput] = useState('')
  const [wsStatus, setWsStatus] = useState('connecting')
  const [sending, setSending] = useState(false)
  const [showMenu, setShowMenu] = useState(false)

  const wsRef = useRef(null)
  const listRef = useRef(null)
  const reconnectTimerRef = useRef(null)
  const reconnectAttempts = useRef(0)

  // ── Load history ─────────────────────────────────────────────────────

  useEffect(() => {
    setHistoryLoading(true)
    setMessages([])
    getMessages(room.id)
      .then((data) => {
        const list = data.content || []
        // REVERSED: We reverse the backend list so the newest items are at the end
        const chronList = [...list].reverse().map(normalizeMessage)
        setMessages(chronList)
      })
      .catch((err) => showToast(err.message || 'Could not load history', 'error'))
      .finally(() => setHistoryLoading(false))
  }, [room.id, showToast])

  // ── WebSocket ─────────────────────────────────────────────────────────

  const connectWs = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.onclose = null
      wsRef.current.close()
    }

    setWsStatus('connecting')
    const ws = new WebSocket(
      (() => {
        const proto = location.protocol === 'https:' ? 'wss:' : 'ws:'
        return `${proto}//${location.host}/ws/rooms/${encodeURIComponent(room.id)}?token=${encodeURIComponent(user.token)}`
      })()
    )

    ws.onopen = () => {
      setWsStatus('connected')
      reconnectAttempts.current = 0
    }

    ws.onmessage = (event) => {
      let raw;
      try { raw = JSON.parse(event.data); } catch { return; }

      if (raw.type === 'ERROR') {
        showToast(raw.message || 'Socket error', 'error');
        return;
      }

      if (raw.type === 'MESSAGE_REMOVED') {
        setMessages((prev) =>
          prev.map((m) => (m.id === raw.messageId ? { ...m, removed: true } : m))
        );
        return;
      }

      const msg = normalizeMessage(raw);
      if (msg.content && msg.id) {
        setMessages((prev) => {
          if (prev.some((m) => m.id === msg.id)) return prev;
          // Add to the end (bottom) of the array
          return [...prev, msg];
        });
      }
    }

    ws.onerror = () => setWsStatus('error')

    ws.onclose = () => {
      setWsStatus('error')
      const delay = Math.min(1000 * 2 ** reconnectAttempts.current, 30000)
      reconnectAttempts.current += 1
      reconnectTimerRef.current = setTimeout(connectWs, delay)
    }

    wsRef.current = ws
  }, [room.id, user.token, showToast])

  useEffect(() => {
    connectWs()
    return () => {
      clearTimeout(reconnectTimerRef.current)
      if (wsRef.current) {
        wsRef.current.onclose = null
        wsRef.current.close()
      }
    }
  }, [connectWs])

  // ── Auto-scroll ───────────────────────────────────────────────────────

  useEffect(() => {
    const el = listRef.current
    if (!el) return
    // Auto-scroll to the bottom whenever a new message is added
    el.scrollTop = el.scrollHeight
  }, [messages])

  // ── Send ──────────────────────────────────────────────────────────────

  function sendMessage() {
    const content = input.trim()
    if (!content || sending) return
    const ws = wsRef.current
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      showToast('Not connected — trying to reconnect', 'error')
      return
    }
    setSending(true)
    ws.send(JSON.stringify({ content }))
    setInput('')
    setSending(false)
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      sendMessage()
    }
  }

  async function handleLeave() {
    setShowMenu(false)
    try {
      await leaveRoom(room.id)
      showToast(`Left ${room.name}`, 'success')
      onRoomDeleted?.()
    } catch (err) {
      showToast(err.message || 'Could not leave room', 'error')
    }
  }

  async function handleDelete() {
    setShowMenu(false)
    if (!confirm(`Delete room "${room.name}"? This cannot be undone.`)) return
    try {
      await deleteRoom(room.id)
      showToast(`Room "${room.name}" deleted`, 'success')
      onRoomDeleted?.()
    } catch (err) {
      showToast(err.message || 'Could not delete room', 'error')
    }
  }

  const isAdmin = room.createdBy === user.id || room.created_by === user.id

  return (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', background: 'var(--bg-0)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '0 20px', height: 52, borderBottom: '1px solid var(--border)', flexShrink: 0, background: 'var(--bg-1)' }}>
        <svg width="16" height="16" viewBox="0 0 16 16" fill="none" style={{ color: 'var(--text-2)' }}>
          <path d="M2 6h12M2 10h12M6.5 1.5l-1 13M10.5 1.5l-1 13" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
        </svg>
        <div style={{ flex: 1 }}>
          <span style={{ fontWeight: 500, fontSize: 15, color: 'var(--text-0)' }}>{room.name}</span>
          {room.description && <span style={{ color: 'var(--text-2)', fontSize: 12, marginLeft: 12 }}>{room.description}</span>}
        </div>
        <WsStatus status={wsStatus} />
        <div style={{ position: 'relative' }}>
          <button onClick={() => setShowMenu(!showMenu)} style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', width: 30, height: 30, borderRadius: 6, color: 'var(--text-2)', transition: 'all 0.12s' }}>
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <circle cx="8" cy="4" r="1.2" /><circle cx="8" cy="8" r="1.2" /><circle cx="8" cy="12" r="1.2" />
            </svg>
          </button>
          {showMenu && (
            <>
              <div style={{ position: 'fixed', inset: 0, zIndex: 99 }} onClick={() => setShowMenu(false)} />
              <div style={{ position: 'absolute', top: '100%', right: 0, marginTop: 4, width: 160, background: 'var(--bg-2)', border: '1px solid var(--border-2)', borderRadius: 8, padding: 6, zIndex: 100, boxShadow: '0 8px 24px rgba(0,0,0,0.4)' }}>
                <button onClick={handleLeave} style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%', padding: '7px 10px', borderRadius: 5, fontSize: 13, color: 'var(--text-1)', textAlign: 'left' }}>Leave room</button>
                {isAdmin && <button onClick={handleDelete} style={{ display: 'flex', alignItems: 'center', gap: 8, width: '100%', padding: '7px 10px', borderRadius: 5, fontSize: 13, color: 'var(--red)', textAlign: 'left' }}>Delete room</button>}
              </div>
            </>
          )}
        </div>
      </div>

      <div ref={listRef} style={{ flex: 1, overflowY: 'auto', padding: '12px 0' }}>
        {historyLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 40 }}><span className="spinner" /></div>
        ) : messages.length === 0 ? (
          <div style={{ textAlign: 'center', padding: 48 }}>
            <div style={{ fontSize: 28, marginBottom: 12 }}>💬</div>
            <p style={{ color: 'var(--text-1)', fontSize: 15, fontWeight: 500 }}>Start the conversation</p>
          </div>
        ) : (
          messages.map((msg, i) => <Message key={msg.id ?? i} msg={msg} isOwn={msg.userId === user.id} prevMsg={messages[i - 1]} />)
        )}
      </div>

      <div style={{ padding: '12px 16px', borderTop: '1px solid var(--border)', flexShrink: 0, background: 'var(--bg-1)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--bg-3)', border: '1px solid var(--border-2)', borderRadius: 8, padding: '0 12px', minHeight: 44 }}>
          <input type="text" value={input} onChange={(e) => setInput(e.target.value)} onKeyDown={handleKeyDown} placeholder={`Message #${room.name}`} style={{ flex: 1, fontFamily: 'var(--font-mono)', fontSize: 13, padding: '10px 0' }} disabled={wsStatus !== 'connected'} />
          <button onClick={sendMessage} disabled={!input.trim() || wsStatus !== 'connected' || sending} style={{ width: 30, height: 30, borderRadius: 6, background: input.trim() && wsStatus === 'connected' ? 'var(--accent-2)' : 'transparent', color: input.trim() && wsStatus === 'connected' ? '#fff' : 'var(--text-2)' }}>
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M12 7H2M8 3l4 4-4 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
          </button>
        </div>
      </div>
    </div>
  )
}