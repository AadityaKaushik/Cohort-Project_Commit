import { useEffect, useState } from 'react'
import { prefersReducedMotion } from '@/lib/motion'
import { LINE_BG, type LineColour } from '@/lib/stage-lines'
import { cn } from '@/lib/utils'

const STEPS: { label: string; line: LineColour; event: string }[] = [
  { label: 'Saved', line: 'saved', event: 'Saved from the job page with the bookmarklet.' },
  { label: 'Applied', line: 'applied', event: 'Applied. Cohort starts counting days in this stage.' },
  { label: 'OA', line: 'oa', event: 'Online test on Friday. Two prep tasks added for arrays and graphs.' },
  { label: 'Interview', line: 'interview', event: 'Round 1 on Tuesday at 10:30. A one-page prep pack is ready.' },
  { label: 'Final', line: 'final', event: 'Debrief done in 3 minutes. Two questions saved to your bank.' },
  { label: 'Offer', line: 'offer', event: 'Offer in. Compare it with the others before the deadline.' },
]

const HOLD_MS = 2200

/** An example application climbing the pipeline, one stage at a time, with what Cohort does at each one. */
export function ClimbDemo() {
  const [stage, setStage] = useState(() => (prefersReducedMotion() ? STEPS.length - 1 : 0))
  const [paused, setPaused] = useState(false)

  useEffect(() => {
    if (prefersReducedMotion() || paused) return
    const timer = window.setTimeout(() => setStage((current) => (current + 1) % STEPS.length), stage === STEPS.length - 1 ? HOLD_MS * 1.6 : HOLD_MS)
    return () => window.clearTimeout(timer)
  }, [stage, paused])

  return (
    <figure
      aria-label="Example: one application climbing from saved to offer"
      className="rounded-md bg-background p-4 text-foreground sm:p-6"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
    >
      <div className="flex items-center justify-between gap-3 border-b pb-3">
        <div className="min-w-0">
          <p className="truncate font-semibold">Kestrel Labs</p>
          <p className="truncate text-xs text-muted-foreground">Backend Engineer Intern</p>
        </div>
        <span className="board-label shrink-0 rounded-sm bg-signal px-2 py-1 text-signal-foreground">{STEPS[stage].label}</span>
      </div>

      <div aria-hidden className="relative mt-6 grid h-40 grid-cols-6 items-end gap-[3px]">
        {STEPS.map((step, index) => (
          <div key={step.label} className="relative flex h-full flex-col justify-end">
            <div
              className={cn(
                'origin-bottom rounded-t-[2px] transition-[opacity,transform] duration-500 motion-safe:animate-[step-rise_640ms_cubic-bezier(0.22,1,0.36,1)_both]',
                LINE_BG[step.line],
                index > stage && 'opacity-25',
              )}
              style={{ height: `${22 + index * 15}%`, animationDelay: `${index * 80}ms` }}
            />
            {index === stage && (
              <span
                key={stage}
                className="absolute left-1/2 size-4 -translate-x-1/2 rounded-sm border-2 border-background bg-foreground shadow-[0_0_0_3px_var(--signal)] motion-safe:animate-[climb-hop_420ms_cubic-bezier(0.34,1.56,0.64,1)_both]"
                style={{ bottom: `calc(${22 + index * 15}% + 6px)` }}
              />
            )}
          </div>
        ))}
      </div>
      <div aria-hidden className="mt-2 grid grid-cols-6 gap-[3px]">
        {STEPS.map((step, index) => (
          <span key={step.label} className={cn('board-label truncate text-[10px]', index === stage ? 'text-foreground' : 'text-muted-foreground')}>
            {step.label}
          </span>
        ))}
      </div>

      <figcaption className="mt-4 flex min-h-12 items-start gap-3 border-t pt-3 text-sm">
        <span className="tabular shrink-0 text-muted-foreground">{String(stage + 1).padStart(2, '0')}/06</span>
        <span key={stage} className="motion-safe:animate-[rise-in_360ms_ease-out_both]">
          {STEPS[stage].event}
        </span>
      </figcaption>
    </figure>
  )
}
