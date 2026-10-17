import { useState, type KeyboardEvent } from 'react'
import { LINE_BG, type LineColour } from '@/lib/stage-lines'
import { cn } from '@/lib/utils'

const STAGES: { key: string; label: string; line: LineColour; title: string; points: string[] }[] = [
  {
    key: 'saved',
    label: 'Saved',
    line: 'saved',
    title: 'Get every role in, without retyping',
    points: [
      'Save any job page with the bookmarklet: title, company, location and link are filled in.',
      'Import an existing spreadsheet or LinkedIn\'s own applications export as CSV.',
      'Paste a job posting and the empty fields fill themselves.',
    ],
  },
  {
    key: 'applied',
    label: 'Applied',
    line: 'applied',
    title: 'Know which ones went quiet',
    points: [
      'Every status change is kept, so each application has a timeline.',
      'Stalls are measured against your own usual wait, not a made-up number.',
      'Contacts and referrals sit next to the company, with a nudge when one goes cold.',
    ],
  },
  {
    key: 'oa',
    label: 'OA',
    line: 'oa',
    title: 'Turn a test date into a plan',
    points: [
      'Prep tasks per topic, with due dates.',
      'A question bank of 288 original problems across 48 topics.',
      'A readiness score per job, from the topics named in the posting.',
    ],
  },
  {
    key: 'interview',
    label: 'Interview',
    line: 'interview',
    title: 'Walk in prepared, walk out with notes',
    points: [
      'A one-page prep pack for each upcoming round, from your own data.',
      'A three-minute debrief after the round. Questions asked go into your bank.',
      'A timed mock interview from your weakest topics, and STAR stories for behavioural rounds.',
    ],
  },
  {
    key: 'final',
    label: 'Final',
    line: 'final',
    title: 'See the whole picture',
    points: [
      'Funnel, conversion and rejection points, each showing how many applications it is based on.',
      'Recommendations that say why they were made and how strong the evidence is.',
      'Follow-up email drafts you edit and send yourself.',
    ],
  },
  {
    key: 'offer',
    label: 'Offer',
    line: 'offer',
    title: 'Decide with the numbers side by side',
    points: [
      'Compare offers on base pay, bonus, equity, location and work mode.',
      'Deadline countdowns so nothing lapses.',
      'Negotiation email drafts, never sent without you.',
    ],
  },
]

/** The pipeline as clickable steps; each one shows what Cohort does at that stage. */
export function StageExplorer() {
  const [selected, setSelected] = useState(0)
  const stage = STAGES[selected]

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    const delta = event.key === 'ArrowRight' ? 1 : event.key === 'ArrowLeft' ? -1 : 0
    if (!delta) return
    event.preventDefault()
    const next = (selected + delta + STAGES.length) % STAGES.length
    setSelected(next)
    document.getElementById(`stage-tab-${STAGES[next].key}`)?.focus()
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] lg:items-end">
      <div role="tablist" aria-label="Pipeline stages" className="grid grid-cols-6 items-end gap-1 sm:gap-1.5">
        {STAGES.map((item, index) => {
          const active = index === selected
          return (
            <button
              key={item.key}
              id={`stage-tab-${item.key}`}
              type="button"
              role="tab"
              aria-selected={active}
              aria-controls="stage-panel"
              tabIndex={active ? 0 : -1}
              onClick={() => setSelected(index)}
              onKeyDown={handleKeyDown}
              className="group flex w-full min-w-0 flex-col gap-2 text-left focus-visible:outline-2 focus-visible:outline-offset-4"
            >
              <span
                className={cn(
                  'block w-full origin-bottom rounded-t-[2px] transition-[transform,opacity,box-shadow] duration-300 group-hover:-translate-y-1 motion-safe:animate-[step-rise_560ms_cubic-bezier(0.22,1,0.36,1)_both]',
                  LINE_BG[item.line],
                  active ? 'shadow-[0_-6px_0_0_var(--signal)]' : 'opacity-60 group-hover:opacity-100',
                )}
                style={{ height: `${2.5 + index * 1.6}rem`, animationDelay: `${index * 90}ms` }}
              />
              <span className={cn('board-label block w-full truncate border-t-2 pt-2 text-[10px] sm:text-[11px]', active ? 'border-signal text-foreground' : 'border-rule/30 text-muted-foreground')}>
                {item.label}
              </span>
            </button>
          )
        })}
      </div>

      <div id="stage-panel" role="tabpanel" aria-labelledby={`stage-tab-${stage.key}`} className="min-h-64 rounded-md border bg-card p-5 sm:p-6">
        <div key={stage.key} className="motion-safe:animate-[rise-in_320ms_ease-out_both]">
          <p className="board-label text-muted-foreground">
            Stage {selected + 1} of {STAGES.length} · {stage.label}
          </p>
          <h3 className="text-display mt-2 text-3xl">{stage.title}</h3>
          <ul className="mt-4 space-y-3">
            {stage.points.map((point) => (
              <li key={point} className="flex gap-3 text-sm/relaxed">
                <span aria-hidden className={cn('mt-1.5 size-2.5 shrink-0 rounded-[1px]', LINE_BG[stage.line])} />
                {point}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  )
}
