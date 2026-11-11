import { describe, expect, it } from 'vitest'
import { base64Url, buildClaims, signToken } from './token'

const input = {
  deviceId: 'dev-test',
  employeeUuid: '0192a1c0-0000-7000-8000-000000000001',
  factories: ['BD', 'LA'],
}

function decodePart(part: string): Record<string, unknown> {
  return JSON.parse(Buffer.from(part, 'base64url').toString('utf8'))
}

describe('QR token (ADR-12)', () => {
  it('carries only the six allowed claims and lives 30 seconds', () => {
    const claims = buildClaims(input, { issuedAt: 1000 })
    expect(Object.keys(claims).sort()).toEqual(['exp', 'fac', 'iat', 'jti', 'nonce', 'sub'])
    expect(claims.exp - claims.iat).toBe(30)
    expect(claims.sub).toBe(input.employeeUuid)
  })

  it('uses a new token id every time unless one is forced', () => {
    expect(buildClaims(input).jti).not.toBe(buildClaims(input).jti)
    expect(buildClaims(input, { jti: 'fixed' }).jti).toBe('fixed')
  })

  it('signs an ES256 compact JWS whose kid is the device id and whose signature verifies', async () => {
    const keys = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, false, [
      'sign',
      'verify',
    ])
    const { token } = await signToken(keys.privateKey, input)
    const [header, payload, signature] = token.split('.')

    expect(decodePart(header)).toEqual({ alg: 'ES256', typ: 'JWT', kid: 'dev-test' })
    expect(decodePart(payload).sub).toBe(input.employeeUuid)
    const signatureBytes = Buffer.from(signature, 'base64url')
    expect(signatureBytes.length).toBe(64) // r||s, not DER
    const ok = await crypto.subtle.verify(
      { name: 'ECDSA', hash: 'SHA-256' },
      keys.publicKey,
      signatureBytes,
      new TextEncoder().encode(`${header}.${payload}`),
    )
    expect(ok).toBe(true)
  })

  it('encodes base64url without padding or URL-unsafe characters', () => {
    expect(base64Url(new Uint8Array([251, 255, 254]))).toBe('-__-')
    expect(base64Url(new Uint8Array([1]))).toBe('AQ')
  })
})
