import type { ApplicationStatusValue } from '@/lib/applications-api'
import { LINE_BG, STATUS_LINE } from '@/lib/stage-lines'
import { cn } from '@/lib/utils'

/** The label is always shown; the line colour only backs it up (NFR-47). */
const STATUS_LABEL: Record<ApplicationStatusValue, string> = {
  SAVED: 'Saved',
  APPLIED: 'Applied',
  OA: 'OA',
  INTERVIEW: 'Interview',
  FINAL_ROUND: 'Final Round',
  OFFER: 'Offer',
  REJECTED: 'Rejected',
  WITHDRAWN: 'Withdrawn',
  ACCEPTED: 'Accepted',
  DECLINED: 'Declined',
}

export function StatusBadge({ status, className }: { status: ApplicationStatusValue; className?: string }) {
  return (
    <span
      className={cn(
        'board-label inline-flex h-5 w-fit shrink-0 items-center gap-1.5 rounded-[3px] border border-border bg-card px-1.5 text-[10.5px] whitespace-nowrap text-foreground',
        className,
      )}
    >
      <span aria-hidden className={cn('size-2 rounded-[1px]', LINE_BG[STATUS_LINE[status]])} />
      {STATUS_LABEL[status]}
    </span>
  )
}
