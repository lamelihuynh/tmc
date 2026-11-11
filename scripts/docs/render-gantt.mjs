// Renders the Mermaid Gantt chart in docs/handover/03-gantt.md to docs/handover/gantt.png, for
// viewers that do not draw Mermaid (plain IntelliJ, PDF export). It also fails if the chart has a
// syntax error. Needs network access to load Mermaid from jsDelivr.
//
//   cd scripts/docs && node render-gantt.mjs
import { existsSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import puppeteer from 'puppeteer-core'

const HERE = dirname(fileURLToPath(import.meta.url))
const DIR = join(HERE, '..', '..', 'docs', 'handover')
const source = readFileSync(join(DIR, '03-gantt.md'), 'utf8').match(/```mermaid\n([\s\S]*?)```/)?.[1]
if (!source) throw new Error('no mermaid block in 03-gantt.md')

const chrome =
  process.env.CHROME_PATH ??
  [
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
  ].find(existsSync)
if (!chrome) throw new Error('Chrome not found; set CHROME_PATH')

const browser = await puppeteer.launch({ executablePath: chrome, headless: true })
try {
  const page = await browser.newPage()
  await page.setViewport({ width: 1500, height: 1200, deviceScaleFactor: 2 })
  await page.setContent(
    `<body style="margin:0;background:#fff;font-family:system-ui"><div id="out" style="padding:16px;width:1460px"></div>
     <script src="https://cdn.jsdelivr.net/npm/mermaid@10.9.1/dist/mermaid.min.js"></script></body>`,
    { waitUntil: 'networkidle0' },
  )
  const error = await page.evaluate(async (src) => {
    try {
      mermaid.initialize({ startOnLoad: false, theme: 'default', gantt: { barHeight: 22, barGap: 5, fontSize: 13, sectionFontSize: 14, leftPadding: 250, useWidth: 1440 } })
      const { svg } = await mermaid.render('gantt', src)
      document.getElementById('out').innerHTML = svg
      return null
    } catch (e) {
      return String(e?.message ?? e)
    }
  }, source)
  if (error) throw new Error('Mermaid rejected the chart: ' + error)
  const el = await page.$('#out')
  writeFileSync(join(DIR, 'gantt.png'), await el.screenshot({ type: 'png' }))
  console.log('wrote', join(DIR, 'gantt.png'))
} finally {
  await browser.close()
}
