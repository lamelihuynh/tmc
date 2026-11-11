// HTTP calls of the employee app. Every path goes through the Vite proxy (see vite.config.ts).
//
// DEV-ONLY identity: identity does not issue a JWT yet (Team C, story C-04). Until then the login
// answer tells the app who it is, and the app sends that back as X-Subject / X-Roles, the same
// headers the web portal's dev bar and device-simulator use. In production the gateway sets them.

export interface Session {
  username: string
  subject: string
  roles: string[]
}

const SESSION_KEY = 'tmc.employee.session'

export function loadSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as Session) : null
  } catch {
    return null
  }
}

export function saveSession(session: Session | null): void {
  try {
    if (session) sessionStorage.setItem(SESSION_KEY, JSON.stringify(session))
    else sessionStorage.removeItem(SESSION_KEY)
  } catch {
    // private mode: the session then lasts until the tab reloads
  }
}

export type LoginResult =
  | { ok: true; session: Session }
  | { ok: false; status: 'DENIED' | 'LOCKED' | 'MFA_REQUIRED' | 'NOT_EMPLOYEE' | 'UNREACHABLE' }

export async function login(username: string, password: string): Promise<LoginResult> {
  let response: Response
  try {
    response = await fetch('/auth/v1/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    })
  } catch {
    return { ok: false, status: 'UNREACHABLE' }
  }
  if (response.status >= 500) return { ok: false, status: 'UNREACHABLE' }
  const body = (await response.json().catch(() => null)) as {
    status?: string
    subject?: string
    roles?: string[]
  } | null
  if (!body?.status) return { ok: false, status: 'UNREACHABLE' }
  if (body.status !== 'GRANTED' || !body.subject) {
    return { ok: false, status: body.status as 'DENIED' | 'LOCKED' | 'MFA_REQUIRED' }
  }
  const roles = body.roles ?? []
  if (!roles.includes('EMPLOYEE')) return { ok: false, status: 'NOT_EMPLOYEE' }
  return { ok: true, session: { username, subject: body.subject, roles } }
}

function identityHeaders(session: Session): Record<string, string> {
  return { 'X-Subject': session.subject, 'X-Roles': 'EMPLOYEE' }
}

/** POST /self-service/v1/devices (user-qr). Registering replaces the employee's previous device. */
export async function registerDevice(session: Session, publicKeyBase64: string): Promise<string> {
  const response = await fetch('/self-service/v1/devices', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...identityHeaders(session) },
    body: JSON.stringify({ publicKey: publicKeyBase64 }),
  })
  if (response.status !== 201)
    throw new Error(`Đăng ký thiết bị thất bại (HTTP ${response.status})`)
  return ((await response.json()) as { deviceId: string }).deviceId
}

/** DELETE /self-service/v1/devices/{deviceId} (user-qr). */
export async function revokeDevice(session: Session, deviceId: string): Promise<void> {
  const response = await fetch(`/self-service/v1/devices/${encodeURIComponent(deviceId)}`, {
    method: 'DELETE',
    headers: identityHeaders(session),
  })
  // 403 also covers "the server no longer knows this device" (its registry was restarted)
  if (response.status !== 204 && response.status !== 403) {
    throw new Error(`Thu hồi thiết bị thất bại (HTTP ${response.status})`)
  }
}

export interface GateInfo {
  gateId: string
  factoryCode: string
  mode: 'ENTRY_ONLY' | 'EXIT_ONLY' | 'BIDIRECTIONAL'
  maintenance: boolean
}

/** GET /kiosk/v1/sync-bundle (gate-station): the gate catalog a kiosk works from. */
export async function loadGates(): Promise<GateInfo[]> {
  const response = await fetch('/kiosk/v1/sync-bundle')
  if (!response.ok) throw new Error(`HTTP ${response.status}`)
  return ((await response.json()) as { gates: GateInfo[] }).gates
}

