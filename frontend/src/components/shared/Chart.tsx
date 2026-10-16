import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface ChartFrameProps {
  height: number
  testId?: string
  className?: string
  label: string
  children: ReactNode
}

export function ChartFrame({ height, testId, className, label, children }: ChartFrameProps) {
  return (
    <div role="img" aria-label={label} data-testid={testId} className={cn('w-full', className)} style={{ height }}>
      {children}
    </div>
  )
}

export interface LegendItem {
  label: string
  color: string
}

export function ChartLegend({ items }: { items: LegendItem[] }) {
  return (
    <ul aria-label="Legend" className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-muted-foreground">
      {items.map((item) => (
        <li key={item.label} className="inline-flex items-center gap-1.5">
          <span aria-hidden className="h-0.5 w-3.5 rounded-full" style={{ background: item.color }} />
          {item.label}
        </li>
      ))}
    </ul>
  )
}

export function ChartEmpty({ icon, message, height }: { icon: ReactNode; message: string; height: number }) {
  return (
    <div
      className="flex flex-col items-center justify-center gap-2 rounded-lg border border-dashed bg-surface-2/50 px-6 text-center text-sm text-muted-foreground"
      style={{ minHeight: height }}
    >
      <span className="flex size-9 items-center justify-center rounded-lg bg-card text-primary ring-1 ring-border">{icon}</span>
      <p className="max-w-sm text-pretty">{message}</p>
    </div>
  )
}
