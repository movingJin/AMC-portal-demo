import type { Config } from 'tailwindcss'

const config: Config = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          'Pretendard Variable',
          'Pretendard',
          '-apple-system',
          'BlinkMacSystemFont',
          'Segoe UI',
          'Apple SD Gothic Neo',
          'Roboto',
          'Noto Sans KR',
          'sans-serif',
        ],
        mono: ['ui-monospace', 'SFMono-Regular', 'Menlo', 'Monaco', 'Consolas', 'monospace'],
      },
      colors: {
        brand: {
          50: '#eef4ff',
          100: '#dbe7ff',
          200: '#bfd2ff',
          300: '#94b3ff',
          400: '#6488ff',
          500: '#3f60ff',
          600: '#2a44e6',
          700: '#2236bf',
          800: '#1f329a',
          900: '#1d2f7a',
        },
        ink: {
          50: '#f8fafc',
          100: '#f1f5f9',
          200: '#e2e8f0',
          300: '#cbd5e1',
          400: '#94a3b8',
          500: '#64748b',
          600: '#475569',
          700: '#334155',
          800: '#1e293b',
          900: '#0f172a',
        },
      },
      boxShadow: {
        soft: '0 1px 2px 0 rgb(15 23 42 / 0.04), 0 1px 3px 0 rgb(15 23 42 / 0.06)',
        card: '0 1px 2px 0 rgb(15 23 42 / 0.04), 0 4px 16px -4px rgb(15 23 42 / 0.08)',
        ring: '0 0 0 4px rgb(63 96 255 / 0.15)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg, #3f60ff 0%, #2236bf 100%)',
        'hero-radial':
          'radial-gradient(120% 80% at 80% 0%, rgba(63,96,255,0.10), rgba(255,255,255,0) 60%), radial-gradient(80% 60% at 0% 100%, rgba(124,58,237,0.08), rgba(255,255,255,0) 60%)',
      },
      keyframes: {
        'fade-in': {
          '0%': { opacity: '0', transform: 'translateY(4px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
      animation: {
        'fade-in': 'fade-in 200ms ease-out',
      },
    },
  },
  plugins: [],
}

export default config
