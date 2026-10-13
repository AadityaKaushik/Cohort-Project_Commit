import { useEffect, useState, type ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface ProgressRingProps {
  value: number
  max: number
  label: string
  size?: number
  stroke?: number
  className?: string
  children?: ReactNode
}

/** A single radial meter; the centre always carries the numeric value so the ring is never the only signal. */
export function ProgressRing({ value, max, label, size = 112, stroke = 10, className, children }: ProgressRingProps) {
  const ratio = max <= 0 ? 0 : Math.min(1, Math.max(0, value / max))
  const radius = (size - stroke) / 2
  const circumference = 2 * Math.PI * radius
  const percent = Math.round(ratio * 100)
  const [drawn, setDrawn] = useState(false)

  useEffect(() => {
    const frame = requestAnimationFrame(() => setDrawn(true))
    return () => cancelAnimationFrame(frame)
  }, [])

  return (
    <div
      role="progressbar"
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={percent}
      className={cn('relative inline-flex shrink-0 items-center justify-center', className)}
      style={{ width: size, height: size }}
    >
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="-rotate-90" aria-hidden>
        <circle cx={size / 2} cy={size / 2} r={radius} fill="none" stroke="var(--color-muted)" strokeWidth={stroke} />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="none"
          stroke="var(--color-primary)"
          strokeWidth={stroke}
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - (drawn ? ratio : 0))}
          className="transition-[stroke-dashoffset] duration-1000 ease-out"
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
        {children ?? (
          <span className={cn('font-semibold tracking-tight tabular-nums', size >= 100 ? 'text-2xl' : 'text-base')}>{percent}%</span>
        )}
      </div>
    </div>
  )
}
