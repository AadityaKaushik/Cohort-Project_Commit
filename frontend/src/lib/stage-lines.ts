import type { ApplicationStatusValue } from '@/lib/applications-api'

export type LineColour = 'saved' | 'applied' | 'oa' | 'interview' | 'final' | 'offer' | 'closed'

/** Each pipeline stage has its own colour, darker the further along it is, used on the dashboard steps, the board and status badges. */
export const STATUS_LINE: Record<ApplicationStatusValue, LineColour> = {
  SAVED: 'saved',
  APPLIED: 'applied',
  OA: 'oa',
  INTERVIEW: 'interview',
  FINAL_ROUND: 'final',
  OFFER: 'offer',
  ACCEPTED: 'offer',
  REJECTED: 'closed',
  WITHDRAWN: 'saved',
  DECLINED: 'saved',
}

// Literal class names so Tailwind keeps them in the build.
export const LINE_BG: Record<LineColour, string> = {
  saved: 'bg-line-saved',
  applied: 'bg-line-applied',
  oa: 'bg-line-oa',
  interview: 'bg-line-interview',
  final: 'bg-line-final',
  offer: 'bg-line-offer',
  closed: 'bg-line-closed',
}

export const LINE_BORDER: Record<LineColour, string> = {
  saved: 'border-line-saved',
  applied: 'border-line-applied',
  oa: 'border-line-oa',
  interview: 'border-line-interview',
  final: 'border-line-final',
  offer: 'border-line-offer',
  closed: 'border-line-closed',
}

export const LINE_VAR: Record<LineColour, string> = {
  saved: 'var(--line-saved)',
  applied: 'var(--line-applied)',
  oa: 'var(--line-oa)',
  interview: 'var(--line-interview)',
  final: 'var(--line-final)',
  offer: 'var(--line-offer)',
  closed: 'var(--line-closed)',
}

export const LINE_BORDER_TOP: Record<LineColour, string> = {
  saved: 'border-t-line-saved',
  applied: 'border-t-line-applied',
  oa: 'border-t-line-oa',
  interview: 'border-t-line-interview',
  final: 'border-t-line-final',
  offer: 'border-t-line-offer',
  closed: 'border-t-line-closed',
}

export const LINE_BORDER_LEFT: Record<LineColour, string> = {
  saved: 'border-l-line-saved',
  applied: 'border-l-line-applied',
  oa: 'border-l-line-oa',
  interview: 'border-l-line-interview',
  final: 'border-l-line-final',
  offer: 'border-l-line-offer',
  closed: 'border-l-line-closed',
}
