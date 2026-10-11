const TOKEN_KEY = 'docmind_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
const setToken = (token) => localStorage.setItem(TOKEN_KEY, token)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

let onUnauthorized = () => {}
export const setUnauthorizedHandler = (fn) => {
  onUnauthorized = fn
}

async function request(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const res = await fetch(path, { ...options, headers })
  const type = res.headers.get('content-type') || ''
  const body = type.includes('application/json') ? await res.json() : await res.text()

  if (!res.ok) {
    if (res.status === 401 && token) onUnauthorized()
    const message =
      (typeof body === 'string' && body) || (body && body.message) || `Request failed (${res.status})`
    throw new Error(message)
  }
  return body
}

const json = (data) => ({
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(data),
})

export const register = (email, password) =>
  request('/api/auth/register', { method: 'POST', ...json({ email, password }) })

export async function login(email, password) {
  const data = await request('/api/auth/login', { method: 'POST', ...json({ email, password }) })
  setToken(data.token)
  return data
}

export const listDocuments = () => request('/api/documents')

export function uploadDocument(file) {
  const form = new FormData()
  form.append('file', file)
  return request('/api/documents', { method: 'POST', body: form })
}

export const reprocessDocument = (id) => request(`/api/documents/${id}/process`, { method: 'POST' })

export const ask = (question) => request('/api/ask', { method: 'POST', ...json({ question }) })