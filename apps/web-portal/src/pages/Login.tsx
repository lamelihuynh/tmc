import { type FormEvent, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { type Session, login } from '../lib/session'

const MESSAGES = {
  DENIED: 'Sai tên đăng nhập, mật khẩu hoặc mã xác thực.',
  LOCKED: 'Tài khoản tạm khóa 15 phút sau 5 lần nhập sai.',
  MFA_REQUIRED: 'Tài khoản này cần mã xác thực 6 số từ ứng dụng Authenticator.',
  UNREACHABLE: 'Không kết nối được dịch vụ đăng nhập (identity, cổng 8087).',
} as const

/** Sign-in against identity (D-13). HR staff and system administrators also enter a TOTP code. */
export default function Login({ onSignedIn }: { onSignedIn: (session: Session) => void }) {
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [mfaCode, setMfaCode] = useState('')
  const [needsMfa, setNeedsMfa] = useState(false)
  const [message, setMessage] = useState<string | null>(
    params.get('expired') ? 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.' : null,
  )
  const [busy, setBusy] = useState(false)

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    const result = await login(username.trim(), password, needsMfa ? mfaCode.trim() : undefined)
    setBusy(false)
    if (result.ok) {
      onSignedIn(result.session)
      navigate('/', { replace: true })
      return
    }
    if (result.status === 'MFA_REQUIRED') setNeedsMfa(true)
    setMessage(MESSAGES[result.status])
  }

  return (
    <form className="login" onSubmit={submit} aria-label="Đăng nhập">
      <h2>Đăng nhập</h2>
      <label>
        Tên đăng nhập
        <input
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
          required
        />
      </label>
      <label>
        Mật khẩu
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
          required
        />
      </label>
      {needsMfa && (
        <label>
          Mã xác thực (6 số)
          <input
            value={mfaCode}
            onChange={(e) => setMfaCode(e.target.value)}
            inputMode="numeric"
            autoComplete="one-time-code"
            pattern="[0-9]{6}"
            required
          />
        </label>
      )}
      <button type="submit" disabled={busy}>
        {busy ? 'Đang đăng nhập...' : 'Đăng nhập'}
      </button>
      {message && <p role="alert">{message}</p>}
    </form>
  )
}
