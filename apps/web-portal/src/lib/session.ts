// Signed-in session of the portal (D-13). The access token comes from identity's POST /auth/v1/login
// (ES256 JWT, 15 minutes, no refresh token) and is sent as Authorization: Bearer to every service.
// Kept in sessionStorage: it ends with the tab and is not shared with other tabs.

export interface Session {
  username: string
  subject: string
  roles: string[]
  accessToken: string
  /** ISO-8601 instant after which the services refuse the token. */
  expiresAt: string
}

const KEY = 'tmc.portal.session'

export function loadSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(KEY)
    if (!raw) return null
    const session = JSON.parse(raw) as Session
    return isExpired(session) ? null : session
  } catch {
    return null
  }
}

export function saveSession(session: Session | null): void {
  try {
    if (session) sessionStorage.setItem(KEY, JSON.stringify(session))
    else sessionStorage.removeItem(KEY)
  } catch {
    // private mode or storage blocked: the user signs in again after a reload
  }
}

export function isExpired(session: Session, now: Date = new Date()): boolean {
  const expires = Date.parse(session.expiresAt)
  return Number.isNaN(expires) || expires <= now.getTime()
}

export type LoginOutcome =
  | { ok: true; session: Session }
  | { ok: false; status: 'DENIED' | 'LOCKED' | 'MFA_REQUIRED' | 'UNREACHABLE' }

interface LoginBody {
  status?: string
  subject?: string
  roles?: string[]
  accessToken?: string
  expiresAt?: string
  expiresIn?: number
}

/** POST /auth/v1/login. HR staff and system administrators also need the 6-digit TOTP code. */
export async function login(
  username: string,
  password: string,
  mfaCode?: string,
): Promise<LoginOutcome> {
  let response: Response
  try {
    response = await fetch('/auth/v1/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(mfaCode ? { username, password, mfaCode } : { username, password }),
    })
  } catch {
    return { ok: false, status: 'UNREACHABLE' }
  }
  if (response.status >= 500) return { ok: false, status: 'UNREACHABLE' }
  const body = (await response.json().catch(() => null)) as LoginBody | null
  if (!body?.status) return { ok: false, status: 'UNREACHABLE' }
  if (body.status !== 'GRANTED' || !body.subject || !body.accessToken) {
    const status =
      body.status === 'LOCKED' || body.status === 'MFA_REQUIRED' ? body.status : 'DENIED'
    return { ok: false, status }
  }
  const expiresAt =
    body.expiresAt ?? new Date(Date.now() + (body.expiresIn ?? 900) * 1000).toISOString()
  return {
    ok: true,
    session: {
      username,
      subject: body.subject,
      roles: body.roles ?? [],
      accessToken: body.accessToken,
      expiresAt,
    },
  }
}
