import { useState } from 'react'
import { api, describeError } from '../lib/api'

interface SyncRecord {
  eventId: string
  status: string
  attempts: number
  lastError: string | null
}

const STATUSES = ['PENDING', 'IN_PROGRESS', 'SYNCED', 'FAILED', 'RETRY_PENDING', 'UNKNOWN']

/**
 * Sync state of attendance sent to HR/Payroll (FR-30). UNKNOWN means HR may or may not have stored the
 * record: it is never treated as failed or as sent. TODO Team D: reconciliation of the month and the
 * 08:00 / 12:00 deadline countdown (NFR-08).
 */
export default function HrReconciliation() {
  const [status, setStatus] = useState('UNKNOWN')
  const [rows, setRows] = useState<SyncRecord[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setError(null)
    try {
      setRows(await api<SyncRecord[]>(`/admin/v1/hr-sync?status=${status}`))
    } catch (e) {
      setRows(null)
      setError(describeError(e))
    }
  }

  return (
    <>
      <h2>Đồng bộ sang HR/Payroll</h2>
      <label>
        Trạng thái
        <select value={status} onChange={(e) => setStatus(e.target.value)}>
          {STATUSES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
      </label>
      <button onClick={load}>Tải</button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {rows && (
        <table>
          <thead>
            <tr>
              <th>Sự kiện</th>
              <th>Trạng thái</th>
              <th>Số lần</th>
              <th>Lỗi gần nhất</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.eventId}>
                <td>{r.eventId}</td>
                <td>{r.status}</td>
                <td>{r.attempts}</td>
                <td>{r.lastError ?? ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
