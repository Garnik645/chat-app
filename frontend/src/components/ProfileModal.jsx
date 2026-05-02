import { useState, useEffect } from 'react'
import { getMe } from '../api'

function Field({ label, value }) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      gap: 4,
      padding: '12px 0',
      borderBottom: '1px solid var(--border)',
    }}>
      <span style={{ fontSize: 11, fontWeight: 500, color: 'var(--text-2)', letterSpacing: '0.06em', textTransform: 'uppercase' }}>
        {label}
      </span>
      <span style={{ fontSize: 14, color: 'var(--text-0)' }}>
        {value ?? <span style={{ color: 'var(--text-2)', fontStyle: 'italic' }}>—</span>}
      </span>
    </div>
  )
}

function formatDate(instant) {
  if (!instant) return null
  return new Date(instant).toLocaleDateString([], {
    year: 'numeric', month: 'long', day: 'numeric',
  })
}

export default function ProfileModal({ onClose }) {
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    getMe()
      .then(setProfile)
      .catch((err) => setError(err.message || 'Failed to load profile'))
      .finally(() => setLoading(false))
  }, [])

  const initials = profile
    ? ((profile.firstName?.[0] ?? '') + (profile.lastName?.[0] ?? '')).toUpperCase()
    : '?'

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="profile-title">
        <div className="modal-header">
          <span id="profile-title" className="modal-title">My profile</span>
          <button className="modal-close" onClick={onClose} aria-label="Close">×</button>
        </div>

        {loading && (
          <div style={{ display: 'flex', justifyContent: 'center', padding: '32px 0' }}>
            <span className="spinner" />
          </div>
        )}

        {error && (
          <div style={{
            background: 'var(--red-dim)',
            border: '1px solid rgba(248,113,113,0.2)',
            borderRadius: 'var(--radius)',
            padding: '10px 14px',
            fontSize: 13,
            color: 'var(--red)',
          }}>
            {error}
          </div>
        )}

        {profile && (
          <>
            {/* Avatar + name header */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 14,
              marginBottom: 8,
              padding: '4px 0 16px',
              borderBottom: '1px solid var(--border)',
            }}>
              <div style={{
                width: 48, height: 48,
                borderRadius: '50%',
                background: 'var(--accent-dim)',
                border: '1px solid rgba(99,102,241,0.3)',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 17, fontWeight: 500, color: 'var(--accent)',
                flexShrink: 0,
              }}>
                {initials}
              </div>
              <div>
                <p style={{ fontSize: 16, fontWeight: 500, color: 'var(--text-0)' }}>
                  {profile.firstName} {profile.lastName}
                </p>
                <p style={{ fontSize: 13, color: 'var(--text-2)', marginTop: 2 }}>
                  @{profile.username}
                </p>
              </div>
            </div>

            {/* Fields */}
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              <Field label="User ID"     value={profile.id} />
              <Field label="Username"    value={profile.username} />
              <Field label="First name"  value={profile.firstName} />
              <Field label="Last name"   value={profile.lastName} />
              <Field label="Member since" value={formatDate(profile.createdAt)} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}
