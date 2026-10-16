import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { Skeleton } from '@/components/ui/skeleton'
import { FlapText } from '@/components/board/FlapText'
import { cn } from '@/lib/utils'

type StatTone = 'primary' | 'info' | 'success' | 'warning'

// A thin top edge per tone instead of a coloured icon tile.
const EDGE_TONES: Record<StatTone, string> = {
  primary: 'border-t-rule',
  info: 'border-t-line-applied',
  success: 'border-t-line-offer',
  warning: 'border-t-signal',
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
    <div className={cn('flex min-w-0 flex-col gap-3 rounded-md border border-t-[3px] bg-card p-4 sm:p-5', EDGE_TONES[tone])}>
      <div className="flex items-start justify-between gap-3">
        <p className="eyebrow">{label}</p>
        {Icon && <Icon aria-hidden className="hidden size-4 shrink-0 text-muted-foreground sm:block" />}
      </div>
      {loading ? (
        <Skeleton className="h-9 w-20" />
      ) : (
        <p className={cn('text-display text-5xl', muted && 'text-muted-foreground')}>
          {value === null ? '-' : <FlapText value={`${value.toFixed(decimals)}${suffix ?? ''}`} />}
        </p>
      )}
      {hint && <div className="text-xs text-pretty text-muted-foreground">{hint}</div>}
    </div>
  )
}

export function StatGrid({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn('grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4', className)}>{children}</div>
}
