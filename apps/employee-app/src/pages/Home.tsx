import QRCode from 'qrcode'
import { useCallback, useEffect, useRef, useState } from 'react'
import {
  type GateInfo,
  type ScanResult,
  type Session,
  describeReason,
  loadGates,
  registerDevice,
  revokeDevice,
  submitScan,
} from '../lib/api'
import {
  type StoredDevice,
  forgetDevice,
  generateKeyPair,
  loadDevice,
  saveDevice,
  secureContextProblem,
} from '../lib/deviceKey'
import { TOKEN_TTL_SECONDS, type TokenClaims, signToken } from '../lib/token'

// The "fac" claim. The gate does not trust it for authorization: gate-station checks the employee
// directory (FactoryPermissionRule). It is carried so an offline kiosk has something to check.
const FACTORIES = ['BD', 'LA']
/** A fresh token is issued before the old one runs out, so the QR on screen is never stale. */
const ROTATE_EVERY_SECONDS = 20

type Mode = 'normal' | 'frozen' | 'longLived' | 'pii'

const MODES: { id: Mode; label: string; expect: string }[] = [
  { id: 'normal', label: 'Bình thường', expect: 'QR đổi mỗi 20 giây, sống 30 giây. Cổng mở.' },
  {
    id: 'frozen',
    label: 'Ảnh chụp màn hình',
    expect: 'QR đứng yên như ảnh chụp. Quét lần hai bị QR_ALREADY_USED, để lâu bị QR_EXPIRED.',
  },
  {
    id: 'longLived',
    label: 'QR sống 24 giờ',
    expect: 'Giống QR tĩnh. Máy chủ từ chối với MALFORMED_TOKEN.',
  },
  {
    id: 'pii',
    label: 'QR chứa họ tên',
    expect: 'Token mang dữ liệu cá nhân. Máy chủ từ chối với MALFORMED_TOKEN.',
  },
]

interface Issued {
  token: string
  claims: TokenClaims
}

