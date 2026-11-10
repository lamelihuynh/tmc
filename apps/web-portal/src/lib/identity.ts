// DEV-ONLY identity. In production the API Gateway validates the JWT issued by the Identity service and
// injects these headers; the browser never sets them. TODO Team D/C: real login page against /auth/v1/login.
export type Role =
  | 'EMPLOYEE'
  | 'PRODUCTION_SUPERVISOR'
  | 'HR_STAFF'
  | 'SECURITY_GUARD'
  | 'SYSTEM_ADMIN'
  | 'EXECUTIVE'

export const ALL_ROLES: Role[] = [
  'EMPLOYEE',
  'PRODUCTION_SUPERVISOR',
  'HR_STAFF',
  'SECURITY_GUARD',
  'SYSTEM_ADMIN',
  'EXECUTIVE',
]

export interface DevIdentity {
  subject: string
  roles: Role[]
  factories: string
  gates: string
}

export const DEFAULT_IDENTITY: DevIdentity = {
  subject: '0192a1c0-0000-7000-8000-00000000b001',
  roles: ['SECURITY_GUARD'],
  factories: 'BD',
  gates: 'BD-GATE-01',
}

const KEY = 'tmc.dev.identity'

export function loadIdentity(): DevIdentity {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? { ...DEFAULT_IDENTITY, ...JSON.parse(raw) } : DEFAULT_IDENTITY
  } catch {
    return DEFAULT_IDENTITY
  }
}

export function saveIdentity(identity: DevIdentity): void {
  localStorage.setItem(KEY, JSON.stringify(identity))
}
