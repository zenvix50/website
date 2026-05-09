export default {
  darkMode: 'class',
  content: ['./src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        bg: {
          primary: 'rgb(255 255 255)',
          surface: 'rgb(249 249 249)',
          card: 'rgb(241 241 245)',
        },
        border: 'rgb(229 229 235)',
        accent: {
          DEFAULT: '#e8342a',
          hover: '#ff3d33',
        },
        text: {
          primary: 'rgb(25 25 35)',
          secondary: 'rgb(120 120 140)',
          muted: 'rgb(160 160 175)',
        },
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', 'monospace'],
        display: ['Syne', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
      },
      animation: {
        'pulse-glow': 'pulseGlow 2s ease-in-out infinite',
        'float': 'float 6s ease-in-out infinite',
        'scan-line': 'scanLine 3s linear infinite',
        'blink': 'blink 1s step-end infinite',
      },
      keyframes: {
        pulseGlow: {
          '0%, 100%': { boxShadow: '0 0 10px #e8342a40' },
          '50%': { boxShadow: '0 0 25px #e8342a80, 0 0 50px #e8342a30' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-12px)' },
        },
        scanLine: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100%)' },
        },
        blink: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '0' },
        },
      },
    },
  },
  plugins: [],
};

