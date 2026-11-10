import { useState } from 'react'
import { api, describeError } from '../lib/api'

interface Presence {
  employeeUuid: string
  factoryCode: string
  direction: string
  at: string
}

/** Who is inside a factory now (FR-22). Reads the reporting read model, never the attendance database. */
export default function SupervisionPresence() {
  const [factory, setFactory] = useState('BD')
  const [rows, setRows] = useState<Presence[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setError(null)
    try {
      setRows(
        await api<Presence[]>(`/supervision/v1/presence?factory=${encodeURIComponent(factory)}`),
      )
    } catch (e) {
      setRows(null)
      setError(describeError(e))
    }
  }

  return (
    <>
      <h2>Hiện diện trong nhà máy</h2>
      <label>
        Nhà máy
        <select value={factory} onChange={(e) => setFactory(e.target.value)}>
          <option>BD</option>
          <option>LA</option>
        </select>
      </label>
      <button onClick={load}>Tải</button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      {rows && (
        <>
          <p>Đang có mặt: {rows.length}</p>
          <ul>
            {rows.map((r) => (
              <li key={r.employeeUuid}>
                {r.employeeUuid} - vào lúc {new Date(r.at).toLocaleString('vi-VN')}
              </li>
            ))}
          </ul>
        </>
      )}
      <p className="hint">
        Dữ liệu có thể trễ vài giây so với cổng vì báo cáo được cập nhật bất đồng bộ (ADR-10).
      </p>
    </>
  )
}
