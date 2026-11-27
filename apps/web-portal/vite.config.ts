/// <reference types="vitest" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// During development the portal talks to the services through this proxy, which stands in for the API
// Gateway of document.md section 15. Requests carry the identity JWT (D-13) or, on the local profile only,
// the development headers.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth/v1': 'http://localhost:8087',
      '/security/v1': 'http://localhost:8081',
      '/kiosk/v1': 'http://localhost:8081',
      '/supervision/v1': 'http://localhost:8084',
      '/admin/v1/hr-sync': 'http://localhost:8085',
      '/admin/v1/audit': 'http://localhost:8086',
    },
  },
  test: { environment: 'node' },
})
