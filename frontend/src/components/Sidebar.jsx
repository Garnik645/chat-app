import { useState, useEffect, useCallback } from 'react'
import { getRooms, joinRoom } from '../api'
import { useAuth, useToast } from '../App'
import CreateRoomModal from './CreateRoomModal'
import ProfileModal from './ProfileModal'

// ── Icons ─────────────────────────────────────────────────────────────────

function HashIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" style={{ flexShrink: 0 }}>
      <path d="M2 5h10M2 9h10M5.5 1.5l-1 11M9.5 1.5l-1 11"
        stroke="currentColor" strokeWidth="1.4" strokeLinecap="round" />
    </svg>
  )
}

function PlusIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <path d="M7 2v10M2 7h10"
        stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  )
}

function SearchIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <circle cx="6" cy="6" r="4" stroke="currentColor" strokeWidth="1.3" />
      <path d="M9.5 9.5l2.5 2.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

function LogoutIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
      <path d="M5.5 2H3a1 1 0 0 0-1 1v8a1 1 0 0 0 1 1h2.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
      <path d="M9 4.5l2.5 2.5-2.5 2.5M11.5 7H5.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

// ── Room Item ─────────────────────────────────────────────────────────────

function RoomItem({ room, isActive, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 8,
        width: '100%',
        padding: '7px 10px',
        borderRadius: 6,
        textAlign: 'left',
        color: isActive ? 'var(--text-0)' : 'var(--text-1)',
        background: isActive ? 'var(--bg-4)' : 'transparent',
        transition: 'all 0.12s',
      }}
      onMouseEnter={(e) => {
        if (!isActive) e.currentTarget.style.background = 'var(--bg-3)'
      }}
      onMouseLeave={(e) => {
        if (!isActive) e.currentTarget.style.background = 'transparent'
      }}
    >
      <span style={{ color: isActive ? 'var(--accent)' : 'var(--text-2)' }}>
        <HashIcon />
      </span>
      <span style={{
        flex: 1,
        overflow: 'hidden',
        textOverflow: 'ellipsis',
        whiteSpace: 'nowrap',
        fontSize: 14,
        fontWeight: isActive ? 500 : 400,
      }}>
        {room.name}
      </span>
      {room.memberCount != null && (
        <span style={{ fontSize: 11, color: 'var(--text-2)', flexShrink: 0 }}>
          {room.memberCount}
        </span>
      )}
    </button>
  )
}

// ── Sidebar ───────────────────────────────────────────────────────────────

