import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: ReactNode
  compact?: boolean
}

/**
 * The one empty-state shape used everywhere (PROJECT_PLAN.md §20.3):
 * icon, one sentence of explanation, one primary action. Used both for
 * "no data yet" placeholders on unbuilt pages and later for genuine
 * empty states once each feature ships.
 */
export function EmptyState({ icon: Icon, title, description, action, compact = false }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'animate-enter flex flex-col items-center justify-center gap-4 rounded-xl border border-dashed bg-surface-2/50 text-center',
        compact ? 'p-8' : 'min-h-[60vh] p-10',
      )}
    >
      <div className="flex size-12 items-center justify-center rounded-xl border bg-card text-primary shadow-sm">
        <Icon aria-hidden className="size-5" />
      </div>
      <div className="space-y-1.5">
        <h2 className="text-base font-semibold tracking-tight">{title}</h2>
        <p className="max-w-sm text-sm text-pretty text-muted-foreground">{description}</p>
      </div>
      {action}
      {!compact && <RotatingQuote rotate={false} offset={3} className="mt-2 max-w-xs text-left text-xs" />}
    </div>
  )
}
