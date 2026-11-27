import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  type Session,
  SessionExpiredError,
  authHeaders,
  loadSession,
  login,
  registerDevice,
  revokeDevice,
  saveSession,
} from './api'

type Fetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>

function memoryStorage(): Storage {
  const data = new Map<string, string>()
  return {
    get length() {
      return data.size
    },
    clear: () => data.clear(),
    getItem: (k) => data.get(k) ?? null,
    key: (i) => [...data.keys()][i] ?? null,
    removeItem: (k) => void data.delete(k),
    setItem: (k, v) => void data.set(k, String(v)),
  }
}

function respond(status: number, body: unknown) {
  return vi.fn<Fetch>(
    async () => new Response(body === null ? null : JSON.stringify(body), { status }),
  )
}

const session: Session = {
  username: 'nv001',
  subject: '0192a1c0-0000-7000-8000-000000000001',
  roles: ['EMPLOYEE'],
  accessToken: 'jwt-token',
  expiresAt: '2999-01-01T00:00:00Z',
}

beforeEach(() => vi.stubGlobal('sessionStorage', memoryStorage()))
afterEach(() => vi.unstubAllGlobals())

describe('employee app identity (D-13)', () => {
  it('keeps the access token of a granted login', async () => {
    vi.stubGlobal(
      'fetch',
      respond(200, {
        status: 'GRANTED',
        subject: session.subject,
        roles: ['EMPLOYEE'],
        accessToken: 'jwt-token',
        expiresAt: '2999-01-01T00:00:00Z',
      }),
    )

    expect(await login('nv001', 'pw')).toEqual({ ok: true, session })
  })

  it('sends the token as Bearer when registering a device', async () => {
    const fetch = respond(201, { deviceId: 'dev-1' })
    vi.stubGlobal('fetch', fetch)

    expect(await registerDevice(session, 'PUBKEY')).toBe('dev-1')

    const headers = fetch.mock.calls[0][1]?.headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer jwt-token')
    expect(headers).not.toHaveProperty('X-Subject')
  })

  it('falls back to development headers for a session saved without a token', () => {
    const h = authHeaders({ username: 'nv001', subject: session.subject, roles: ['EMPLOYEE'] })
    expect(h['X-Subject']).toBe(session.subject)
    expect(h).not.toHaveProperty('Authorization')
  })

  it('reports an expired session on 401 so the app goes back to the login page', async () => {
    vi.stubGlobal('fetch', respond(401, { error: 'UNAUTHENTICATED' }))

    await expect(registerDevice(session, 'PUBKEY')).rejects.toBeInstanceOf(SessionExpiredError)
    await expect(revokeDevice(session, 'dev-1')).rejects.toBeInstanceOf(SessionExpiredError)
  })

  it('does not call the server with a token that has already run out', async () => {
    const fetch = respond(201, { deviceId: 'dev-1' })
    vi.stubGlobal('fetch', fetch)
    const old = { ...session, expiresAt: '2000-01-01T00:00:00Z' }

    await expect(registerDevice(old, 'PUBKEY')).rejects.toBeInstanceOf(SessionExpiredError)
    expect(fetch).not.toHaveBeenCalled()

    saveSession(old)
    expect(loadSession()).toBeNull()
  })
})
