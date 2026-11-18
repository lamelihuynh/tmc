import { type FormEvent, useState } from 'react'
import { Link } from 'react-router-dom'
import { type Session, login } from '../lib/api'

const MESSAGES = {
  DENIED: 'Sai tên đăng nhập hoặc mật khẩu.',
  LOCKED: 'Tài khoản bị khóa 15 phút do nhập sai 5 lần.',
  MFA_REQUIRED: 'Tài khoản này cần mã xác thực hai lớp và không dùng cho ứng dụng nhân viên.',
  NOT_EMPLOYEE: 'Tài khoản này không phải tài khoản nhân viên.',
  UNREACHABLE: 'Không kết nối được dịch vụ đăng nhập (identity, cổng 8087).',
}

// Seeded in services/identity IdentityConfig; each maps to an employee gate-station knows.
const DEMO_ACCOUNTS = [
  { username: 'nv001', note: 'được vào BD và LA' },
  { username: 'nv002', note: 'chỉ được vào BD' },
  { username: 'nv003', note: 'đã nghỉ việc, cổng sẽ từ chối' },
  { username: 'nv004', note: 'được vào BD và LA' },
  { username: 'nv005', note: 'được vào BD và LA' },
]
const DEMO_PASSWORD = 'dev-only-change-me'

export default function Login({ onLogin }: { onLogin: (session: Session) => void }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    const result = await login(username.trim(), password)
    setBusy(false)
    if (result.ok) onLogin(result.session)
    else setError(MESSAGES[result.status])
  }

  return (
    <main className="screen">
      <header className="brand">
        <span className="brand-mark">TMC</span>
        <h1>Thẻ QR nhân viên</h1>
        <p className="muted">Đăng nhập để hiện mã QR ra vào cổng.</p>
      </header>

      <form className="card stack" onSubmit={submit}>
        <label className="field">
          <span>Tên đăng nhập</span>
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            required
          />
        </label>
        <label className="field">
          <span>Mật khẩu</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && (
          <p className="notice notice-bad" role="alert">
            {error}
          </p>
        )}
        <button className="btn btn-primary" disabled={busy}>
          {busy ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </button>
      </form>

      <section className="card stack">
        <h2>Tài khoản mẫu để kiểm thử</h2>
        <p className="muted small">
          Mật khẩu chung: <code>{DEMO_PASSWORD}</code>. Bấm để điền nhanh.
        </p>
        <ul className="plain-list">
          {DEMO_ACCOUNTS.map((a) => (
            <li key={a.username}>
              <button
                type="button"
                className="btn btn-quiet"
                onClick={() => {
                  setUsername(a.username)
                  setPassword(DEMO_PASSWORD)
                }}
              >
                <code>{a.username}</code>
                <span className="muted small">{a.note}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>

      <p className="muted small center">
        Cần giả lập máy quét tại cổng? Mở <Link to="/kiosk">trang kiosk</Link> trên máy có webcam.
      </p>
    </main>
  )
}
