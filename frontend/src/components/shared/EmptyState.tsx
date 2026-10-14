import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { cn } from '@/lib/utils'

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: ReactNode
  secondaryAction?: ReactNode
  compact?: boolean
  quote?: number
}

export function EmptyState({ icon: Icon, title, description, action, secondaryAction, compact = false, quote }: EmptyStateProps) {
  return (
    <div
      className={cn(
        'animate-enter relative isolate flex flex-col items-center justify-center gap-4 overflow-hidden rounded-xl border border-dashed bg-surface-2/60 text-center',
        compact ? 'px-6 py-8' : 'min-h-[22rem] px-6 py-12 sm:min-h-[26rem]',
      )}
    >
      {!compact && <div aria-hidden className="hero-glow absolute inset-x-0 top-0 -z-10 h-48 opacity-70" />}
      <div className="relative flex size-12 items-center justify-center rounded-2xl border bg-card text-primary shadow-[var(--elevation)]">
        <Icon aria-hidden className="size-5" />
      </div>
      <div className="space-y-1.5">
        <h2 className="font-heading text-base font-semibold tracking-tight">{title}</h2>
        <p className="max-w-sm text-sm text-pretty text-muted-foreground">
          {description}
        </p>
      </div>
      {(action || secondaryAction) && (
        <div className="flex flex-wrap items-center justify-center gap-2">
          {action}
          {secondaryAction}
        </div>
      )}
      {quote !== undefined && <RotatingQuote rotate={false} offset={quote} className="mt-2 max-w-xs text-left text-xs" />}
    </div>
  )
}
