// Regenerates the web-portal screenshots (guard, supervisor, HR) in docs/screenshots from the REAL
// portal. The supervisor and HR pages only have data when events travel through RabbitMQ, so this
// needs the full stack on real infrastructure, not the "local" profile:
//
//   docker compose -f docker/docker-compose.yml up -d
//   start all 8 services with the default profile   (docs/handover/02-chay-local.md section 9)
//   npm run dev in apps/employee-app (5174) and in apps/web-portal (5173)
//   cd scripts/docs && node capture-portal.mjs
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import puppeteer from 'puppeteer-core'

const HERE = dirname(fileURLToPath(import.meta.url))
const OUT = join(HERE, '..', '..', 'docs', 'screenshots')
const PORTAL = process.env.PORTAL_URL ?? 'http://localhost:5173'
const APP = process.env.APP_URL ?? 'http://localhost:5174'
const chrome =
  process.env.CHROME_PATH ??
  [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
  ].find(existsSync)
if (!chrome) throw new Error('Chrome not found; set CHROME_PATH')
mkdirSync(OUT, { recursive: true })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

// ---------- put two employees inside the BD factory, through the same calls the app makes ----------
const b64u = (bytes) => Buffer.from(bytes).toString('base64url')
const enc = (o) => b64u(new TextEncoder().encode(JSON.stringify(o)))
async function enterFactory(username, gate) {
  const login = await (
    await fetch(`${APP}/auth/v1/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password: 'dev-only-change-me' }),
    })
  ).json()
  const pair = await crypto.subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign'])
  const spki = Buffer.from(await crypto.subtle.exportKey('spki', pair.publicKey)).toString('base64')
  const { deviceId } = await (
    await fetch(`${APP}/self-service/v1/devices`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'X-Subject': login.subject, 'X-Roles': 'EMPLOYEE' },
      body: JSON.stringify({ publicKey: spki }),
    })
  ).json()
  const iat = Math.floor(Date.now() / 1000)
  const input =
    enc({ alg: 'ES256', typ: 'JWT', kid: deviceId }) +
    '.' +
    enc({ sub: login.subject, jti: crypto.randomUUID(), iat, exp: iat + 30, fac: ['BD', 'LA'], nonce: 'doc00001' })
  const sig = await crypto.subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, pair.privateKey, new TextEncoder().encode(input))
  const res = await fetch(`${APP}/kiosk/v1/scans`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': crypto.randomUUID(), 'X-Kiosk-Id': 'DOCS' },
    body: JSON.stringify({ gate_id: gate, qr_token: `${input}.${b64u(new Uint8Array(sig))}`, scanned_at: new Date().toISOString() }),
  })
  const body = await res.json()
  console.log(`${username} at ${gate}: ${body.outcome} ${body.reason_code ?? ''}`)
}
await enterFactory('nv004', 'BD-GATE-01')
await enterFactory('nv005', 'BD-GATE-01')
await sleep(4000) // outbox publisher runs every second; give the consumers time

// ---------- the portal, one identity per role ----------
const IDENTITIES = {
  guard: { subject: '0192a1c0-0000-7000-8000-00000000b001', roles: ['SECURITY_GUARD'], factories: 'BD', gates: 'BD-GATE-01' },
  supervisor: { subject: '0192a1c0-0000-7000-8000-00000000a001', roles: ['PRODUCTION_SUPERVISOR'], factories: 'BD', gates: '' },
  admin: { subject: '0192a1c0-0000-7000-8000-00000000a002', roles: ['SYSTEM_ADMIN'], factories: '*', gates: '' },
}

const browser = await puppeteer.launch({ executablePath: chrome, headless: true })
try {
  async function open(identity, navText) {
    const page = await browser.newPage()
    await page.setViewport({ width: 1100, height: 760, deviceScaleFactor: 2 })
    await page.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }])
    await page.evaluateOnNewDocument((id) => localStorage.setItem('tmc.dev.identity', JSON.stringify(id)), identity)
    // Always enter through "/": a direct load of /security/... would hit the dev proxy, not the app.
    await page.goto(PORTAL, { waitUntil: 'networkidle0' })
    await clickText(page, navText, 'a')
    await sleep(500)
    return page
  }
  async function clickText(page, text, selector = 'button') {
    const handle = await page.evaluateHandle(
      (sel, t) => [...document.querySelectorAll(sel)].find((e) => e.textContent.trim().includes(t)),
      selector,
      text,
    )
    const element = handle.asElement()
    if (!element) throw new Error(`no ${selector} with text "${text}"`)
    await element.click()
  }
  const save = async (page, name) =>
    writeFileSync(join(OUT, name), await page.screenshot({ type: 'png', fullPage: true }))

  // Security guard: manual open with a mandatory reason (UC-06)
  const guard = await open(IDENTITIES.guard, 'Bảo vệ')
  const inputs = await guard.$$('form input')
  await inputs[0].type('0192a1c0-0000-7000-8000-000000000001')
  await inputs[1].type('Điện thoại hết pin, đã đối chiếu thẻ nhân viên')
  await guard.click('form button[type="submit"]')
  await guard.waitForFunction(() => /ALLOWED|DENIED|PENDING/.test(document.body.innerText), { timeout: 15000 })
  await save(guard, '09-bao-ve-mo-cong-thu-cong.png')

  // Production supervisor: who is inside the factory right now
  const supervisor = await open(IDENTITIES.supervisor, 'Quản lý')
  await clickText(supervisor, 'Tải')
  await supervisor.waitForFunction(() => document.querySelectorAll('tbody tr, li').length > 0, { timeout: 15000 })
  await save(supervisor, '10-quan-doc-hien-dien.png')

  // HR sync monitor (only the system administrator may read it)
  const admin = await open(IDENTITIES.admin, 'HR')
  await admin.select('main select, select:not(.dev-bar select)', 'SYNCED').catch(async () => {
    const selects = await admin.$$('select')
    await selects[selects.length - 1].select('SYNCED')
  })
  await clickText(admin, 'Tải')
  await admin.waitForFunction(() => document.querySelectorAll('tbody tr').length > 0, { timeout: 15000 })
  await save(admin, '11-hr-dong-bo.png')
} finally {
  await browser.close()
}
console.log('wrote 3 portal screenshots to', OUT)
