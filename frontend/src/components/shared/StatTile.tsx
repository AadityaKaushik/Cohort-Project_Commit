import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { CountUp } from '@/components/motion/Reveal'
import { Skeleton } from '@/components/ui/skeleton'
import { cn } from '@/lib/utils'

type StatTone = 'primary' | 'info' | 'success' | 'warning'

const ICON_TONES: Record<StatTone, string> = {
  primary: 'bg-primary/12 text-primary',
  info: 'bg-info/12 text-info',
  success: 'bg-success/12 text-success',
  warning: 'bg-warning-soft text-warning-soft-foreground',
}

interface StatTileProps {
  label: string
  value: number | null
  decimals?: number
  suffix?: string
  hint?: ReactNode
  icon?: LucideIcon
  tone?: StatTone
  loading?: boolean
  muted?: boolean
}

export function StatTile({ label, value, decimals = 0, suffix, hint, icon: Icon, tone = 'primary', loading = false, muted = false }: StatTileProps) {
  return (
    <div className="surface-raised flex min-w-0 flex-col gap-3 rounded-xl border p-4 sm:p-5">
      <div className="flex items-start justify-between gap-3">
        <p className="eyebrow">{label}</p>
        {Icon && (
          <span className={cn('hidden size-8 shrink-0 items-center justify-center rounded-lg sm:flex', ICON_TONES[tone])}>
            <Icon aria-hidden className="size-4" />
          </span>
        )}
      </div>
      {loading ? (
        <Skeleton className="h-9 w-20" />
      ) : (
        <p className={cn('text-3xl font-semibold tracking-tight', muted && 'text-muted-foreground')}>
          {value === null ? '—' : <CountUp value={value} decimals={decimals} suffix={suffix} />}
        </p>
      )}
      {hint && <div className="text-xs text-pretty text-muted-foreground">{hint}</div>}
    </div>
  )
}

export function StatGrid({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('stagger grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4', className)}>{children}</div>
}
