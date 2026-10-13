import { CalendarClock, ListChecks, MessagesSquare, NotebookPen, type LucideIcon } from 'lucide-react'
import { StatusBadge } from '@/components/shared/StatusBadge'
import type { ApplicationStatusValue } from '@/lib/applications-api'
import { cn } from '@/lib/utils'

const BOARD: { status: ApplicationStatusValue; cards: [string, string][] }[] = [
  { status: 'APPLIED', cards: [['Harbor Analytics', '3d'], ['Quill & Co', '16d']] },
  { status: 'OA', cards: [['Brightline', '2d']] },
  { status: 'INTERVIEW', cards: [['Kestrel Systems', '1d'], ['Northwind Labs', '4d']] },
  { status: 'OFFER', cards: [['Atlas Health', 'new']] },
]

export function MiniBoard() {
  return (
    <div className="grid grid-cols-2 gap-2 rounded-xl border bg-background/60 p-3 sm:grid-cols-4">
      {BOARD.map((column) => (
        <div key={column.status} className="space-y-2">
          <StatusBadge status={column.status} />
          {column.cards.map(([company, age]) => (
            <div key={company} className="flex items-center justify-between gap-2 rounded-lg border bg-card px-2.5 py-2">
              <span className="truncate text-xs font-medium">{company}</span>
              <span
                className={cn(
                  'shrink-0 rounded px-1 font-mono text-[10px]',
                  age === '16d' ? 'bg-warning-soft text-warning-soft-foreground' : 'text-muted-foreground',
                )}
              >
                {age}
              </span>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}

const MASTERY: [string, number][] = [
  ['Arrays', 72],
  ['Sliding Window', 48],
  ['System Design Basics', 31],
]

export function MasteryBars() {
  return (
    <div className="space-y-3 rounded-xl border bg-background/60 p-3">
      {MASTERY.map(([topic, value]) => (
        <div key={topic} className="space-y-1.5 text-xs">
          <div className="flex justify-between gap-2">
            <span className="truncate font-medium">{topic}</span>
            <span className="tabular-nums text-muted-foreground">{value}%</span>
          </div>
          <div className="h-1.5 rounded-full bg-muted">
            <div className="animate-grow-x h-full origin-left rounded-full bg-primary" style={{ width: `${value}%` }} />
          </div>
        </div>
      ))}
    </div>
  )
}

const LOOP_STEPS: { icon: LucideIcon; label: string }[] = [
  { icon: CalendarClock, label: 'Round scheduled' },
  { icon: MessagesSquare, label: 'Debrief written' },
  { icon: NotebookPen, label: 'Prep tasks created' },
]

export function InterviewLoop() {
  return (
    <ol className="space-y-2">
      {LOOP_STEPS.map(({ icon: Icon, label }, index) => (
        <li
          key={label}
          className="animate-step-glow flex items-center gap-3 rounded-lg border bg-background/60 px-3 py-2 text-xs"
          style={{ animationDelay: `${index * 2}s` }}
        >
          <span className="font-mono text-muted-foreground tabular-nums">0{index + 1}</span>
          <Icon className="size-3.5 text-primary" />
          <span className="font-medium">{label}</span>
        </li>
      ))}
    </ol>
  )
}

const FUNNEL: [string, number][] = [
  ['Applied', 24],
  ['Assessment', 14],
  ['Interview', 8],
  ['Final', 4],
  ['Offer', 2],
]

export function MiniFunnel() {
  return (
    <div className="space-y-2 rounded-xl border bg-background/60 p-3">
      {FUNNEL.map(([stage, count], index) => (
        <div key={stage} className="grid grid-cols-[4.75rem_1fr_1.5rem] items-center gap-2 text-xs">
          <span className="truncate text-muted-foreground">{stage}</span>
          <div className="h-2 rounded-full bg-muted">
            <div
              className={cn('animate-grow-x h-full origin-left rounded-full', index === FUNNEL.length - 1 ? 'bg-success' : 'bg-chart-1')}
              style={{ width: `${(count / FUNNEL[0][1]) * 100}%` }}
            />
          </div>
          <span className="text-right font-medium tabular-nums">{count}</span>
        </div>
      ))}
    </div>
  )
}

export function CoachCard() {
  return (
    <div className="space-y-3 rounded-xl border bg-background/60 p-3 text-xs">
      <div className="flex items-start gap-2">
        <ListChecks className="mt-0.5 size-3.5 shrink-0 text-primary" />
        <p className="font-medium">Review graphs before your Kestrel technical round</p>
      </div>
      <div className="rounded-lg border bg-card p-2.5 text-muted-foreground">
        <p className="mb-1 font-medium text-foreground">Why this</p>
        Two past debriefs flagged graph questions, and your graphs mastery is below 40%.
      </div>
    </div>
  )
}

const COMPARISONS: { label: string; you: number; cohort: number }[] = [
  { label: 'Days from applied to assessment', you: 9, cohort: 12 },
  { label: 'Days from assessment to interview', you: 14, cohort: 10 },
]

export function CohortCompare() {
  const max = 16
  return (
    <div className="grid gap-4 rounded-xl border bg-background/60 p-4 sm:grid-cols-2 sm:gap-6">
      {COMPARISONS.map((row) => (
        <div key={row.label} className="space-y-2.5">
          <p className="text-xs font-medium">{row.label}</p>
          {(
            [
              ['You', row.you, 'bg-primary'],
              ['Cohort median', row.cohort, 'bg-chart-2'],
            ] as const
          ).map(([who, days, color]) => (
            <div key={who} className="grid grid-cols-[5.5rem_1fr_2rem] items-center gap-2 text-xs">
              <span className="text-muted-foreground">{who}</span>
              <div className="h-2 rounded-full bg-muted">
                <div className={cn('animate-grow-x h-full origin-left rounded-full', color)} style={{ width: `${(days / max) * 100}%` }} />
              </div>
              <span className="text-right tabular-nums">{days}d</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
