import { useEffect, useState } from 'react'
import { Link, NavLink, Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { setUnauthenticatedHandler } from './lib/api'
import { ALL_ROLES, type DevIdentity, type Role, loadIdentity, saveIdentity } from './lib/identity'
import { type Session, loadSession, saveSession } from './lib/session'
import HrReconciliation from './pages/HrReconciliation'
import Login from './pages/Login'
import SecurityGates from './pages/SecurityGates'
import SupervisionPresence from './pages/SupervisionPresence'

/**
 * Dev-only identity switcher, shown while nobody is signed in. Its headers are accepted only by
 * services running the local profile; the default profile needs a signed-in token (D-13).
 */
function DevIdentityBar() {
  const [identity, setIdentity] = useState<DevIdentity>(loadIdentity())
  const update = (next: DevIdentity) => {
    setIdentity(next)
    saveIdentity(next)
  }
  return (
    <section className="dev-bar" aria-label="Danh tính phát triển">
      <strong>DEV</strong>
      <label>
        Vai trò
        <select
          value={identity.roles[0]}
          onChange={(e) => update({ ...identity, roles: [e.target.value as Role] })}
        >
          {ALL_ROLES.map((r) => (
            <option key={r}>{r}</option>
          ))}
        </select>
      </label>
      <label>
        Nhà máy
        <input
          value={identity.factories}
          onChange={(e) => update({ ...identity, factories: e.target.value })}
        />
      </label>
      <label>
        Cổng
        <input
          value={identity.gates}
          onChange={(e) => update({ ...identity, gates: e.target.value })}
        />
      </label>
    </section>
  )
}

/** Who is signed in, and the way out. */
function SessionBar({ session, onSignOut }: { session: Session; onSignOut: () => void }) {
  return (
    <section className="session-bar" aria-label="Phiên đăng nhập">
      <span>
        {session.username} ({session.roles.join(', ')})
      </span>
      <button type="button" onClick={onSignOut}>
        Đăng xuất
      </button>
    </section>
  )
}

export default function App() {
  const [session, setSession] = useState<Session | null>(loadSession())
  const navigate = useNavigate()

  useEffect(() => {
    // a service answered 401: the token expired or was refused, so sign in again
    setUnauthenticatedHandler(() => {
      saveSession(null)
      setSession(null)
      navigate('/login?expired=1', { replace: true })
    })
  }, [navigate])

  const signIn = (next: Session) => {
    saveSession(next)
    setSession(next)
  }
  const signOut = () => {
    saveSession(null)
    setSession(null)
    navigate('/login', { replace: true })
  }

  return (
    <div className="shell">
      <header>
        <h1>TMC - Kiểm soát ra vào và chấm công</h1>
        <nav>
          <NavLink to="/security/gates">Bảo vệ: cổng</NavLink>
          <NavLink to="/supervision/presence">Quản lý: hiện diện</NavLink>
          <NavLink to="/hr/reconciliation">HR: đối soát</NavLink>
        </nav>
        {session ? (
          <SessionBar session={session} onSignOut={signOut} />
        ) : (
          <>
            <Link to="/login">Đăng nhập</Link>
            <DevIdentityBar />
          </>
        )}
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/security/gates" replace />} />
          <Route path="/login" element={<Login onSignedIn={signIn} />} />
          <Route path="/security/gates" element={<SecurityGates />} />
          <Route path="/supervision/presence" element={<SupervisionPresence />} />
          <Route path="/hr/reconciliation" element={<HrReconciliation />} />
        </Routes>
      </main>
    </div>
  )
}
