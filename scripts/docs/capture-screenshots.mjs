// Regenerates docs/screenshots/*.png and docs/screenshots/employee-app-demo.gif by driving the REAL
// employee app against the REAL local stack in headless Chrome. Nothing here is a mock-up: if the
// stack is down or a flow breaks, this script fails instead of producing pictures.
//
// Prerequisites: local stack running (docs/handover/02-chay-local.md section 3) and
// `npm run dev` in apps/employee-app (http://localhost:5174).
//
//   cd scripts/docs && npm install && node capture-screenshots.mjs
//
// CHROME_PATH overrides the browser location.
import { existsSync, mkdirSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import gifenc from 'gifenc'
import { PNG } from 'pngjs'
import puppeteer from 'puppeteer-core'

const { GIFEncoder, quantize, applyPalette } = gifenc
const HERE = dirname(fileURLToPath(import.meta.url))
const OUT = join(HERE, '..', '..', 'docs', 'screenshots')
const APP = process.env.APP_URL ?? 'http://localhost:5174'
const PHONE = { width: 390, height: 844, deviceScaleFactor: 2, isMobile: true, hasTouch: true }
const USER = 'nv001'

const chrome =
  process.env.CHROME_PATH ??
  [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Google/Chrome/Application/chrome.exe',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
  ].find(existsSync)
if (!chrome) throw new Error('Chrome not found; set CHROME_PATH')

mkdirSync(OUT, { recursive: true })
const sleep = (ms) => new Promise((r) => setTimeout(r, ms))
const frames = []

async function clickText(page, text, selector = 'button') {
  const handle = await page.evaluateHandle(
    (sel, t) => [...document.querySelectorAll(sel)].find((e) => e.textContent.trim().includes(t)),
    selector,
    text,
  )
  const element = handle.asElement()
  if (!element) throw new Error(`no ${selector} with text "${text}"`)
  await element.evaluate((e) => e.scrollIntoView({ block: 'center' }))
  await element.click()
}

async function waitText(page, text) {
  await page.waitForFunction((t) => document.body.innerText.includes(t), { timeout: 15000 }, text)
}

async function scrollTo(page, text) {
  await page.evaluate((t) => {
    const el = [...document.querySelectorAll('h2, summary')].find((e) => e.textContent.includes(t))
    el?.closest('.card')?.scrollIntoView({ block: 'center' })
  }, text)
  await sleep(150)
}

/** Screenshot of the phone viewport; also kept as a GIF frame shown for `hold` seconds. */
async function shot(page, name, hold = 1.6) {
  const png = Buffer.from(await page.screenshot({ type: 'png' }))
  frames.push({ png, hold })
  if (name) raw[name] = png
}
const raw = {}

const browser = await puppeteer.launch({ executablePath: chrome, headless: true })
try {
  // ---------- the phone ----------
  const phone = await browser.newPage()
  await phone.setViewport(PHONE)
  await phone.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }])
  // The clipboard is not available headless; remember what the app copies so the kiosk can use it.
  await phone.evaluateOnNewDocument(() => {
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText: async (text) => void (window.__copied = text) },
    })
  })
  await phone.goto(APP, { waitUntil: 'networkidle0' })
  await shot(phone, '01-dang-nhap')

  await clickText(phone, USER)
  await shot(phone, null, 1)
  await clickText(phone, 'Đăng nhập')
  await waitText(phone, 'Đăng ký thiết bị này')
  await shot(phone, '02-dang-ky-thiet-bi')

  await clickText(phone, 'Tạo khóa và đăng ký')
  await waitText(phone, 'Chế độ kiểm thử')
  await sleep(400)
  await shot(phone, '03-ma-qr', 2.2)

  await scrollTo(phone, 'Thử ngay không cần máy quét')
  await shot(phone, null, 1)
  await clickText(phone, 'Gửi tới cổng')
  await waitText(phone, 'CỔNG MỞ')
  await scrollTo(phone, 'Thử ngay không cần máy quét')
  await shot(phone, '04-cong-mo', 2.2)

  // screenshot mode: the QR stops rotating, so the second use of the same code is a replay
  await clickText(phone, 'Ảnh chụp màn hình')
  await sleep(500)
  await clickText(phone, 'Gửi tới cổng')
  await waitText(phone, 'Chiều ra')
  await clickText(phone, 'Gửi tới cổng')
  await waitText(phone, 'QR_ALREADY_USED')
  await scrollTo(phone, 'Thử ngay không cần máy quét')
  await shot(phone, '05-dung-lai-bi-tu-choi', 2.4)

  await clickText(phone, 'QR chứa họ tên')
  await sleep(500)
  await clickText(phone, 'Gửi tới cổng')
  await waitText(phone, 'MALFORMED_TOKEN')
  await scrollTo(phone, 'Thử ngay không cần máy quét')
  await shot(phone, '06-qr-chua-ten-bi-tu-choi', 2.4)

  // back to normal and copy a fresh token for the kiosk
  await clickText(phone, 'Bình thường')
  await sleep(600)
  await clickText(phone, 'Nội dung token đang hiển thị', 'summary')
  await scrollTo(phone, 'Nội dung token đang hiển thị')
  await shot(phone, '07-noi-dung-token', 2)
  await clickText(phone, 'Sao chép token')
  const token = await phone.evaluate(() => window.__copied)
  if (!token) throw new Error('the app did not copy a token')

  // ---------- the kiosk (a laptop at the gate) ----------
  const kiosk = await browser.newPage()
  await kiosk.setViewport({ width: 640, height: 900, deviceScaleFactor: 2 })
  await kiosk.emulateMediaFeatures([{ name: 'prefers-color-scheme', value: 'light' }])
  await kiosk.goto(`${APP}/kiosk`, { waitUntil: 'networkidle0' })
  await clickText(kiosk, 'Không có camera? Dán token', 'summary')
  await kiosk.type('textarea', token, { delay: 0 })
  await clickText(kiosk, 'Gửi như một lượt quét')
  await waitText(kiosk, 'CỔNG MỞ')
  await kiosk.evaluate(() => window.scrollTo(0, document.body.scrollHeight))
  await sleep(200)
  writeFileSync(join(OUT, '08-kiosk.png'), await kiosk.screenshot({ type: 'png', fullPage: true }))

  // ---------- phone pictures inside a device bezel ----------
  const frame = await browser.newPage()
  await frame.setViewport({ width: 470, height: 980, deviceScaleFactor: 2 })
  for (const [name, png] of Object.entries(raw)) {
    await frame.setContent(
      `<body style="margin:0;background:transparent">
         <div id="phone" style="width:390px;margin:20px auto;padding:12px;border-radius:54px;background:#111a1f;
              box-shadow:0 0 0 2px #2d3a42, 0 18px 40px rgba(0,0,0,.28)">
           <div style="border-radius:42px;overflow:hidden;background:#f3f5f4">
             <div style="height:46px;position:relative;font:600 15px system-ui;color:#14211f">
               <span style="position:absolute;left:34px;top:15px">9:41</span>
               <div style="position:absolute;top:9px;left:50%;transform:translateX(-50%);width:112px;height:30px;
                    border-radius:16px;background:#111a1f"></div>
             </div>
             <img src="data:image/png;base64,${png.toString('base64')}" style="width:390px;display:block">
           </div>
         </div>
       </body>`,
      { waitUntil: 'load' },
    )
    const el = await frame.$('#phone')
    writeFileSync(join(OUT, `${name}.png`), await el.screenshot({ type: 'png', omitBackground: true }))
  }
} finally {
  await browser.close()
}

// ---------- animated walkthrough ----------
const gif = GIFEncoder()
for (const { png, hold } of frames) {
  const src = PNG.sync.read(png) // 780 x 1688 (2x); halve it by averaging 2x2 blocks
  const w = src.width >> 1
  const h = src.height >> 1
  const rgba = new Uint8Array(w * h * 4)
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      for (let c = 0; c < 4; c++) {
        const i = ((y * 2) * src.width + x * 2) * 4 + c
        const j = i + src.width * 4
        rgba[(y * w + x) * 4 + c] = (src.data[i] + src.data[i + 4] + src.data[j] + src.data[j + 4]) >> 2
      }
    }
  }
  const palette = quantize(rgba, 128)
  gif.writeFrame(applyPalette(rgba, palette), w, h, { palette, delay: Math.round(hold * 1000) })
}
gif.finish()
writeFileSync(join(OUT, 'employee-app-demo.gif'), Buffer.from(gif.bytes()))

console.log('wrote', Object.keys(raw).length + 1, 'screenshots and employee-app-demo.gif to', OUT)
