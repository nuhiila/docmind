import { useEffect, useRef, useState } from 'react'
import { ask } from './api'

function uniqueSources(sources = []) {
  const seen = new Set()
  return sources.filter((s) => {
    const key = `${s.filename}|${s.page}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

export default function ChatPanel() {
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [busy, setBusy] = useState(false)
  const bottom = useRef(null)

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, busy])

  async function submit(e) {
    e.preventDefault()
    const q = question.trim()
    if (!q || busy) return
    setMessages((m) => [...m, { role: 'user', text: q }])
    setQuestion('')
    setBusy(true)
    try {
      const data = await ask(q)
      setMessages((m) => [...m, { role: 'assistant', text: data.answer, sources: data.sources }])
    } catch (err) {
      setMessages((m) => [...m, { role: 'assistant', text: err.message, error: true }])
    } finally {
      setBusy(false)
    }
  }

  return (
    <section className="card panel chat">
      <h2>Ask your documents</h2>

      <div className="messages">
        {messages.length === 0 && (
          <p className="muted">
            Upload a PDF, wait until it is <strong>READY</strong>, then ask a question about it.
          </p>
        )}

        {messages.map((m, i) => (
          <div key={i} className={`msg ${m.role} ${m.error ? 'error' : ''}`}>
            <p>{m.text}</p>
            {m.sources && uniqueSources(m.sources).length > 0 && (
              <div className="sources">
                {uniqueSources(m.sources).map((s) => (
                  <span key={`${s.filename}|${s.page}`} className="chip">
                    {s.filename} · page {s.page}
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}

        {busy && <div className="msg assistant thinking">Thinking…</div>}
        <div ref={bottom} />
      </div>

      <form className="composer" onSubmit={submit}>
        <input
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about your documents…"
          maxLength={1000}
        />
        <button className="primary" disabled={busy || !question.trim()}>
          Ask
        </button>
      </form>
    </section>
  )
}