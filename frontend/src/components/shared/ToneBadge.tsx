import type { ComponentProps, ReactNode } from 'react'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

export type Tone = 'neutral' | 'primary' | 'info' | 'success' | 'warning' | 'danger'

const TONE_CLASSES: Record<Tone, string> = {
  neutral: 'bg-muted text-muted-foreground ring-border',
  primary: 'bg-primary/10 text-primary ring-primary/20',
  info: 'bg-info/10 text-info ring-info/20',
  success: 'bg-success/10 text-success ring-success/25',
  warning: 'bg-warning-soft text-warning-soft-foreground ring-warning/25',
  danger: 'bg-destructive/10 text-destructive ring-destructive/20',
}

interface ToneBadgeProps extends Omit<ComponentProps<typeof Badge>, 'variant'> {
  tone: Tone
  dot?: boolean
  children: ReactNode
}

export function ToneBadge({ tone, dot = false, className, children, ...props }: ToneBadgeProps) {
  return (
    <Badge variant="outline" className={cn('border-transparent font-medium ring-1 ring-inset', TONE_CLASSES[tone], className)} {...props}>
      {dot ? (
        <>
          <span aria-hidden className="size-1.5 rounded-full bg-current" />
          {children}
        </>
      ) : (
        children
      )}
    </Badge>
  )
}

const PRIORITY_TONES: Record<string, { label: string; tone: Tone }> = {
  low: { label: 'Low', tone: 'neutral' },
  medium: { label: 'Medium', tone: 'info' },
  high: { label: 'High', tone: 'warning' },
}

export function PriorityBadge({ priority }: { priority: string | null }) {
  if (!priority) {
    return <span className="text-muted-foreground">—</span>
  }
  const meta = PRIORITY_TONES[priority] ?? { label: priority, tone: 'neutral' as Tone }
  return (
    <ToneBadge tone={meta.tone} dot>
      {meta.label}
    </ToneBadge>
  )
}

const OUTCOME_TONES: Record<string, { label: string; tone: Tone }> = {
  passed: { label: 'Passed', tone: 'success' },
  failed: { label: 'Not selected', tone: 'danger' },
  pending: { label: 'Pending', tone: 'warning' },
  unknown: { label: 'Unknown', tone: 'neutral' },
}

export function OutcomeBadge({ outcome }: { outcome: string }) {
  const meta = OUTCOME_TONES[outcome] ?? { label: outcome, tone: 'neutral' as Tone }
  return <ToneBadge tone={meta.tone}>{meta.label}</ToneBadge>
}
