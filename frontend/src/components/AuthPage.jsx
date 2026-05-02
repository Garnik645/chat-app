import { useState } from 'react'
import { useAuth, useToast } from '../App'
import { login as apiLogin, register as apiRegister } from '../api'

// ── Login Form ────────────────────────────────────────────────────────────

function LoginForm({ onSwitch }) {
  const { login } = useAuth()
  const showToast = useToast()
  const [values, setValues] = useState({ username: '', password: '' })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)

  const set = (field) => (e) =>
    setValues((v) => ({ ...v, [field]: e.target.value }))

  function validate() {
    const errs = {}
    if (!values.username.trim()) errs.username = 'Required'
    if (!values.password) errs.password = 'Required'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setLoading(true)
    try {
      const data = await apiLogin(values.username, values.password)
      // Expect: { token, userId, username } — adjust to match your User Service response
      login({
        token:    data.token,
        id:       data.userId ?? data.id,
        username: data.username ?? values.username,
      })
    } catch (err) {
      showToast(err.message || 'Login failed', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div className="field">
        <label htmlFor="login-username">Username</label>
        <input
          id="login-username"
          type="text"
          autoComplete="username"
          placeholder="your_handle"
          value={values.username}
          onChange={set('username')}
          className={errors.username ? 'error' : ''}
        />
        {errors.username && <FieldError>{errors.username}</FieldError>}
      </div>

      <div className="field">
        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          type="password"
          autoComplete="current-password"
          placeholder="••••••••"
          value={values.password}
          onChange={set('password')}
          className={errors.password ? 'error' : ''}
        />
        {errors.password && <FieldError>{errors.password}</FieldError>}
      </div>

      <button
        type="submit"
        className="btn btn-primary"
        style={{ width: '100%', marginTop: 4 }}
        disabled={loading}
      >
        {loading ? <span className="spinner" /> : 'Sign in'}
      </button>

      <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--text-2)' }}>
        No account?{' '}
        <button
          type="button"
          onClick={onSwitch}
          style={{ color: 'var(--accent)', background: 'none', fontSize: 13 }}
        >
          Register
        </button>
      </p>
    </form>
  )
}

// ── Register Form ─────────────────────────────────────────────────────────

function RegisterForm({ onSwitch }) {
  const showToast = useToast()
  const [values, setValues] = useState({
    username: '', email: '', password: '', firstName: '', lastName: '',
  })
  const [errors, setErrors] = useState({})
  const [loading, setLoading] = useState(false)
  const [success, setSuccess] = useState(false)

  const set = (field) => (e) =>
    setValues((v) => ({ ...v, [field]: e.target.value }))

  function validate() {
    const errs = {}
    if (!values.username.trim()) errs.username = 'Required'
    else if (values.username.length < 3 || values.username.length > 50)
      errs.username = 'Must be 3–50 characters'
    if (!values.email.trim()) errs.email = 'Required'
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email))
      errs.email = 'Enter a valid email'
    else if (values.email.length > 255)
      errs.email = 'Too long'
    if (!values.password) errs.password = 'Required'
    else if (values.password.length < 8) errs.password = 'Minimum 8 characters'
    if (!values.firstName.trim()) errs.firstName = 'Required'
    else if (values.firstName.length > 100) errs.firstName = 'Too long'
    if (!values.lastName.trim()) errs.lastName = 'Required'
    else if (values.lastName.length > 100) errs.lastName = 'Too long'
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setLoading(true)
    try {
      await apiRegister(values)
      setSuccess(true)
      showToast('Account created — sign in now', 'success')
      setTimeout(onSwitch, 1500)
    } catch (err) {
      showToast(err.message || 'Registration failed', 'error')
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div style={{ textAlign: 'center', padding: '32px 0' }}>
        <div style={{ fontSize: 32, marginBottom: 12 }}>✓</div>
        <p style={{ color: 'var(--green)', fontWeight: 500 }}>Account created!</p>
        <p style={{ color: 'var(--text-2)', fontSize: 13, marginTop: 4 }}>
          Redirecting to login…
        </p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        <div className="field">
          <label htmlFor="reg-firstname">First name</label>
          <input
            id="reg-firstname"
            type="text"
            autoComplete="given-name"
            placeholder="Ada"
            value={values.firstName}
            onChange={set('firstName')}
            className={errors.firstName ? 'error' : ''}
          />
          {errors.firstName && <FieldError>{errors.firstName}</FieldError>}
        </div>
        <div className="field">
          <label htmlFor="reg-lastname">Last name</label>
          <input
            id="reg-lastname"
            type="text"
            autoComplete="family-name"
            placeholder="Lovelace"
            value={values.lastName}
            onChange={set('lastName')}
            className={errors.lastName ? 'error' : ''}
          />
          {errors.lastName && <FieldError>{errors.lastName}</FieldError>}
        </div>
      </div>

      <div className="field">
        <label htmlFor="reg-username">Username</label>
        <input
          id="reg-username"
          type="text"
          autoComplete="username"
          placeholder="ada_lovelace"
          value={values.username}
          onChange={set('username')}
          className={errors.username ? 'error' : ''}
        />
        {errors.username && <FieldError>{errors.username}</FieldError>}
      </div>

      <div className="field">
        <label htmlFor="reg-email">Email</label>
        <input
          id="reg-email"
          type="email"
          autoComplete="email"
          placeholder="ada@example.com"
          value={values.email}
          onChange={set('email')}
          className={errors.email ? 'error' : ''}
        />
        {errors.email && <FieldError>{errors.email}</FieldError>}
      </div>

      <div className="field">
        <label htmlFor="reg-password">Password</label>
        <input
          id="reg-password"
          type="password"
          autoComplete="new-password"
          placeholder="Min. 8 characters"
          value={values.password}
          onChange={set('password')}
          className={errors.password ? 'error' : ''}
        />
        {errors.password && <FieldError>{errors.password}</FieldError>}
      </div>

      <button
        type="submit"
        className="btn btn-primary"
        style={{ width: '100%', marginTop: 4 }}
        disabled={loading}
      >
        {loading ? <span className="spinner" /> : 'Create account'}
      </button>

      <p style={{ textAlign: 'center', fontSize: 13, color: 'var(--text-2)' }}>
        Already registered?{' '}
        <button
          type="button"
          onClick={onSwitch}
          style={{ color: 'var(--accent)', background: 'none', fontSize: 13 }}
        >
          Sign in
        </button>
      </p>
    </form>
  )
}

