/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#eef2ff',
          100: '#e0e7ff',
          200: '#c7d2fe',
          300: '#a5b4fc',
          400: '#818cf8',
          500: '#6366f1',
          600: '#4f46e5',
          700: '#4338ca',
          800: '#3730a3',
          900: '#312e81',
        },
        surface: {
          DEFAULT: '#0f0f1a',
          50:  '#1a1a2e',
          100: '#16213e',
          200: '#0f3460',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'gradient-mesh': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      },
      animation: {
        'fade-in':    'fadeIn 0.3s ease-out',
        'slide-up':   'slideUp 0.4s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'bounce-in':  'bounceIn 0.5s ease-out',
      },
      keyframes: {
        fadeIn:   { from: { opacity: 0 },              to: { opacity: 1 } },
        slideUp:  { from: { opacity: 0, transform: 'translateY(20px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        bounceIn: { '0%': { transform: 'scale(0.8)', opacity: 0 }, '60%': { transform: 'scale(1.05)' }, '100%': { transform: 'scale(1)', opacity: 1 } },
      },
      backdropBlur: { xs: '2px' },
    },
  },
  plugins: [],
}
