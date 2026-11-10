import { type DevIdentity, loadIdentity } from './identity'

/** Headers the gateway would inject. Pure function so it can be unit tested. */
export function identityHeaders(identity: DevIdentity): Record<string, string> {
  const headers: Record<string, string> = {
    'X-Subject': identity.subject,
    'X-Roles': identity.roles.join(','),
  }
  if (identity.factories) headers['X-Scope-Factories'] = identity.factories
  if (identity.gates) headers['X-Scope-Gates'] = identity.gates
  return headers
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public body: unknown,
  ) {
    super(`HTTP ${status}`)
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      'X-Correlation-Id': crypto.randomUUID(),
      ...identityHeaders(loadIdentity()),
      ...init.headers,
    },
  })
  const text = await response.text()
  const body = text ? JSON.parse(text) : null
  if (!response.ok) throw new ApiError(response.status, body)
  return body as T
}

/** Maps the 403 the services return for out-of-scope or wrong-role calls to a readable message. */
export function describeError(error: unknown): string {
  if (error instanceof ApiError) {
    if (error.status === 403) return 'Bạn không có quyền thực hiện thao tác này (HTTP 403).'
    if (error.status === 503) return 'Dịch vụ phụ thuộc tạm thời không khả dụng (HTTP 503).'
    return `Lỗi từ máy chủ (HTTP ${error.status}).`
  }
  return 'Không kết nối được máy chủ.'
}