// ── Helpers ───────────────────────────────────────────────────────────────

function FieldError({ children }) {
  return (
    <span style={{ fontSize: 12, color: 'var(--red)', marginTop: 2 }}>
      {children}
    </span>
  )
}

// ── AuthPage ──────────────────────────────────────────────────────────────

export default function AuthPage() {
  const [tab, setTab] = useState('login')

  return (
    <div style={{
      height: '100%',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--bg-0)',
      padding: 24,
    }}>
      {/* Background grid decoration */}
      <div style={{
        position: 'fixed', inset: 0, pointerEvents: 'none',
        backgroundImage: `
          linear-gradient(var(--border) 1px, transparent 1px),
          linear-gradient(90deg, var(--border) 1px, transparent 1px)
        `,
        backgroundSize: '48px 48px',
        opacity: 0.3,
      }} />

      <div style={{
        position: 'relative',
        width: '100%',
        maxWidth: 420,
        animation: 'authIn 0.3s ease',
      }}>
        {/* Brand */}
        <div style={{ textAlign: 'center', marginBottom: 36 }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: 44, height: 44,
            background: 'var(--accent-dim)',
            border: '1px solid rgba(99,102,241,0.3)',
            borderRadius: 10,
            marginBottom: 16,
          }}>
            <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
              <path d="M2 4a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H7l-5 4V4z"
                fill="var(--accent)" opacity="0.9" />
            </svg>
          </div>
          <h1 style={{
            fontFamily: 'var(--font-brand)',
            fontSize: 26,
            fontWeight: 300,
            fontStyle: 'italic',
            color: 'var(--text-0)',
            letterSpacing: '-0.01em',
          }}>
            ChatApp
          </h1>
          <p style={{ color: 'var(--text-2)', fontSize: 13, marginTop: 4 }}>
            Real-time group conversations
          </p>
        </div>

        {/* Card */}
        <div style={{
          background: 'var(--bg-1)',
          border: '1px solid var(--border)',
          borderRadius: 12,
          overflow: 'hidden',
        }}>
          {/* Tab switcher */}
          <div style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            borderBottom: '1px solid var(--border)',
          }}>
            {['login', 'register'].map((t) => (
              <button
                key={t}
                onClick={() => setTab(t)}
                style={{
                  padding: '12px 0',
                  fontSize: 13,
                  fontWeight: 500,
                  color: tab === t ? 'var(--text-0)' : 'var(--text-2)',
                  background: tab === t ? 'var(--bg-2)' : 'transparent',
                  borderBottom: tab === t ? '2px solid var(--accent)' : '2px solid transparent',
                  transition: 'all 0.15s',
                  textTransform: 'capitalize',
                }}
              >
                {t === 'login' ? 'Sign in' : 'Register'}
              </button>
            ))}
          </div>

          {/* Form area */}
          <div style={{ padding: 24 }}>
            {tab === 'login' ? (
              <LoginForm onSwitch={() => setTab('register')} />
            ) : (
              <RegisterForm onSwitch={() => setTab('login')} />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
