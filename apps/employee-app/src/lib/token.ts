// Builds the QR token exactly as ADR-12 defines it and as simulators/device-simulator/TokenSigner.java
// does: compact JWS, ES256, kid = deviceId, claims limited to sub, jti, iat, exp, fac, nonce.
// The private key is a non-extractable WebCrypto key: this code can ask it to sign, never read it.

export const TOKEN_TTL_SECONDS = 30

export interface TokenInput {
  deviceId: string
  employeeUuid: string
  factories: string[]
}

/** The options exist only to demonstrate rejections (same idea as device-sim's flags). */
export interface TokenOptions {
  /** Unix seconds; defaults to now. */
  issuedAt?: number
  ttlSeconds?: number
  /** Reuse a token id to demonstrate replay. */
  jti?: string
  /** Adds a readable name; the server must reject it (ADR-09). */
  addPii?: boolean
}

export interface TokenClaims {
  sub: string
  jti: string
  iat: number
  exp: number
  fac: string[]
  nonce: string
  [extra: string]: unknown
}

export function base64Url(bytes: Uint8Array): string {
  let binary = ''
  for (const b of bytes) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function encodeJson(value: unknown): string {
  return base64Url(new TextEncoder().encode(JSON.stringify(value)))
}

export function buildClaims(input: TokenInput, options: TokenOptions = {}): TokenClaims {
  const iat = options.issuedAt ?? Math.floor(Date.now() / 1000)
  const claims: TokenClaims = {
    sub: input.employeeUuid,
    jti: options.jti ?? crypto.randomUUID(),
    iat,
    exp: iat + (options.ttlSeconds ?? TOKEN_TTL_SECONDS),
    fac: input.factories,
    nonce: crypto.randomUUID().slice(0, 8),
  }
  if (options.addPii) claims.name = 'Nguyen Van A'
  return claims
}

export async function signToken(
  privateKey: CryptoKey,
  input: TokenInput,
  options: TokenOptions = {},
): Promise<{ token: string; claims: TokenClaims }> {
  const claims = buildClaims(input, options)
  const header = { alg: 'ES256', typ: 'JWT', kid: input.deviceId }
  const signingInput = `${encodeJson(header)}.${encodeJson(claims)}`
  // WebCrypto ECDSA returns r||s (64 bytes for P-256), which is exactly the JWS ES256 format.
  const signature = await crypto.subtle.sign(
    { name: 'ECDSA', hash: 'SHA-256' },
    privateKey,
    new TextEncoder().encode(signingInput),
  )
  return { token: `${signingInput}.${base64Url(new Uint8Array(signature))}`, claims }
}
