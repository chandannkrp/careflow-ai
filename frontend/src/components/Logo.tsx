import { useId } from 'react';

export function LogoMark({ size = 40, className = '' }: { size?: number; className?: string }) {
  const gradientId = useId();
  return (
    <svg
      viewBox="0 0 64 64"
      width={size}
      height={size}
      role="img"
      aria-label="CareFlow AI"
      className={className}
    >
      <defs>
        <linearGradient id={gradientId} x1="0" y1="0" x2="64" y2="64" gradientUnits="userSpaceOnUse">
          <stop offset="0" stopColor="#38bdf8" />
          <stop offset="0.5" stopColor="#6366f1" />
          <stop offset="1" stopColor="#10b981" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="16" fill="#0f172a" />
      <rect x="3" y="3" width="58" height="58" rx="13" fill={`url(#${gradientId})`} />
      <path
        d="M11 34 H22 L27 21 L33 45 L38 30 L42 34 H53"
        fill="none"
        stroke="#ffffff"
        strokeWidth="3.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}