export default function Sidebar({ selectedRoomId, onSelectRoom }) {
  const { user, logout } = useAuth()
  const showToast = useToast()

  const [rooms, setRooms] = useState([])
  const [loading, setLoading] = useState(true)
  const [query, setQuery] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [showProfile, setShowProfile] = useState(false)
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)

  const fetchRooms = useCallback(async (p = 0, append = false) => {
    setLoading(true)
    try {
      const data = await getRooms(p, 30)
      // Handles both paginated (Spring Page) and plain array responses
      const list = Array.isArray(data) ? data : (data.content ?? [])
      const total = data.totalPages ?? 1
      setRooms((prev) => append ? [...prev, ...list] : list)
      setHasMore(p + 1 < total)
      setPage(p)
    } catch (err) {
      showToast(err.message || 'Failed to load rooms', 'error')
    } finally {
      setLoading(false)
    }
  }, [showToast])

  useEffect(() => { fetchRooms(0) }, [fetchRooms])

  async function handleJoin(room) {
    try {
      await joinRoom(room.id)
    } catch {
      // Ignore — may already be a member
    }
    onSelectRoom(room)
  }

  function handleRoomCreated(room) {
    setShowCreate(false)
    setRooms((prev) => [room, ...prev])
    onSelectRoom(room)
  }

  const filtered = rooms.filter((r) =>
    r.name?.toLowerCase().includes(query.toLowerCase())
  )

  const initials = (user?.username ?? 'U')
    .split('_').map((s) => s[0]?.toUpperCase() ?? '').join('').slice(0, 2)

  return (
    <>
      <aside style={{
        width: 'var(--sidebar-w)',
        flexShrink: 0,
        height: '100%',
        background: 'var(--bg-1)',
        borderRight: '1px solid var(--border)',
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
      }}>
        {/* Header */}
        <div style={{
          padding: '16px 14px 12px',
          borderBottom: '1px solid var(--border)',
          flexShrink: 0,
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            marginBottom: 12,
          }}>
            <div style={{
              width: 28, height: 28,
              background: 'var(--accent-dim)',
              border: '1px solid rgba(99,102,241,0.25)',
              borderRadius: 6,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M1 3a1 1 0 0 1 1-1h10a1 1 0 0 1 1 1v7a1 1 0 0 1-1 1H4l-3 2V3z"
                  fill="var(--accent)" />
              </svg>
            </div>
            <span style={{
              fontFamily: 'var(--font-brand)',
              fontStyle: 'italic',
              fontSize: 15,
              fontWeight: 300,
              color: 'var(--text-0)',
            }}>
              ChatApp
            </span>
          </div>

          {/* Search */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            background: 'var(--bg-3)',
            border: '1px solid var(--border)',
            borderRadius: 6,
            padding: '0 10px',
            height: 32,
          }}>
            <span style={{ color: 'var(--text-2)' }}><SearchIcon /></span>
            <input
              type="search"
              placeholder="Search rooms…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              style={{ flex: 1, fontSize: 13 }}
            />
          </div>
        </div>

        {/* Rooms section */}
        <div style={{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 14px 4px',
            flexShrink: 0,
          }}>
            <span style={{ fontSize: 11, fontWeight: 500, color: 'var(--text-2)', letterSpacing: '0.07em', textTransform: 'uppercase' }}>
              Rooms
            </span>
            <button
              onClick={() => setShowCreate(true)}
              title="Create room"
              style={{
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                width: 22, height: 22,
                borderRadius: 4,
                color: 'var(--text-2)',
                transition: 'all 0.12s',
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--bg-3)'
                e.currentTarget.style.color = 'var(--text-0)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent'
                e.currentTarget.style.color = 'var(--text-2)'
              }}
            >
              <PlusIcon />
            </button>
          </div>

          {/* Room list */}
          <div style={{ flex: 1, overflowY: 'auto', padding: '0 8px 8px' }}>
            {loading && rooms.length === 0 ? (
              <div style={{ display: 'flex', justifyContent: 'center', padding: '24px 0' }}>
                <span className="spinner" />
              </div>
            ) : filtered.length === 0 ? (
              <p style={{ color: 'var(--text-2)', fontSize: 13, padding: '12px 6px', textAlign: 'center' }}>
                {query ? 'No rooms match' : 'No rooms yet'}
              </p>
            ) : (
              filtered.map((room) => (
                <RoomItem
                  key={room.id}
                  room={room}
                  isActive={room.id === selectedRoomId}
                  onClick={() => handleJoin(room)}
                />
              ))
            )}

            {hasMore && !query && (
              <button
                onClick={() => fetchRooms(page + 1, true)}
                className="btn btn-ghost"
                style={{ width: '100%', height: 32, fontSize: 12, marginTop: 4 }}
              >
                Load more
              </button>
            )}
          </div>
        </div>

        {/* User footer */}
        <div style={{
          borderTop: '1px solid var(--border)',
          padding: '10px 12px',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
          flexShrink: 0,
        }}>
          <div style={{
            width: 30, height: 30,
            borderRadius: '50%',
            background: 'var(--accent-dim)',
            border: '1px solid rgba(99,102,241,0.3)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 11, fontWeight: 500, color: 'var(--accent)',
            flexShrink: 0,
          }}>
            {initials}
          </div>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <p style={{
              fontSize: 13, fontWeight: 500,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              color: 'var(--text-0)',
            }}>
              {user?.username}
            </p>
            <span className="badge badge-green" style={{ fontSize: 10 }}>online</span>
          </div>
          <button
            onClick={() => setShowProfile(true)}
            title="View profile"
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              width: 28, height: 28,
              borderRadius: 6,
              color: 'var(--text-2)',
              transition: 'all 0.12s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'var(--bg-3)'
              e.currentTarget.style.color = 'var(--text-0)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent'
              e.currentTarget.style.color = 'var(--text-2)'
            }}
          >
            {/* Person icon */}
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <circle cx="7" cy="4.5" r="2.5" stroke="currentColor" strokeWidth="1.3" />
              <path d="M1.5 13c0-2.485 2.462-4.5 5.5-4.5s5.5 2.015 5.5 4.5" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
            </svg>
          </button>
          <button
            onClick={logout}
            title="Sign out"
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              width: 28, height: 28,
              borderRadius: 6,
              color: 'var(--text-2)',
              transition: 'all 0.12s',
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.background = 'var(--bg-3)'
              e.currentTarget.style.color = 'var(--red)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.background = 'transparent'
              e.currentTarget.style.color = 'var(--text-2)'
            }}
          >
            <LogoutIcon />
          </button>
        </div>
      </aside>

      {showCreate && (
        <CreateRoomModal
          onClose={() => setShowCreate(false)}
          onCreated={handleRoomCreated}
        />
      )}

      {showProfile && (
        <ProfileModal onClose={() => setShowProfile(false)} />
      )}
    </>
  )
}
