import { useState } from 'react'
import { createRoom, joinRoom } from '../api'
import { useToast } from '../App'

export default function CreateRoomModal({ onClose, onCreated }) {
  const showToast = useToast()
  const [values, setValues] = useState({ name: '', description: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  const set = (field) => (e) =>
    setValues((v) => ({ ...v, [field]: e.target.value }))

  function validate() {
    const errs = {}
    if (!values.name.trim()) errs.name = 'Room name is required'
    else if (values.name.length > 80) errs.name = 'Max 80 characters'
    if (values.description.length > 255) errs.description = 'Max 255 characters'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setLoading(true)
    try {
      const room = await createRoom({ name: values.name.trim(), description: values.description.trim() })
      // Auto-join after creation
      try { await joinRoom(room.id) } catch { /* may already be member */ }
      showToast(`Room "${room.name}" created`, 'success')
      onCreated(room)
    } catch (err) {
      showToast(err.message || 'Failed to create room', 'error')
    } finally {
      setLoading(false)
    }
  }

  function handleOverlayClick(e) {
    if (e.target === e.currentTarget) onClose()
  }

  return (
    <div className="modal-overlay" onClick={handleOverlayClick}>
      <div className="modal" role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="modal-header">
          <span id="modal-title" className="modal-title">Create room</span>
          <button className="modal-close" onClick={onClose} aria-label="Close">×</button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="field">
            <label htmlFor="room-name">Room name</label>
            <input
              id="room-name"
              type="text"
              placeholder="e.g. general"
              value={values.name}
              onChange={set('name')}
              className={errors.name ? 'error' : ''}
              autoFocus
            />
            {errors.name && (
              <span style={{ fontSize: 12, color: 'var(--red)' }}>{errors.name}</span>
            )}
          </div>

          <div className="field">
            <label htmlFor="room-desc">Description <span style={{ color: 'var(--text-2)', fontWeight: 400 }}>(optional)</span></label>
            <input
              id="room-desc"
              type="text"
              placeholder="What's this room about?"
              value={values.description}
              onChange={set('description')}
              className={errors.description ? 'error' : ''}
            />
            {errors.description && (
              <span style={{ fontSize: 12, color: 'var(--red)' }}>{errors.description}</span>
            )}
          </div>

          <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
            <button
              type="button"
              className="btn btn-ghost"
              style={{ flex: 1 }}
              onClick={onClose}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              style={{ flex: 1 }}
              disabled={loading}
            >
              {loading ? <span className="spinner" style={{ width: 16, height: 16 }} /> : 'Create room'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
