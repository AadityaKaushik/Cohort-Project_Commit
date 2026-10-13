import type { ReactNode } from 'react'
import { BarChart3, Briefcase, MessagesSquare, PencilLine, type LucideIcon } from 'lucide-react'
import { Reveal } from '@/components/motion/Reveal'
import { StatusBadge } from '@/components/shared/StatusBadge'

interface Step {
  icon: LucideIcon
  name: string
  body: string
  visual: ReactNode
}

const STEPS: Step[] = [
  {
    icon: Briefcase,
    name: 'Track',
    body: 'Log every application and move it through clear stages, from saved to offer.',
    visual: (
      <div className="flex flex-wrap items-center gap-1.5">
        <StatusBadge status="APPLIED" />
        <span className="text-muted-foreground">→</span>
        <StatusBadge status="OA" />
        <span className="text-muted-foreground">→</span>
        <StatusBadge status="INTERVIEW" />
        <span className="text-muted-foreground">→</span>
        <StatusBadge status="OFFER" />
      </div>
    ),
  },
  {
    icon: PencilLine,
    name: 'Prepare',
    body: 'Work through the question bank, log practice, and watch mastery rise topic by topic.',
    visual: (
      <div className="w-full max-w-xs space-y-1.5 text-xs">
        <div className="flex justify-between">
          <span className="font-medium">Binary Search</span>
          <span className="tabular-nums text-muted-foreground">54%</span>
        </div>
        <div className="h-1.5 rounded-full bg-muted">
          <div className="animate-grow-x h-full w-[54%] origin-left rounded-full bg-primary" />
        </div>
      </div>
    ),
  },
  {
    icon: MessagesSquare,
    name: 'Interview',
    body: 'Debrief every round while it is fresh, so the questions you faced turn into prep.',
    visual: (
      <div className="max-w-xs rounded-lg border bg-card px-3 py-2 text-xs">
        <p className="font-medium">Round 2 · Technical</p>
        <p className="text-muted-foreground">Asked: design a rate limiter. Felt rushed on trade-offs.</p>
      </div>
    ),
  },
  {
    icon: BarChart3,
    name: 'Analyse',
    body: 'See your conversion funnel, what is working, and how your pace compares with the cohort.',
    visual: (
      <div className="flex items-end gap-1.5" aria-hidden>
        {[100, 62, 38, 20, 10].map((height, index) => (
          <span
            key={height}
            className={`animate-grow-y w-6 origin-bottom rounded-sm ${index === 4 ? 'bg-success' : 'bg-chart-1'}`}
            style={{ height: `${height * 0.4}px`, animationDelay: `${index * 80}ms` }}
          />
        ))}
      </div>
    ),
  },
]

/** The four-step journey on a vertical line that draws itself as the section enters the viewport. */
export function JourneyTimeline() {
  return (
    <Reveal className="relative">
      <span aria-hidden className="absolute top-2 bottom-2 left-[19px] w-px bg-border" />
      <span aria-hidden className="animate-grow-y absolute top-2 bottom-2 left-[19px] w-px origin-top bg-primary [animation-duration:1600ms]" />
      <ol className="space-y-10">
        {STEPS.map((step, index) => (
          <li key={step.name} className="relative grid grid-cols-[40px_1fr] gap-5">
            <span className="z-10 flex size-10 items-center justify-center rounded-full border bg-card text-primary shadow-sm">
              <step.icon aria-hidden className="size-4" />
            </span>
            <div className="space-y-3 pt-1">
              <h3 className="text-lg font-semibold tracking-tight">
                <span className="mr-2 font-mono text-xs text-muted-foreground tabular-nums">0{index + 1}</span>
                {step.name}
              </h3>
              <p className="max-w-md text-sm text-muted-foreground">{step.body}</p>
              <div aria-hidden>{step.visual}</div>
            </div>
          </li>
        ))}
      </ol>
    </Reveal>
  )
}
