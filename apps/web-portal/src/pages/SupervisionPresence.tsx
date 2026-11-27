import { useState } from 'react'
import { api, describeError } from '../lib/api'

interface Presence {
  employeeUuid: string
  factoryCode: string
  direction: string
  at: string
}

interface FraudAlert {
  attemptId: string
  employeeUuid: string
  factoryCode: string
  otherFactory: string
  rule: string
  detail: string
  at: string
}

const time = (iso: string) => new Date(iso).toLocaleString('vi-VN')

/**
 * Who is inside a factory now (FR-22) and the fraud alerts that involve it (ADR-12 R4, D-05). Reads
 * the reporting read model, never the attendance database.
 */
export default function SupervisionPresence() {
  const [factory, setFactory] = useState('BD')
  const [rows, setRows] = useState<Presence[] | null>(null)
  const [alerts, setAlerts] = useState<FraudAlert[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setError(null)
    const f = encodeURIComponent(factory)
    try {
      const [presence, found] = await Promise.all([
        api<Presence[]>(`/supervision/v1/presence?factory=${f}`),
        api<FraudAlert[]>(`/supervision/v1/alerts?factory=${f}`),
      ])
      setRows(presence)
      setAlerts(found)
    } catch (e) {
      setRows(null)
      setAlerts(null)
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
                {r.employeeUuid} - vào lúc {time(r.at)}
              </li>
            ))}
          </ul>
        </>
      )}
      {alerts && (
        <section aria-label="Cảnh báo gian lận">
          <h3>Cảnh báo gian lận ({alerts.length})</h3>
          {alerts.length === 0 ? (
            <p>Không có cảnh báo.</p>
          ) : (
            <table>
              <thead>
                <tr>
                  <th>Lúc</th>
                  <th>Nhân viên</th>
                  <th>Nhà máy</th>
                  <th>Luật</th>
                  <th>Chi tiết</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((a) => (
                  <tr key={`${a.attemptId}-${a.rule}`}>
                    <td>{time(a.at)}</td>
                    <td>{a.employeeUuid}</td>
                    <td>
                      {a.otherFactory} → {a.factoryCode}
                    </td>
                    <td>{a.rule}</td>
                    <td>{a.detail}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <p className="hint">
            Lượt quét bị cảnh báo vẫn được giữ để HR đối soát, không bị xóa (ADR-12 R4).
          </p>
        </section>
      )}
      <p className="hint">
        Dữ liệu có thể trễ vài giây so với cổng vì báo cáo được cập nhật bất đồng bộ (ADR-10).
      </p>
    </>
  )
}
