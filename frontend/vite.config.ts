import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue(), vueDevTools(), tailwindcss()],
  define: {
    'import.meta.env.VAPID_PUBLIC_KEY': JSON.stringify(process.env.VAPID_PUBLIC_KEY),
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:12345',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../src/main/resources/static', // Output directory for the build
    emptyOutDir: true, // Clear the output directory before building
  },
})
