import type { ReactNode } from 'react'
import { CheckIcon } from 'lucide-react'
import { useInView } from '@/lib/use-in-view'
import { cn } from '@/lib/utils'

interface Problem {
  number: string
  problem: string
  detail: string
  fix: string
  demo: (play: boolean) => ReactNode
}

const PROBLEMS: Problem[] = [
  {
    number: '01',
    problem: 'Applications go quiet and nobody notices',
    detail: 'At 40 or 80 applications a spreadsheet stops being read. The ones that stalled are found weeks too late to follow up.',
    fix: 'Cohort learns how long you usually wait at each stage and flags anything quiet for longer, with the follow-up one click away.',
    demo: (play) => <StallDemo play={play} />,
  },
  {
    number: '02',
    problem: 'What you were asked is forgotten by Thursday',
    detail: 'The most useful part of an interview is the questions. Without notes, they are gone within a day or two.',
    fix: 'A three-minute debrief after each round records the outcome and the questions, and any question goes straight into your bank.',
    demo: (play) => <DebriefDemo play={play} />,
  },
  {
    number: '03',
    problem: 'Practice follows a list, not your gaps',
    detail: 'Generic question lists treat every topic the same, including the ones you already know.',
    fix: 'Each topic has a mastery score that fades without practice. The review queue and mock interview pull from your weakest topics.',
    demo: (play) => <MasteryDemo play={play} />,
  },
]

export function ProblemShowcase() {
  return (
    <ol className="space-y-6">
      {PROBLEMS.map((item) => (
        <ProblemRow key={item.number} item={item} />
      ))}
    </ol>
  )
}

function ProblemRow({ item }: { item: Problem }) {
  const { ref, inView } = useInView<HTMLLIElement>(0.35)
  return (
    <li ref={ref} className="grid gap-6 rounded-md border bg-card p-5 sm:p-7 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)] lg:items-center lg:gap-10">
      <div className="space-y-4">
        <p className="tabular text-sm text-muted-foreground">{item.number}</p>
        <h3 className="text-display text-3xl sm:text-4xl">{item.problem}</h3>
        <p className="text-sm/relaxed text-muted-foreground">{item.detail}</p>
        <p className="flex gap-3 border-l-[3px] border-signal pl-3 text-sm/relaxed font-medium">{item.fix}</p>
      </div>
      <div aria-hidden className="rounded-md border bg-background p-4">
        {item.demo(inView)}
      </div>
    </li>
  )
}

function appear(play: boolean, delayMs: number) {
  return {
    className: cn('transition-[opacity,translate] duration-500 ease-out', play ? 'translate-y-0 opacity-100' : 'translate-y-2 opacity-0'),
    style: { transitionDelay: play ? `${delayMs}ms` : '0ms' },
  }
}

function StallDemo({ play }: { play: boolean }) {
  const rows = [
    { company: 'Northwind', stage: 'Applied', days: 4 },
    { company: 'Arcadia Bank', stage: 'OA', days: 18, stalled: true },
    { company: 'Kestrel Labs', stage: 'Interview', days: 2 },
  ]
  return (
    <div className="space-y-2">
      <p className="board-label text-muted-foreground">Applications</p>
      {rows.map((row) => (
        <div
          key={row.company}
          className={cn(
            'flex items-center justify-between gap-3 rounded-sm border px-3 py-2 text-sm transition-colors duration-500',
            row.stalled && play && 'border-destructive/50 bg-destructive/5',
          )}
          style={{ transitionDelay: row.stalled && play ? '700ms' : '0ms' }}
        >
          <span className="min-w-0">
            <span className="block truncate font-semibold">{row.company}</span>
            <span className="block text-xs text-muted-foreground">
              {row.stage} · {row.days} days
            </span>
          </span>
          {row.stalled && (
            <span {...appear(play, 900)}>
              <span className="board-label inline-block rounded-sm bg-destructive px-1.5 py-1 text-[10px] text-white dark:text-black">Stalled</span>
            </span>
          )}
        </div>
      ))}
      <p {...appear(play, 1400)}>
        <span className="flex items-center justify-between gap-3 rounded-sm bg-accent px-3 py-2 text-xs">
          <span>Quiet for 18 days. You usually hear back in 9.</span>
          <span className="shrink-0 font-semibold underline decoration-signal decoration-2 underline-offset-4">Follow up</span>
        </span>
      </p>
    </div>
  )
}

function DebriefDemo({ play }: { play: boolean }) {
  const chips = ['Graphs', 'System design', 'Tell me about a conflict']
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="board-label text-muted-foreground">Debrief · Round 2</p>
        <span className="tabular text-xs text-muted-foreground">2:41</span>
      </div>
      <div className="flex gap-2">
        {['Passed', 'Waiting', 'Not selected'].map((outcome, index) => (
          <span
            key={outcome}
            className={cn('rounded-sm border px-2 py-1 text-xs transition-colors duration-300', index === 0 && play && 'border-foreground bg-foreground text-background')}
            style={{ transitionDelay: play ? '300ms' : '0ms' }}
          >
            {outcome}
          </span>
        ))}
      </div>
      <p className="text-xs text-muted-foreground">Asked about</p>
      <div className="flex flex-wrap gap-2">
        {chips.map((chip, index) => (
          <span key={chip} {...appear(play, 600 + index * 250)}>
            <span className="inline-block rounded-sm border border-foreground/30 px-2 py-1 text-xs">{chip}</span>
          </span>
        ))}
      </div>
      <p {...appear(play, 1600)}>
        <span className="flex items-center gap-2 rounded-sm bg-accent px-3 py-2 text-xs">
          <CheckIcon className="size-3.5 shrink-0" />
          "Shortest path in a weighted graph" saved to your question bank
        </span>
      </p>
    </div>
  )
}

function MasteryDemo({ play }: { play: boolean }) {
  const topics = [
    { name: 'Arrays', from: 3, to: 3 },
    { name: 'Graphs', from: 1, to: 2 },
    { name: 'Dynamic programming', from: 1, to: 1 },
  ]
  return (
    <div className="space-y-3">
      <p className="board-label text-muted-foreground">Readiness</p>
      {topics.map((topic, row) => {
        const level = play ? topic.to : topic.from
        return (
          <div key={topic.name} className="flex items-center justify-between gap-3 border-b pb-2 text-sm last:border-b-0">
            <span className="truncate">{topic.name}</span>
            <span className="flex h-5 items-end gap-[3px]">
              {[1, 2, 3].map((step) => (
                <span
                  key={step}
                  className={cn(
                    'w-2 rounded-[1px] transition-colors duration-500',
                    step > level ? 'bg-foreground/15' : level === 3 ? 'bg-line-offer' : 'bg-foreground',
                  )}
                  style={{ height: `${step * 33.4}%`, transitionDelay: play ? `${700 + row * 200}ms` : '0ms' }}
                />
              ))}
            </span>
          </div>
        )
      })}
      <p {...appear(play, 1500)}>
        <span className="block rounded-sm bg-accent px-3 py-2 text-xs">Review today: Dynamic programming. Last practised 12 days ago and fading.</span>
      </p>
    </div>
  )
}
