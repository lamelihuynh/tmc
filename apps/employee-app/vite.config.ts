/// <reference types="vitest" />
import basicSsl from '@vitejs/plugin-basic-ssl'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// The proxy stands in for the API Gateway (document.md section 15): the browser only ever talks to
// this origin, so a phone on the LAN needs no direct route to the services and there is no CORS.
//
// `npm run dev`      http://localhost:5174          (same machine)
// `npm run dev:lan`  https://<LAN-IP>:5174          (a real phone; self-signed certificate)
// HTTPS is required off localhost: browsers only expose WebCrypto and the camera in a secure context.
export default defineConfig(({ mode }) => ({
  plugins: [react(), ...(mode === 'lan' ? [basicSsl()] : [])],
  server: {
    port: 5174,
    host: mode === 'lan',
    proxy: {
      '/auth': 'http://localhost:8087', // identity
      '/self-service': 'http://localhost:8083', // user-qr: device enrollment
      '/kiosk/v1': 'http://localhost:8081', // gate-station: kiosk API
    },
  },
  test: { environment: 'node' },
}))
