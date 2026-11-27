import { type DevIdentity, loadIdentity } from './identity'
import { type Session, loadSession, saveSession } from './session'

/** Development headers (local profile only). Pure function so it can be unit tested. */
export function identityHeaders(identity: DevIdentity): Record<string, string> {
  const headers: Record<string, string> = {
    'X-Subject': identity.subject,
    'X-Roles': identity.roles.join(','),
  }
  if (identity.factories) headers['X-Scope-Factories'] = identity.factories
  if (identity.gates) headers['X-Scope-Gates'] = identity.gates
  return headers
}

/**
 * Who the request is from (D-13): the signed-in session's token, or, with no session, the
 * development identity bar. The services take the token whenever one is sent and accept the
 * development headers only on the local profile.
 */
export function authHeaders(
  session: Session | null,
  identity: DevIdentity,
): Record<string, string> {
  return session ? { Authorization: `Bearer ${session.accessToken}` } : identityHeaders(identity)
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
  ) {
    super(`HTTP ${status}`)
  }
}

/** Where to go when a service refuses the token (expired, signed by an old key, signed out). */
let onUnauthenticated: () => void = () => {
  window.location.assign('/login?expired=1')
}

/** For tests and for the router: replaces the 401 handler. */
export function setUnauthenticatedHandler(handler: () => void): void {
  onUnauthenticated = handler
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = loadSession()
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-Id': crypto.randomUUID(),
      ...authHeaders(session, loadIdentity()),
      ...init.headers,
    },
  })
  const text = await response.text()
  const body = text ? JSON.parse(text) : null
  if (response.status === 401) {
    saveSession(null)
    onUnauthenticated()
  }
  if (!response.ok) throw new ApiError(response.status, body)
  return body as T
}

/** Maps the statuses the services return to a readable message, without server internals. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 401) return 'Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập (HTTP 401).'
    if (error.status === 403) return 'Bạn không có quyền thực hiện thao tác này (HTTP 403).'
    if (error.status === 503) return 'Dịch vụ phụ thuộc tạm thời không khả dụng (HTTP 503).'
    return `Lỗi từ máy chủ (HTTP ${error.status}).`
  }
  return 'Không kết nối được máy chủ.'
}
