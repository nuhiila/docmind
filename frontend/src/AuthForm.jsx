import { useState } from 'react'
import { login, register } from './api'

export default function AuthForm({ onLoggedIn, notice }) {
  const [mode, setMode] = useState('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    setInfo('')
    setBusy(true)
    try {
      if (mode === 'register') {
        await register(email, password)
        setInfo('Account created. You can log in now.')
        setMode('login')
      } else {
        await login(email, password)
        onLoggedIn()
      }
    } catch (err) {
      if (mode === 'register' && err.message.includes('(400)')) {
        setError('Enter a valid email and a password of at least 8 characters.')
      } else {
        setError(err.message)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <form className="card auth-card" onSubmit={submit}>
        <h1>DocMind</h1>
        <p className="muted">Ask questions about your documents and get answers with sources.</p>

        {notice && <p className="alert warn">{notice}</p>}
        {info && <p className="alert ok">{info}</p>}
        {error && <p className="alert bad">{error}</p>}

        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            minLength={8}
            required
          />
        </label>

        <button className="primary" disabled={busy}>
          {busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Create account'}
        </button>

        <p className="muted switch">
          {mode === 'login' ? 'No account yet?' : 'Already have an account?'}{' '}
          <button
            type="button"
            className="link"
            onClick={() => {
              setMode(mode === 'login' ? 'register' : 'login')
              setError('')
              setInfo('')
            }}
          >
            {mode === 'login' ? 'Create one' : 'Log in'}
          </button>
        </p>
      </form>
    </div>
  )
}