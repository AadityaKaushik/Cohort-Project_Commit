import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import type { ApplicationStatusValue } from '@/lib/applications-api'

/**
 * Status is never conveyed by colour alone (NFR-47) — the label text is
 * always present, colour is a secondary reinforcement only.
 */
const NEUTRAL = 'border-transparent bg-muted text-muted-foreground'
const IN_PROGRESS = 'border-transparent bg-primary/10 text-primary'
const POSITIVE = 'border-transparent bg-success/10 text-success'
const NEGATIVE = 'border-transparent bg-destructive/10 text-destructive'

const STATUS_META: Record<ApplicationStatusValue, { label: string; className: string }> = {
  SAVED: { label: 'Saved', className: NEUTRAL },
  APPLIED: { label: 'Applied', className: IN_PROGRESS },
  OA: { label: 'OA', className: IN_PROGRESS },
  INTERVIEW: { label: 'Interview', className: IN_PROGRESS },
  FINAL_ROUND: { label: 'Final Round', className: IN_PROGRESS },
  OFFER: { label: 'Offer', className: POSITIVE },
  REJECTED: { label: 'Rejected', className: NEGATIVE },
  WITHDRAWN: { label: 'Withdrawn', className: NEUTRAL },
  ACCEPTED: { label: 'Accepted', className: POSITIVE },
  DECLINED: { label: 'Declined', className: NEGATIVE },
}

export function StatusBadge({ status, className }: { status: ApplicationStatusValue; className?: string }) {
  const meta = STATUS_META[status]
  return (
    <Badge variant="outline" className={cn('font-medium', meta.className, className)}>
      {meta.label}
    </Badge>
  )
}
