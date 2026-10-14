import { useEffect, useRef, useState, type PointerEvent } from 'react'
import { CalendarClock, Flame, MousePointerClick } from 'lucide-react'
import { ActionQueueList } from '@/components/dashboard/ActionQueueList'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { InteractiveBoard } from '@/components/landing/InteractiveBoard'
import { useFrameThrottle, useReducedMotion } from '@/hooks/use-motion'
import type { ActionItem } from '@/lib/action-queue'

const SAMPLE_ACTIONS: ActionItem[] = [
  { id: 'sample-1', type: 'upcoming_interview', title: 'Prepare: Kestrel Systems interview', subtitle: 'Technical round, Thursday', href: '/interviews' },
  { id: 'sample-2', type: 'missing_debrief', title: 'Debrief: Brightline assessment', subtitle: 'Write it while it is fresh', href: '/interviews' },
  { id: 'sample-3', type: 'due_task', title: 'Practise sliding-window problems', subtitle: 'Due today, Arrays', href: '/prep' },
]

const PREP_STEPS = [9, 12, 15]
const STEP_MS = 2600
const PREP_TOTAL = 20
const MAX_TILT_DEG = 3

export function ProductPreview() {
  const reduced = useReducedMotion()
  const [step, setStep] = useState(0)
  const tiltRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (reduced) {
      return
    }
    const timer = window.setInterval(() => {
      if (!document.hidden) {
        setStep((current) => (current + 1) % PREP_STEPS.length)
      }
    }, STEP_MS)
    return () => window.clearInterval(timer)
  }, [reduced])

  const tilt = useFrameThrottle(({ target, clientX, clientY }: { target: HTMLElement; clientX: number; clientY: number }) => {
    const element = tiltRef.current
    if (!element) return
    const bounds = target.getBoundingClientRect()
    const x = (clientX - bounds.left) / bounds.width - 0.5
    const y = (clientY - bounds.top) / bounds.height - 0.5
    element.style.transform = `rotateX(${(-y * MAX_TILT_DEG).toFixed(2)}deg) rotateY(${(x * MAX_TILT_DEG).toFixed(2)}deg)`
  })

  function handlePointerMove(event: PointerEvent<HTMLElement>) {
    if (reduced || event.pointerType !== 'mouse') return
    tilt({ target: event.currentTarget, clientX: event.clientX, clientY: event.clientY })
  }

  function resetTilt() {
    if (tiltRef.current) tiltRef.current.style.transform = ''
  }

  const prep = PREP_STEPS[reduced ? PREP_STEPS.length - 1 : step]

  return (
    <figure
      className="relative mx-auto w-full max-w-5xl [perspective:1600px]"
      aria-label="Interactive product preview with sample data"
      onPointerMove={handlePointerMove}
      onPointerLeave={resetTilt}
    >
      <div ref={tiltRef} className="transition-transform duration-300 ease-out [transform-style:preserve-3d]">
        <div className="gradient-border rounded-2xl p-px">
          <div className="surface-raised overflow-hidden rounded-[15px]">
            <div className="flex items-center gap-2 border-b bg-surface-2 px-4 py-2.5">
              <span className="size-2.5 rounded-full bg-border" />
              <span className="size-2.5 rounded-full bg-border" />
              <span className="size-2.5 rounded-full bg-border" />
              <span className="ml-3 truncate font-mono text-[11px] text-muted-foreground">cohort / dashboard</span>
              <span className="ml-auto rounded-md border px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">Sample data</span>
            </div>
            <div className="grid gap-4 p-3 text-left sm:p-5 md:grid-cols-[1.9fr_1fr]">
              <section aria-labelledby="demo-board-title" className="rounded-xl border bg-background/60 p-3 sm:p-4">
                <div className="mb-3 flex items-center justify-between gap-2">
                  <h3 id="demo-board-title" className="text-xs font-medium tracking-wide text-muted-foreground uppercase">
                    Pipeline board
                  </h3>
                  <span className="hidden items-center gap-1 text-[11px] text-primary sm:inline-flex">
                    <MousePointerClick aria-hidden className="size-3.5" />
                    Try it: drag a card
                  </span>
                </div>
                <InteractiveBoard />
              </section>
              <div inert className="grid content-start gap-4">
                <section className="flex items-center gap-4 rounded-xl border bg-background/60 p-4">
                  <ProgressRing value={prep} max={PREP_TOTAL} label="Prep tasks completed" size={84} stroke={8}>
                    <span className="text-lg font-semibold tabular-nums">
                      {prep}
                      <span className="text-xs text-muted-foreground">/{PREP_TOTAL}</span>
                    </span>
                  </ProgressRing>
                  <div className="min-w-0 space-y-1.5">
                    <h3 className="text-sm font-medium">Prep this month</h3>
                    <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                      <Flame className="size-3.5 text-warning" aria-hidden />
                      <span>
                        <span className="tabular-nums">4</span>-day streak
                      </span>
                    </p>
                    <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
                      <CalendarClock className="size-3.5 text-primary" aria-hidden />
                      <span>
                        Next interview in <span className="tabular-nums">2</span> days
                      </span>
                    </p>
                  </div>
                </section>
                <section className="hidden rounded-xl border bg-background/60 p-4 sm:block">
                  <h3 className="mb-2 text-xs font-medium tracking-wide text-muted-foreground uppercase">Next actions</h3>
                  <ActionQueueList items={SAMPLE_ACTIONS} label="Sample next actions" />
                </section>
              </div>
            </div>
          </div>
        </div>
      </div>
      <figcaption className="sr-only">
        A sample dashboard. The pipeline board is interactive: move a card one stage forward with drag and drop or its arrow button.
      </figcaption>
    </figure>
  )
}
