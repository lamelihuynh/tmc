import { useState } from 'react'
import { NavLink, Navigate, Route, Routes } from 'react-router-dom'
import { ALL_ROLES, type DevIdentity, type Role, loadIdentity, saveIdentity } from './lib/identity'
import HrReconciliation from './pages/HrReconciliation'
import SecurityGates from './pages/SecurityGates'
import SupervisionPresence from './pages/SupervisionPresence'

/** Dev-only identity switcher: stands in for login until the Identity service issues tokens. */
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

export default function App() {
  return (
    <div className="shell">
      <header>
        <h1>TMC - Kiểm soát ra vào và chấm công</h1>
        <nav>
          <NavLink to="/security/gates">Bảo vệ: cổng</NavLink>
          <NavLink to="/supervision/presence">Quản lý: hiện diện</NavLink>
          <NavLink to="/hr/reconciliation">HR: đối soát</NavLink>
        </nav>
        <DevIdentityBar />
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/security/gates" replace />} />
          <Route path="/security/gates" element={<SecurityGates />} />
          <Route path="/supervision/presence" element={<SupervisionPresence />} />
          <Route path="/hr/reconciliation" element={<HrReconciliation />} />
        </Routes>
      </main>
    </div>
  )
}
