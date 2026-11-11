// The phone's key pair (ADR-12 device binding). The private key is generated as NON-EXTRACTABLE and
// kept in IndexedDB as a CryptoKey object: scripts can use it to sign but cannot export its bytes,
// which is the browser equivalent of the Android Keystore / iOS Secure Enclave of the real app.
// Only the public key ever leaves the device.

export interface StoredDevice {
  employeeUuid: string
  deviceId: string
  privateKey: CryptoKey
  registeredAt: string
}

const DB_NAME = 'tmc-employee-app'
const STORE = 'devices'

function openDb(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1)
    request.onupgradeneeded = () =>
      request.result.createObjectStore(STORE, { keyPath: 'employeeUuid' })
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error)
  })
}

async function withStore<T>(
  mode: IDBTransactionMode,
  run: (store: IDBObjectStore) => IDBRequest<T>,
): Promise<T> {
  const db = await openDb()
  try {
    return await new Promise<T>((resolve, reject) => {
      const request = run(db.transaction(STORE, mode).objectStore(STORE))
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error)
    })
  } finally {
    db.close()
  }
}

export async function loadDevice(employeeUuid: string): Promise<StoredDevice | undefined> {
  return withStore<StoredDevice | undefined>('readonly', (s) => s.get(employeeUuid))
}

export async function saveDevice(device: StoredDevice): Promise<void> {
  await withStore('readwrite', (s) => s.put(device))
}

export async function forgetDevice(employeeUuid: string): Promise<void> {
  await withStore('readwrite', (s) => s.delete(employeeUuid))
}

/** @returns the key pair and the public key as Base64 X.509 SubjectPublicKeyInfo, as user-qr expects */
export async function generateKeyPair(): Promise<{
  privateKey: CryptoKey
  publicKeyBase64: string
}> {
  const pair = await crypto.subtle.generateKey(
    { name: 'ECDSA', namedCurve: 'P-256' },
    false, // the private key can never be exported
    ['sign'],
  )
  const spki = new Uint8Array(await crypto.subtle.exportKey('spki', pair.publicKey))
  let binary = ''
  for (const b of spki) binary += String.fromCharCode(b)
  return { privateKey: pair.privateKey, publicKeyBase64: btoa(binary) }
}

/** WebCrypto and IndexedDB only exist in a secure context (HTTPS or localhost). */
export function secureContextProblem(): string | null {
  if (typeof crypto === 'undefined' || !crypto.subtle) {
    return 'Trình duyệt chặn WebCrypto vì trang không chạy trên HTTPS hoặc localhost. Trên điện thoại hãy mở bằng địa chỉ https:// (chạy "npm run dev:lan").'
  }
  return null
}
