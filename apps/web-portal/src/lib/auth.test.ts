import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, api, authHeaders, describeError, setUnauthenticatedHandler } from './api'
import { DEFAULT_IDENTITY } from './identity'
import { type Session, isExpired, loadSession, login, saveSession } from './session'

/** sessionStorage and localStorage stand-ins: the tests run in node, without a DOM. */
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

const session: Session = {
  username: 'supervisor1',
  subject: '0192a1c0-0000-7000-8000-00000000a001',
  roles: ['PRODUCTION_SUPERVISOR'],
  accessToken: 'header.payload.signature',
  expiresAt: '2999-01-01T00:00:00Z',
}

type Fetch = (input: RequestInfo | URL, init?: RequestInit) => Promise<Response>

function respond(status: number, body: unknown) {
  return vi.fn<Fetch>(
    async () => new Response(body === null ? '' : JSON.stringify(body), { status }),
  )
}

beforeEach(() => {
  vi.stubGlobal('sessionStorage', memoryStorage())
  vi.stubGlobal('localStorage', memoryStorage())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('authHeaders', () => {
  it('sends the bearer token when signed in, and no development headers', () => {
    const h = authHeaders(session, DEFAULT_IDENTITY)
    expect(h).toEqual({ Authorization: 'Bearer header.payload.signature' })
  })

  it('falls back to the development headers when nobody is signed in', () => {
    const h = authHeaders(null, DEFAULT_IDENTITY)
    expect(h).not.toHaveProperty('Authorization')
    expect(h['X-Subject']).toBe(DEFAULT_IDENTITY.subject)
  })
})

describe('api', () => {
  it('puts the session token on every call', async () => {
    saveSession(session)
    const fetch = respond(200, [])
    vi.stubGlobal('fetch', fetch)

    await api('/supervision/v1/presence?factory=BD')

    const init = fetch.mock.calls[0][1] as RequestInit
    expect((init.headers as Record<string, string>).Authorization).toBe(
      'Bearer header.payload.signature',
    )
  })

  it('on 401 forgets the session and sends the user back to the login page', async () => {
    saveSession(session)
    vi.stubGlobal('fetch', respond(401, { error: 'UNAUTHENTICATED' }))
    const toLogin = vi.fn()
    setUnauthenticatedHandler(toLogin)

    await expect(api('/admin/v1/audit')).rejects.toBeInstanceOf(ApiError)

    expect(toLogin).toHaveBeenCalledOnce()
    expect(loadSession()).toBeNull()
  })

  it('keeps the session on 403: signed in, but not allowed', async () => {
    saveSession(session)
    vi.stubGlobal('fetch', respond(403, { error: 'FORBIDDEN' }))
    const toLogin = vi.fn()
    setUnauthenticatedHandler(toLogin)

    await expect(api('/admin/v1/audit')).rejects.toBeInstanceOf(ApiError)

    expect(toLogin).not.toHaveBeenCalled()
    expect(loadSession()).not.toBeNull()
  })

  it('explains a 401 in plain language', () => {
    expect(describeError(new ApiError(401, null))).toContain('401')
  })
})

describe('session', () => {
  it('drops an expired session when it is loaded', () => {
    saveSession({ ...session, expiresAt: '2000-01-01T00:00:00Z' })
    expect(loadSession()).toBeNull()
    expect(isExpired({ ...session, expiresAt: 'not a date' })).toBe(true)
  })

  it('keeps the token and expiry of a granted login', async () => {
    vi.stubGlobal(
      'fetch',
      respond(200, {
        status: 'GRANTED',
        subject: session.subject,
        roles: session.roles,
        accessToken: 'jwt',
        tokenType: 'Bearer',
        expiresIn: 900,
        expiresAt: '2999-01-01T00:15:00Z',
      }),
    )

    const result = await login('supervisor1', 'pw')

    expect(result).toEqual({
      ok: true,
      session: { ...session, accessToken: 'jwt', expiresAt: '2999-01-01T00:15:00Z' },
    })
  })

  it('asks for the TOTP code when identity requires it, and sends it next time', async () => {
    const fetch = respond(401, { status: 'MFA_REQUIRED' })
    vi.stubGlobal('fetch', fetch)

    expect(await login('hr1', 'pw')).toEqual({ ok: false, status: 'MFA_REQUIRED' })
    await login('hr1', 'pw', '123456')

    const body = JSON.parse((fetch.mock.calls[1][1] as RequestInit).body as string)
    expect(body.mfaCode).toBe('123456')
  })

  it('reports an unreachable identity service', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new TypeError('failed to fetch')
      }),
    )
    expect(await login('x', 'y')).toEqual({ ok: false, status: 'UNREACHABLE' })
  })
})
