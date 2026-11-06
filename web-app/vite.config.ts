import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: process.env.VITE_BACKEND_URL || 'https://140.238.240.128:8443',
        changeOrigin: true,
        secure: false, // Allow self-signed certificate in development
      },
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
