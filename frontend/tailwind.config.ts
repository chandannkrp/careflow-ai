import type { Config } from 'tailwindcss';

export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        care: {
          critical: '#b42318',
          high: '#b54708',
          medium: '#175cd3',
          low: '#027a48',
        },
      },
    },
  },
  plugins: [],
} satisfies Config;
