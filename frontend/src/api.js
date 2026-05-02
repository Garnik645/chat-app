const BASE = ''  // proxied through Vite dev server → api-gateway:8080

function getToken() {
  try {
    const stored = localStorage.getItem('chat_user')
    return stored ? JSON.parse(stored).token : null
  } catch {
    return null
  }
}

async function request(method, path, body, options = {}) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    ...options,
  })

  if (res.status === 204) return null

  let data
  const contentType = res.headers.get('content-type') || ''
  if (contentType.includes('application/json')) {
    data = await res.json()
  } else {
    data = await res.text()
  }

  if (!res.ok) {
    const message =
      (data && data.message) ||
      (data && data.error) ||
      (typeof data === 'string' ? data : null) ||
      `HTTP ${res.status}`
    const err = new Error(message)
    err.status = res.status
    err.data = data
    throw err
  }

  return data
}

// ── Auth ─────────────────────────────────────────────────────────────────

export async function login(username, password) {
  return request('POST', '/api/users/login', { username, password })
}

export async function register({ username, email, password, firstName, lastName }) {
  return request('POST', '/api/users/register', {
    username,
    email,
    password,
    firstName,
    lastName,
  })
}

export async function getProfile(userId) {
  return request('GET', `/api/users/${userId}`)
}

export async function getMe() {
  return request('GET', '/api/users/me')
}

// ── Rooms ─────────────────────────────────────────────────────────────────

export async function getRooms(page = 0, size = 30) {
  return request('GET', `/api/rooms?page=${page}&size=${size}`)
}

export async function getRoom(roomId) {
  return request('GET', `/api/rooms/${roomId}`)
}

export async function createRoom({ name, description }) {
  return request('POST', '/api/rooms', { name, description })
}

export async function joinRoom(roomId) {
  return request('POST', `/api/rooms/${roomId}/members`)
}

export async function leaveRoom(roomId) {
  return request('DELETE', `/api/rooms/${roomId}/members`)
}

export async function deleteRoom(roomId) {
  return request('DELETE', `/api/rooms/${roomId}`)
}

export async function getRoomMembers(roomId) {
  return request('GET', `/api/rooms/${roomId}/members`)
}

// ── Messages ─────────────────────────────────────────────────────────────

export async function getMessages(roomId, page = 0, size = 50) {
  return request('GET', `/api/messages/${roomId}?page=${page}&size=${size}`)
}

// ── WebSocket ─────────────────────────────────────────────────────────────
//
// Returns a WebSocket connected to the chat service via API Gateway.
// The gateway validates the JWT from the `token` query parameter.
//
export function createChatSocket(roomId, token) {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = location.host  // handled by Vite proxy in dev, nginx in prod
  const url = `${protocol}//${host}/ws/rooms/${encodeURIComponent(roomId)}?token=${encodeURIComponent(token)}`
  return new WebSocket(url)
}
