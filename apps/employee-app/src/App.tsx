import { useState } from 'react'
import { Route, Routes } from 'react-router-dom'
import { type Session, loadSession, saveSession } from './lib/api'
import Home from './pages/Home'
import Kiosk from './pages/Kiosk'
import Login from './pages/Login'

/**
 * Two screens that stand in for two physical things:
 *   /       the employee's phone (login, device enrollment, rotating QR)
 *   /kiosk  the kiosk at the gate (camera scans the QR and calls gate-station)
 */
export default function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession())

  function update(next: Session | null) {
    saveSession(next)
    setSession(next)
  }

  return (
    <Routes>
      <Route path="/kiosk" element={<Kiosk />} />
      <Route
        path="*"
        element={
          session ? (
            <Home session={session} onLogout={() => update(null)} />
          ) : (
            <Login onLogin={update} />
          )
        }
      />
    </Routes>
  )
}
