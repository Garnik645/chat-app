import { useState } from 'react'
import Sidebar from './Sidebar'
import ChatRoom from './ChatRoom'

export default function ChatPage() {
  const [selectedRoom, setSelectedRoom] = useState(null)

  function handleRoomDeleted() {
    setSelectedRoom(null)
  }

  return (
    <div style={{
      display: 'flex',
      height: '100%',
      overflow: 'hidden',
    }}>
      <Sidebar
        selectedRoomId={selectedRoom?.id}
        onSelectRoom={setSelectedRoom}
      />

      <main style={{ flex: 1, overflow: 'hidden', display: 'flex' }}>
        {selectedRoom ? (
          <ChatRoom
            key={selectedRoom.id}
            room={selectedRoom}
            onRoomDeleted={handleRoomDeleted}
          />
        ) : (
          <EmptyState />
        )}
      </main>
    </div>
  )
}

function EmptyState() {
  return (
    <div style={{
      flex: 1,
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      gap: 12,
      background: 'var(--bg-0)',
      position: 'relative',
      overflow: 'hidden',
    }}>
      {/* Grid background */}
      <div style={{
        position: 'absolute', inset: 0, pointerEvents: 'none',
        backgroundImage: `
          linear-gradient(var(--border) 1px, transparent 1px),
          linear-gradient(90deg, var(--border) 1px, transparent 1px)
        `,
        backgroundSize: '48px 48px',
        opacity: 0.2,
      }} />

      {/* Center glow */}
      <div style={{
        position: 'absolute',
        top: '50%', left: '50%',
        transform: 'translate(-50%, -50%)',
        width: 400, height: 400,
        borderRadius: '50%',
        background: 'radial-gradient(circle, rgba(99,102,241,0.06) 0%, transparent 70%)',
        pointerEvents: 'none',
      }} />

      <div style={{
        position: 'relative',
        textAlign: 'center',
        maxWidth: 340,
      }}>
        <div style={{
          width: 56, height: 56,
          background: 'var(--accent-dim)',
          border: '1px solid rgba(99,102,241,0.3)',
          borderRadius: 14,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 20px',
        }}>
          <svg width="26" height="26" viewBox="0 0 26 26" fill="none">
            <path d="M3 5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v13a2 2 0 0 1-2 2H8l-5 5V5z"
              fill="var(--accent)" opacity="0.85" />
          </svg>
        </div>

        <h2 style={{
          fontFamily: 'var(--font-brand)',
          fontStyle: 'italic',
          fontWeight: 300,
          fontSize: 22,
          color: 'var(--text-0)',
          marginBottom: 8,
        }}>
          Select a room to start
        </h2>
        <p style={{
          fontSize: 14,
          color: 'var(--text-2)',
          lineHeight: 1.7,
        }}>
          Choose a room from the sidebar or create a new one.
          Messages are delivered in real time and automatically moderated.
        </p>

        <div style={{
          marginTop: 28,
          display: 'flex',
          gap: 20,
          justifyContent: 'center',
          flexWrap: 'wrap',
        }}>
          {[
            { label: 'Real-time', desc: 'WebSocket delivery' },
            { label: 'Moderated', desc: 'ML content filter' },
            { label: 'Paginated', desc: 'Full history' },
          ].map((f) => (
            <div key={f.label} style={{ textAlign: 'center' }}>
              <p style={{ fontSize: 13, fontWeight: 500, color: 'var(--text-1)' }}>{f.label}</p>
              <p style={{ fontSize: 11, color: 'var(--text-2)', marginTop: 2 }}>{f.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
