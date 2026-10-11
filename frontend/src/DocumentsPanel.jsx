import { useCallback, useEffect, useRef, useState } from 'react'
import { listDocuments, reprocessDocument, uploadDocument } from './api'

const PENDING = ['UPLOADED', 'PROCESSING']

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

export default function DocumentsPanel() {
  const [documents, setDocuments] = useState([])
  const [error, setError] = useState('')
  const [uploading, setUploading] = useState(false)
  const fileInput = useRef(null)

  const refresh = useCallback(async () => {
    try {
      setDocuments(await listDocuments())
      setError('')
    } catch (err) {
      setError(err.message)
    }
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  // While something is being processed in the background, check again every 3 seconds.
  const hasPending = documents.some((d) => PENDING.includes(d.status))
  useEffect(() => {
    if (!hasPending) return
    const timer = setInterval(refresh, 3000)
    return () => clearInterval(timer)
  }, [hasPending, refresh])

  async function onFileChosen(e) {
    const file = e.target.files[0]
    if (!file) return
    setUploading(true)
    setError('')
    try {
      await uploadDocument(file)
      await refresh()
    } catch (err) {
      setError(err.message)
    } finally {
      setUploading(false)
      if (fileInput.current) fileInput.current.value = ''
    }
  }

  async function retry(id) {
    try {
      await reprocessDocument(id)
      await refresh()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <section className="card panel">
      <div className="panel-head">
        <h2>Documents</h2>
        <label className={`button primary ${uploading ? 'disabled' : ''}`}>
          {uploading ? 'Uploading…' : 'Upload PDF'}
          <input
            ref={fileInput}
            type="file"
            accept="application/pdf,.pdf"
            hidden
            disabled={uploading}
            onChange={onFileChosen}
          />
        </label>
      </div>

      {error && <p className="alert bad">{error}</p>}

      {documents.length === 0 ? (
        <p className="muted">No documents yet. Upload a PDF to get started.</p>
      ) : (
        <ul className="docs">
          {documents.map((d) => (
            <li key={d.id}>
              <div className="doc-main">
                <strong title={d.name}>{d.name}</strong>
                <span className="muted small">
                  {formatSize(d.size)} · {new Date(d.uploadedAt).toLocaleString()}
                </span>
              </div>
              <div className="doc-side">
                <span className={`badge ${d.status.toLowerCase()}`}>{d.status}</span>
                {d.status === 'FAILED' && (
                  <button className="link" onClick={() => retry(d.id)}>
                    Retry
                  </button>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}