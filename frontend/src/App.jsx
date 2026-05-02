import { useState, createContext, useContext, useCallback } from 'react'
import AuthPage from './components/AuthPage'
import ChatPage from './components/ChatPage'

// ── Auth Context ──────────────────────────────────────────────────────────

const AuthContext = createContext(null)

export function useAuth() {
  return useContext(AuthContext)
}

// ── Toast Context ─────────────────────────────────────────────────────────

const ToastContext = createContext(null)

export function useToast() {
  return useContext(ToastContext)
}

// ── App ───────────────────────────────────────────────────────────────────

export default function App() {
  const [user, setUser] = useState(() => {
    try {
      const s = localStorage.getItem('chat_user')
      return s ? JSON.parse(s) : null
    } catch {
      return null
    }
  })

  const [toast, setToast] = useState(null)

  const showToast = useCallback((message, type = 'info') => {
    setToast({ message, type, id: Date.now() })
    setTimeout(() => setToast(null), 3500)
  }, [])

  const login = useCallback((userData) => {
    localStorage.setItem('chat_user', JSON.stringify(userData))
    setUser(userData)
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('chat_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      <ToastContext.Provider value={showToast}>
        {user ? <ChatPage /> : <AuthPage />}

        {toast && (
          <div key={toast.id} className={`toast ${toast.type}`}>
            {toast.type === 'error' && (
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="7" stroke="#f87171" strokeWidth="1.5" />
                <path d="M8 4.5v4M8 10.5v1" stroke="#f87171" strokeWidth="1.5" strokeLinecap="round" />
              </svg>
            )}
            {toast.type === 'success' && (
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                <circle cx="8" cy="8" r="7" stroke="#34d399" strokeWidth="1.5" />
                <path d="M5 8l2 2 4-4" stroke="#34d399" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            )}
            {toast.message}
          </div>
        )}
      </ToastContext.Provider>
    </AuthContext.Provider>
  )
}
