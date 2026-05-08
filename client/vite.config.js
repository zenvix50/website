import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:5000',
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('react-syntax-highlighter')) return 'syntax-highlighter';
          if (id.includes('framer-motion')) return 'framer-motion';
          if (id.includes('node_modules/react') || id.includes('react-router-dom')) return 'vendor';
        },
      },
    },
  },
});
