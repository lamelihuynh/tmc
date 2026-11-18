import jsQR from 'jsqr'
import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { type GateInfo, type ScanResult, loadGates, submitScan } from '../lib/api'
import { ResultBanner } from './Home'

const KIOSK_ID = 'WEB-KIOSK-01'
/** The same QR held in front of the camera is submitted once, not on every video frame. */
const SAME_TOKEN_PAUSE_MS = 4000

const MODE_LABEL = {
  ENTRY_ONLY: 'chỉ vào',
  EXIT_ONLY: 'chỉ ra',
  BIDIRECTIONAL: 'hai chiều',
}

interface LogLine {
  at: string
  gateId: string
  result: ScanResult
}

/**
 * A kiosk in the browser: the camera reads the QR, then the page sends POST /kiosk/v1/scans to
 * gate-station, exactly what kiosk-simulator does online. It has NO journal and NO offline mode;
 * for store-and-forward (ADR-11) use the kiosk-simulator CLI.
 */
export default function Kiosk() {
  const [gates, setGates] = useState<GateInfo[]>([])
  const [gateId, setGateId] = useState('BD-GATE-03')
  const [cameraOn, setCameraOn] = useState(false)
  const [cameraError, setCameraError] = useState<string | null>(null)
  const [edgeError, setEdgeError] = useState(false)
  const [last, setLast] = useState<ScanResult | null>(null)
  const [log, setLog] = useState<LogLine[]>([])
  const [pasted, setPasted] = useState('')

  const video = useRef<HTMLVideoElement>(null)
  const canvas = useRef<HTMLCanvasElement>(null)
  const gateRef = useRef(gateId)
  const seen = useRef<{ token: string; at: number }>({ token: '', at: 0 })
  const sending = useRef(false)
  gateRef.current = gateId

  useEffect(() => {
    loadGates()
      .then((g) => {
        setGates(g)
        setEdgeError(false)
      })
      .catch(() => setEdgeError(true))
  }, [])

  async function handle(token: string) {
    if (sending.current) return
    sending.current = true
    try {
      const gate = gateRef.current
      const result = await submitScan(gate, token, KIOSK_ID)
      setLast(result)
      setLog((lines) =>
        [{ at: new Date().toLocaleTimeString('vi-VN'), gateId: gate, result }, ...lines].slice(
          0,
          12,
        ),
      )
    } finally {
      sending.current = false
    }
  }

  useEffect(() => {
    if (!cameraOn) return
    let stream: MediaStream | null = null
    let frame = 0
    let stopped = false

    function tick() {
      if (stopped) return
      const v = video.current
      const c = canvas.current
      if (v && c && v.readyState === v.HAVE_ENOUGH_DATA && v.videoWidth > 0) {
        c.width = v.videoWidth
        c.height = v.videoHeight
        const ctx = c.getContext('2d', { willReadFrequently: true })
        if (ctx) {
          ctx.drawImage(v, 0, 0, c.width, c.height)
          const image = ctx.getImageData(0, 0, c.width, c.height)
          const code = jsQR(image.data, image.width, image.height, {
            inversionAttempts: 'dontInvert',
          })
          const token = code?.data
          if (token) {
            const nowMs = Date.now()
            const repeat =
              token === seen.current.token && nowMs - seen.current.at < SAME_TOKEN_PAUSE_MS
            if (!repeat) {
              seen.current = { token, at: nowMs }
              void handle(token)
            }
          }
        }
      }
      frame = requestAnimationFrame(tick)
    }

    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError(
        'Trình duyệt chặn camera vì trang không chạy trên HTTPS hoặc localhost. Dùng "npm run dev:lan" hoặc dán token ở ô bên dưới.',
      )
      setCameraOn(false)
      return
    }
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'environment' }, audio: false })
      .then((s) => {
        if (stopped) {
          s.getTracks().forEach((t) => t.stop())
          return
        }
        stream = s
        setCameraError(null)
        if (video.current) {
          video.current.srcObject = s
          void video.current.play()
        }
        frame = requestAnimationFrame(tick)
      })
      .catch(() => {
        setCameraError('Không mở được camera. Hãy cấp quyền camera hoặc dán token ở ô bên dưới.')
        setCameraOn(false)
      })

    return () => {
      stopped = true
      cancelAnimationFrame(frame)
      stream?.getTracks().forEach((t) => t.stop())
    }
  }, [cameraOn])

  const gate = gates.find((g) => g.gateId === gateId)

  return (
    <main className="screen screen-wide">
      <header className="topbar">
        <div>
          <span className="brand-mark">TMC</span>
          <strong>Kiosk tại cổng</strong>
        </div>
        <Link className="btn btn-quiet" to="/">
          Ứng dụng nhân viên
        </Link>
      </header>

      {edgeError && (
        <p className="notice notice-bad" role="alert">
          Không kết nối được gate-station (cổng 8081). Hãy khởi động stack rồi tải lại trang.
        </p>
      )}

      <section className="card stack">
        <label className="field">
          <span>Kiosk này gắn với cổng</span>
          <select value={gateId} onChange={(e) => setGateId(e.target.value)}>
            {(gates.length ? gates : [{ gateId } as GateInfo]).map((g) => (
              <option key={g.gateId} value={g.gateId}>
                {g.gateId}
                {g.mode ? ` (${g.factoryCode}, ${MODE_LABEL[g.mode]})` : ''}
              </option>
            ))}
          </select>
        </label>
        {gate?.maintenance && <p className="notice notice-warn">Cổng này đang bảo trì.</p>}
      </section>

      <section className="card stack">
        <div className="viewfinder">
          <video ref={video} playsInline muted className={cameraOn ? '' : 'hidden'} />
          {!cameraOn && <p className="muted">Camera đang tắt</p>}
          <canvas ref={canvas} className="hidden" />
        </div>
        <button
          className={cameraOn ? 'btn' : 'btn btn-primary'}
          onClick={() => setCameraOn((on) => !on)}
        >
          {cameraOn ? 'Tắt camera' : 'Bật camera và bắt đầu quét'}
        </button>
        {cameraError && (
          <p className="notice notice-bad" role="alert">
            {cameraError}
          </p>
        )}
        {last ? (
          <ResultBanner result={last} />
        ) : (
          <p className="muted small center">Đưa mã QR trên điện thoại vào trước camera.</p>
        )}
      </section>

      <details className="card">
        <summary>Không có camera? Dán token</summary>
        <div className="stack">
          <textarea
            rows={3}
            value={pasted}
            onChange={(e) => setPasted(e.target.value)}
            placeholder="eyJhbGciOiJFUzI1NiIs…"
            aria-label="Token QR"
          />
          <button
            className="btn btn-primary"
            disabled={!pasted.trim()}
            onClick={() => void handle(pasted.trim())}
          >
            Gửi như một lượt quét
          </button>
        </div>
      </details>

      {log.length > 0 && (
        <section className="card stack">
          <h2>Các lượt quét gần đây</h2>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Giờ</th>
                  <th>Cổng</th>
                  <th>Kết quả</th>
                  <th>Chiều</th>
                  <th>Lý do</th>
                </tr>
              </thead>
              <tbody>
                {log.map((line, i) => (
                  <tr key={`${line.result.attemptId}-${i}`}>
                    <td>{line.at}</td>
                    <td>{line.gateId}</td>
                    <td>{line.result.outcome}</td>
                    <td>{line.result.direction ?? ''}</td>
                    <td>{line.result.reasonCode ?? ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </main>
  )
}