export default function Home({ session, onLogout }: { session: Session; onLogout: () => void }) {
  const [device, setDevice] = useState<StoredDevice | null | undefined>(undefined)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const blocked = secureContextProblem()

  useEffect(() => {
    if (blocked) return
    loadDevice(session.subject)
      .then((d) => setDevice(d ?? null))
      .catch(() => setDevice(null))
  }, [session.subject, blocked])

  async function enroll() {
    setBusy(true)
    setError(null)
    try {
      const { privateKey, publicKeyBase64 } = await generateKeyPair()
      const deviceId = await registerDevice(session, publicKeyBase64)
      const stored: StoredDevice = {
        employeeUuid: session.subject,
        deviceId,
        privateKey,
        registeredAt: new Date().toISOString(),
      }
      await saveDevice(stored)
      setDevice(stored)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không đăng ký được thiết bị.')
    } finally {
      setBusy(false)
    }
  }

  async function revoke() {
    if (!device) return
    setBusy(true)
    setError(null)
    try {
      await revokeDevice(session, device.deviceId)
      await forgetDevice(session.subject)
      setDevice(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Không thu hồi được thiết bị.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="screen">
      <header className="topbar">
        <div>
          <span className="brand-mark">TMC</span>
          <strong>{session.username}</strong>
        </div>
        <button className="btn btn-quiet" onClick={onLogout}>
          Đăng xuất
        </button>
      </header>

      {blocked && (
        <p className="notice notice-bad" role="alert">
          {blocked}
        </p>
      )}
      {error && (
        <p className="notice notice-bad" role="alert">
          {error}
        </p>
      )}

      {!blocked && device === null && (
        <section className="card stack">
          <h2>Đăng ký thiết bị này</h2>
          <p className="muted">
            Ứng dụng sẽ tạo một cặp khóa ngay trên máy. Khóa riêng không bao giờ rời thiết bị; chỉ
            khóa công khai được gửi lên máy chủ. Mỗi nhân viên chỉ có một thiết bị hoạt động, nên
            thiết bị cũ (nếu có) sẽ bị thu hồi.
          </p>
          <button className="btn btn-primary" onClick={enroll} disabled={busy}>
            {busy ? 'Đang đăng ký…' : 'Tạo khóa và đăng ký'}
          </button>
        </section>
      )}

      {!blocked && device && (
        <QrCard
          session={session}
          device={device}
          onRevoke={revoke}
          onReEnroll={async () => {
            await forgetDevice(session.subject)
            setDevice(null)
          }}
          busy={busy}
        />
      )}
    </main>
  )
}

function QrCard({
  session,
  device,
  onRevoke,
  onReEnroll,
  busy,
}: {
  session: Session
  device: StoredDevice
  onRevoke: () => void
  onReEnroll: () => void
  busy: boolean
}) {
  const [mode, setMode] = useState<Mode>('normal')
  const [issued, setIssued] = useState<Issued | null>(null)
  const [now, setNow] = useState(() => Math.floor(Date.now() / 1000))
  const canvas = useRef<HTMLCanvasElement>(null)

  const issue = useCallback(
    async (forMode: Mode) => {
      const next = await signToken(
        device.privateKey,
        { deviceId: device.deviceId, employeeUuid: session.subject, factories: FACTORIES },
        {
          ttlSeconds: forMode === 'longLived' ? 24 * 3600 : TOKEN_TTL_SECONDS,
          addPii: forMode === 'pii',
        },
      )
      setIssued(next)
    },
    [device, session.subject],
  )

  // one clock tick per second drives both the countdown and the rotation
  useEffect(() => {
    const timer = setInterval(() => setNow(Math.floor(Date.now() / 1000)), 1000)
    return () => clearInterval(timer)
  }, [])

  useEffect(() => {
    void issue(mode)
  }, [mode, issue])

  const age = issued ? now - issued.claims.iat : 0
  useEffect(() => {
    if (mode !== 'frozen' && issued && age >= ROTATE_EVERY_SECONDS) void issue(mode)
  }, [age, mode, issued, issue])

  useEffect(() => {
    if (issued && canvas.current) {
      void QRCode.toCanvas(canvas.current, issued.token, {
        errorCorrectionLevel: 'L',
        margin: 2,
        width: 296,
      })
    }
  }, [issued])

  const secondsLeft = issued ? issued.claims.exp - now : 0
  const expired = secondsLeft <= 0
  const active = MODES.find((m) => m.id === mode)!

  return (
    <>
      <section className="card stack center">
        <div className={`qr-frame${expired ? ' qr-expired' : ''}`}>
          <canvas ref={canvas} aria-label="Mã QR ra vào cổng" />
          {expired && <span className="qr-stamp">HẾT HẠN</span>}
        </div>
        <p className="countdown" aria-live="off">
          {mode === 'longLived'
            ? 'Hết hạn sau 24 giờ'
            : expired
              ? 'Mã đã hết hạn'
              : `Còn ${secondsLeft} giây`}
        </p>
        <p className="muted small">Đưa mã này vào máy quét tại cổng.</p>
      </section>

      <section className="card stack">
        <h2>Chế độ kiểm thử</h2>
        <div className="segmented" role="radiogroup" aria-label="Chế độ kiểm thử">
          {MODES.map((m) => (
            <button
              key={m.id}
              role="radio"
              aria-checked={mode === m.id}
              className={mode === m.id ? 'on' : ''}
              onClick={() => setMode(m.id)}
            >
              {m.label}
            </button>
          ))}
        </div>
        <p className="muted small">
          <strong>Kỳ vọng:</strong> {active.expect}
        </p>
      </section>

      {issued && <SelfTest token={issued.token} onDeviceUnknown={onReEnroll} />}

      {issued && (
        <details className="card">
          <summary>Nội dung token đang hiển thị</summary>
          <dl className="claims">
            <dt>kid (thiết bị)</dt>
            <dd>{device.deviceId}</dd>
            <dt>sub (nhân viên)</dt>
            <dd>{issued.claims.sub}</dd>
            <dt>jti (mã một lần)</dt>
            <dd>{issued.claims.jti}</dd>
            <dt>iat / exp</dt>
            <dd>
              {new Date(issued.claims.iat * 1000).toLocaleTimeString('vi-VN')} /{' '}
              {new Date(issued.claims.exp * 1000).toLocaleTimeString('vi-VN')}
            </dd>
            <dt>fac</dt>
            <dd>{issued.claims.fac.join(', ')}</dd>
            {'name' in issued.claims && (
              <>
                <dt>name (không được phép)</dt>
                <dd>{String(issued.claims.name)}</dd>
              </>
            )}
          </dl>
          <CopyButton text={issued.token} />
          <p className="muted small">
            Dán token vào <code>kiosk-sim scan --gate BD-GATE-01 &lt;token&gt;</code> để thử kiosk
            dòng lệnh, kể cả chế độ mất mạng.
          </p>
        </details>
      )}

      <section className="card stack">
        <h2>Thiết bị</h2>
        <p className="muted small">
          Đăng ký lúc {new Date(device.registeredAt).toLocaleString('vi-VN')}. Thu hồi thì mọi mã QR
          của thiết bị này bị từ chối với DEVICE_REVOKED.
        </p>
        <button className="btn btn-danger" onClick={onRevoke} disabled={busy}>
          Thu hồi thiết bị này
        </button>
      </section>
    </>
  )
}

/** Sends the token straight to gate-station, as a kiosk would, when no second device is at hand. */
function SelfTest({ token, onDeviceUnknown }: { token: string; onDeviceUnknown: () => void }) {
  const [gates, setGates] = useState<GateInfo[]>([])
  const [gateId, setGateId] = useState('BD-GATE-03')
  const [result, setResult] = useState<ScanResult | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    loadGates()
      .then(setGates)
      .catch(() => setGates([]))
  }, [])

  async function send() {
    setBusy(true)
    setResult(await submitScan(gateId, token, 'APP-SELFTEST'))
    setBusy(false)
  }

  return (
    <section className="card stack">
      <h2>Thử ngay không cần máy quét</h2>
      <p className="muted small">
        Gửi mã đang hiển thị thẳng tới gate-station, giống như kiosk vừa quét nó.
      </p>
      <div className="row">
        <select value={gateId} onChange={(e) => setGateId(e.target.value)} aria-label="Cổng">
          {(gates.length ? gates.map((g) => g.gateId) : [gateId]).map((id) => (
            <option key={id}>{id}</option>
          ))}
        </select>
        <button className="btn btn-primary" onClick={send} disabled={busy}>
          {busy ? 'Đang gửi…' : 'Gửi tới cổng'}
        </button>
      </div>
      {result && <ResultBanner result={result} />}
      {result?.reasonCode === 'DEVICE_UNKNOWN' && (
        <div className="stack">
          <p className="muted small">
            Máy chủ không còn biết thiết bị này. Ở chế độ local, user-qr lưu trong bộ nhớ nên mất
            danh sách thiết bị mỗi lần khởi động lại.
          </p>
          <button className="btn" onClick={onDeviceUnknown}>
            Đăng ký lại thiết bị
          </button>
        </div>
      )}
    </section>
  )
}

