import { useCallback, useEffect, useState } from 'react'
import { clearToken, getToken, setUnauthorizedHandler } from './api'
import AuthForm from './AuthForm'
import DocumentsPanel from './DocumentsPanel'
import ChatPanel from './ChatPanel'

export default function App() {
  const [loggedIn, setLoggedIn] = useState(Boolean(getToken()))
  const [notice, setNotice] = useState('')

  const logout = useCallback((message = '') => {
    clearToken()
    setLoggedIn(false)
    setNotice(message)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => logout('Your session expired. Please log in again.'))
  }, [logout])

  if (!loggedIn) {
    return (
      <AuthForm
        notice={notice}
        onLoggedIn={() => {
          setNotice('')
          setLoggedIn(true)
        }}
      />
    )
  }

  return (
    <div className="app">
      <header className="topbar">
        <h1>DocMind</h1>
        <button className="ghost" onClick={() => logout()}>
          Log out
        </button>
      </header>
      <main className="layout">
        <DocumentsPanel />
        <ChatPanel />
      </main>
    </div>
  )
}