export type ScanOutcome = 'ALLOWED' | 'DENIED' | 'PENDING' | 'IN_PROGRESS' | 'UNCONFIRMED'

export interface ScanResult {
  outcome: ScanOutcome | 'ERROR'
  reasonCode: string | null
  direction: 'ENTRY' | 'EXIT' | null
  attemptId: string | null
  duplicate: boolean
  httpStatus: number
}

/**
 * POST /kiosk/v1/scans (gate-station), what a kiosk sends for one scan. Idempotency-Key is the
 * attempt id: retrying with the same key can never open the gate twice (ADR-11).
 */
export async function submitScan(
  gateId: string,
  qrToken: string,
  kioskId: string,
  attemptId: string = crypto.randomUUID(),
): Promise<ScanResult> {
  let response: Response
  try {
    response = await fetch('/kiosk/v1/scans', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Idempotency-Key': attemptId,
        'X-Kiosk-Id': kioskId,
        'X-Correlation-Id': crypto.randomUUID(),
      },
      body: JSON.stringify({
        gate_id: gateId,
        qr_token: qrToken,
        scanned_at: new Date().toISOString(),
      }),
    })
  } catch {
    return errorResult(attemptId, 0, 'EDGE_UNREACHABLE')
  }
  const body = (await response.json().catch(() => null)) as {
    attempt_id?: string
    outcome?: ScanOutcome
    reason_code?: string | null
    direction?: 'ENTRY' | 'EXIT' | null
    duplicate?: boolean
    error?: string
  } | null
  if (!body?.outcome) {
    return errorResult(attemptId, response.status, body?.error ?? `HTTP_${response.status}`)
  }
  return {
    outcome: body.outcome,
    reasonCode: body.reason_code ?? null,
    direction: body.direction ?? null,
    attemptId: body.attempt_id ?? attemptId,
    duplicate: body.duplicate ?? false,
    httpStatus: response.status,
  }
}

function errorResult(attemptId: string, httpStatus: number, reasonCode: string): ScanResult {
  return { outcome: 'ERROR', reasonCode, direction: null, attemptId, duplicate: false, httpStatus }
}

const REASONS: Record<string, string> = {
  QR_EXPIRED: 'Mã QR đã hết hạn',
  QR_ALREADY_USED: 'Mã QR này đã được dùng',
  INVALID_SIGNATURE: 'Chữ ký không hợp lệ',
  MALFORMED_TOKEN: 'Mã QR sai định dạng hoặc chứa dữ liệu không được phép',
  DEVICE_UNKNOWN: 'Thiết bị chưa được đăng ký',
  DEVICE_REVOKED: 'Thiết bị đã bị thu hồi',
  CLOCK_SKEW_EXCEEDED: 'Đồng hồ thiết bị lệch quá 60 giây',
  EMPLOYEE_INACTIVE: 'Nhân viên không còn hoạt động',
  FACTORY_NOT_PERMITTED: 'Không có quyền vào nhà máy này',
  ANTI_PASSBACK_VIOLATION: 'Vi phạm anti-passback (hai lượt cùng chiều liên tiếp)',
  GATE_IN_MAINTENANCE: 'Cổng đang bảo trì',
  GATE_NOT_FOUND: 'Không tìm thấy cổng',
  TURNSTILE_TIMEOUT_RETRY_SCAN: 'Cổng chưa phản hồi, hệ thống đang đối soát',
  GATE_HARDWARE_ERROR: 'Cổng báo lỗi phần cứng',
  IN_PROGRESS: 'Lượt quét trước đang được xử lý',
  EDGE_UNREACHABLE: 'Không kết nối được máy chủ biên (gate-station)',
  DEPENDENCY_UNAVAILABLE: 'Một dịch vụ phía sau đang tắt (attendance hoặc user-qr)',
}

export function describeReason(code: string | null): string {
  if (!code) return ''
  return REASONS[code] ?? code
}
