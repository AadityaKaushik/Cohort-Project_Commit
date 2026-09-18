import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'
import type { ApplicationStatusValue } from '@/lib/applications-api'

/**
 * Status is never conveyed by colour alone (NFR-47) — the label text is
 * always present, colour is a secondary reinforcement only.
 */
const STATUS_META: Record<ApplicationStatusValue, { label: string; className: string }> = {
  SAVED: { label: 'Saved', className: 'bg-muted text-muted-foreground border-transparent' },
  APPLIED: {
    label: 'Applied',
    className: 'bg-blue-100 text-blue-800 border-transparent dark:bg-blue-950 dark:text-blue-300',
  },
  OA: {
    label: 'OA',
    className: 'bg-violet-100 text-violet-800 border-transparent dark:bg-violet-950 dark:text-violet-300',
  },
  INTERVIEW: {
    label: 'Interview',
    className: 'bg-amber-100 text-amber-800 border-transparent dark:bg-amber-950 dark:text-amber-300',
  },
  FINAL_ROUND: {
    label: 'Final Round',
    className: 'bg-orange-100 text-orange-800 border-transparent dark:bg-orange-950 dark:text-orange-300',
  },
  OFFER: {
    label: 'Offer',
    className: 'bg-emerald-100 text-emerald-800 border-transparent dark:bg-emerald-950 dark:text-emerald-300',
  },
  REJECTED: {
    label: 'Rejected',
    className: 'bg-red-100 text-red-800 border-transparent dark:bg-red-950 dark:text-red-300',
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    className: 'bg-slate-200 text-slate-700 border-transparent dark:bg-slate-800 dark:text-slate-300',
  },
  ACCEPTED: {
    label: 'Accepted',
    className: 'bg-green-100 text-green-800 border-transparent dark:bg-green-950 dark:text-green-300',
  },
  DECLINED: {
    label: 'Declined',
    className: 'bg-rose-100 text-rose-800 border-transparent dark:bg-rose-950 dark:text-rose-300',
  },
}

export function StatusBadge({ status, className }: { status: ApplicationStatusValue; className?: string }) {
  const meta = STATUS_META[status]
  return (
    <Badge variant="outline" className={cn('font-medium', meta.className, className)}>
      {meta.label}
    </Badge>
  )
}
