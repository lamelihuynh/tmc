import { type FormEvent, useEffect, useState } from 'react'
import { api, describeError } from '../lib/api'

interface Gate {
  gateId: string
  factoryCode: string
  mode: string
  maintenance: boolean
}
interface ManualResult {
  outcome: string
  reason: string | null
  direction: string | null
}

/**
 * Guard Console (UC-05, UC-06). Lists gates and lets a guard open one by hand. The form mirrors the rule of
 * the server: a reason is mandatory, and only gates in the guard's scope are accepted (the server decides).
 * TODO Team D: live gate health and anti-passback alerts over SSE (FR-14, FR-18).
 */
export default function SecurityGates() {
  const [gates, setGates] = useState<Gate[]>([])
  const [error, setError] = useState<string | null>(null)
  const [gateId, setGateId] = useState('')
  const [employee, setEmployee] = useState('')
  const [direction, setDirection] = useState('ENTRY')
  const [reason, setReason] = useState('')
  const [result, setResult] = useState<ManualResult | null>(null)

  useEffect(() => {
    api<{ gates: Gate[] }>('/kiosk/v1/sync-bundle')
      .then((b) => {
        setGates(b.gates)
        if (b.gates[0]) setGateId(b.gates[0].gateId)
      })
      .catch((e) => setError(describeError(e)))
  }, [])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setResult(null)
    try {
      setResult(
        await api<ManualResult>(`/security/v1/gates/${gateId}/manual-open`, {
          method: 'POST',
          body: JSON.stringify({ employee_uuid: employee, direction, reason }),
        }),
      )
    } catch (err) {
      setError(describeError(err))
    }
  }

  return (
    <>
      <h2>Cổng ra vào</h2>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
      <table>
        <thead>
          <tr>
            <th>Cổng</th>
            <th>Nhà máy</th>
            <th>Chế độ</th>
            <th>Bảo trì</th>
          </tr>
        </thead>
        <tbody>
          {gates.map((g) => (
            <tr key={g.gateId}>
              <td>{g.gateId}</td>
              <td>{g.factoryCode}</td>
              <td>{g.mode}</td>
              <td>{g.maintenance ? 'Có' : 'Không'}</td>
            </tr>
          ))}
        </tbody>
      </table>

      <h2>Mở cổng thủ công</h2>
      <p>
        Chỉ dùng khi QR hoặc thiết bị lỗi và đã xác minh danh tính nhân viên. Lý do là bắt buộc và
        được ghi nhật ký.
      </p>
      <form onSubmit={submit}>
        <label>
          Cổng
          <select value={gateId} onChange={(e) => setGateId(e.target.value)}>
            {gates.map((g) => (
              <option key={g.gateId}>{g.gateId}</option>
            ))}
          </select>
        </label>
        <label>
          Mã định danh nhân viên (UUID)
          <input required value={employee} onChange={(e) => setEmployee(e.target.value)} />
        </label>
        <label>
          Chiều
          <select value={direction} onChange={(e) => setDirection(e.target.value)}>
            <option>ENTRY</option>
            <option>EXIT</option>
          </select>
        </label>
        <label>
          Lý do
          <input required value={reason} onChange={(e) => setReason(e.target.value)} />
        </label>
        <button type="submit" disabled={!reason.trim() || !employee.trim()}>
          Mở cổng
        </button>
      </form>
      {result && (
        <p className={result.outcome === 'ALLOWED' ? 'ok' : 'error'}>
          Kết quả: {result.outcome}
          {result.reason ? ` (${result.reason})` : ''}
        </p>
      )}
    </>
  )
}
