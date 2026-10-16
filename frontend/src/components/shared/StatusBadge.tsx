import type { ApplicationStatusValue } from '@/lib/applications-api'
import { ToneBadge, type Tone } from '@/components/shared/ToneBadge'

/**
 * Status is never conveyed by colour alone (NFR-47), so the label text is
 * always present, colour is a secondary reinforcement only.
 */
const STATUS_META: Record<ApplicationStatusValue, { label: string; tone: Tone }> = {
  SAVED: { label: 'Saved', tone: 'neutral' },
  APPLIED: { label: 'Applied', tone: 'primary' },
  OA: { label: 'OA', tone: 'primary' },
  INTERVIEW: { label: 'Interview', tone: 'info' },
  FINAL_ROUND: { label: 'Final Round', tone: 'info' },
  OFFER: { label: 'Offer', tone: 'success' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  WITHDRAWN: { label: 'Withdrawn', tone: 'neutral' },
  ACCEPTED: { label: 'Accepted', tone: 'success' },
  DECLINED: { label: 'Declined', tone: 'warning' },
}

export function StatusBadge({ status, className }: { status: ApplicationStatusValue; className?: string }) {
  const meta = STATUS_META[status]
  return (
    <ToneBadge tone={meta.tone} dot className={className}>
      {meta.label}
    </ToneBadge>
  )
}