export function ResultBanner({ result }: { result: ScanResult }) {
  const tone =
    result.outcome === 'ALLOWED' || result.outcome === 'UNCONFIRMED'
      ? 'good'
      : result.outcome === 'DENIED' || result.outcome === 'ERROR'
        ? 'bad'
        : 'warn'
  const title = {
    ALLOWED: 'CỔNG MỞ',
    UNCONFIRMED: 'CỔNG MỞ (đang ghi nhận)',
    DENIED: 'TỪ CHỐI',
    PENDING: 'ĐANG CHỜ CỔNG',
    IN_PROGRESS: 'ĐANG XỬ LÝ',
    ERROR: 'LỖI HỆ THỐNG',
  }[result.outcome]
  return (
    <div className={`result result-${tone}`} role="status">
      <strong>{title}</strong>
      {result.direction && <span>{result.direction === 'ENTRY' ? 'Chiều vào' : 'Chiều ra'}</span>}
      {result.reasonCode && (
        <span>
          {describeReason(result.reasonCode)} <code>{result.reasonCode}</code>
        </span>
      )}
      {result.duplicate && <span>Đây là câu trả lời đã lưu của lượt quét trước.</span>}
    </div>
  )
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      className="btn"
      onClick={async () => {
        try {
          await navigator.clipboard.writeText(text)
          setCopied(true)
          setTimeout(() => setCopied(false), 1500)
        } catch {
          setCopied(false)
        }
      }}
    >
      {copied ? 'Đã sao chép' : 'Sao chép token'}
    </button>
  )
}
