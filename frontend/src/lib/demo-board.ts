import type { ApplicationStatusValue } from '@/lib/applications-api'

export const DEMO_COLUMNS: { status: ApplicationStatusValue; label: string }[] = [
  { status: 'APPLIED', label: 'Applied' },
  { status: 'OA', label: 'Assessment' },
  { status: 'INTERVIEW', label: 'Interview' },
  { status: 'OFFER', label: 'Offer' },
]

export interface DemoCard {
  id: string
  company: string
  role: string
  column: number
}

export const INITIAL_DEMO_CARDS: DemoCard[] = [
  { id: 'northwind', company: 'Northwind Labs', role: 'Backend Engineer', column: 0 },
  { id: 'harbor', company: 'Harbor Analytics', role: 'Data Engineer', column: 0 },
  { id: 'brightline', company: 'Brightline', role: 'Platform Engineer', column: 1 },
  { id: 'kestrel', company: 'Kestrel Systems', role: 'Software Engineer', column: 2 },
]

export interface MoveFeedback {
  tone: 'ok' | 'blocked'
  message: string
}

/** Mirrors the real board's State rules in miniature: one stage forward at a time, and backward moves are refused here because they need a note. */
export function moveVerdict(from: number, to: number): MoveFeedback | null {
  if (from === to) return null
  if (to < from) {
    return { tone: 'blocked', message: `Moving back to ${DEMO_COLUMNS[to].label} needs a note in the app, so the demo keeps it here.` }
  }
  if (to > from + 1) {
    return { tone: 'blocked', message: `${DEMO_COLUMNS[from].label} cannot skip straight to ${DEMO_COLUMNS[to].label}. Take it one stage at a time.` }
  }
  return { tone: 'ok', message: `Moved to ${DEMO_COLUMNS[to].label}.` }
}
