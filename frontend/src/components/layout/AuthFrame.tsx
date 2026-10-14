import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { RouteProgress } from '@/components/motion/RouteProgress'
import { CountUp } from '@/components/motion/Reveal'
import { TypingText } from '@/components/motion/TypingText'
import { useReducedMotion } from '@/hooks/use-motion'
import { CompanyMark } from '@/components/shared/CompanyMark'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { Skeleton } from '@/components/ui/skeleton'
import type { ApplicationStatusValue } from '@/lib/applications-api'

const USE_LINES = ['Track every application', 'Prepare for every interview', 'Learn from your cohort, privately'] as const
const PREVIEW_ROWS = [
  { company: 'Northwind Labs', role: 'Backend Engineer' },
  { company: 'Atlas Health', role: 'Platform Engineer' },
  { company: 'Lumen Systems', role: 'Software Engineer' },
]
const PREVIEW_STAGES: ApplicationStatusValue[] = ['APPLIED', 'INTERVIEW', 'OFFER']
const ADVANCE_EVERY_MS = 3200

function PanelShell({ children }: { children: ReactNode }) {
  return (
    <aside
      aria-label="About Cohort"
      className="relative isolate hidden overflow-hidden border-l bg-surface-2 lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-14"
    >
      <div aria-hidden className="aurora hero-glow absolute -inset-x-1/4 -top-1/4 -z-10 h-[80%] opacity-60" />
      {children}
    </aside>
  )
}

function PipelinePreview() {
  const reduced = useReducedMotion()
  const [tick, setTick] = useState(0)

  useEffect(() => {
    if (reduced) {
      return
    }
    const timer = window.setInterval(() => {
      if (!document.hidden) {
        setTick((current) => current + 1)
      }
    }, ADVANCE_EVERY_MS)
    return () => window.clearInterval(timer)
  }, [reduced])

  const stages = PREVIEW_ROWS.map((_, index) => (index + tick) % PREVIEW_STAGES.length)
  const offers = stages.filter((stage) => stage === PREVIEW_STAGES.length - 1).length

  return (
    <div inert aria-hidden className="surface-raised overflow-hidden rounded-xl border">
      <div className="flex items-center justify-between border-b bg-surface-2/60 px-4 py-2.5">
        <span className="eyebrow">Pipeline</span>
        <span className="text-xs text-muted-foreground">
          <CountUp value={offers} className="font-semibold text-success" /> {offers === 1 ? 'offer' : 'offers'} this week
        </span>
      </div>
      <ul className="stagger divide-y">
        {PREVIEW_ROWS.map((row, index) => {
          const stage = stages[index]
          return (
            <li key={row.company} className="flex items-center gap-3 px-4 py-3">
              <CompanyMark name={row.company} />
              <span className="min-w-0 flex-1 space-y-1.5">
                <span className="block truncate text-sm font-medium">{row.company}</span>
                <span className="block h-1 overflow-hidden rounded-full bg-muted">
                  <span
                    className="block h-full origin-left rounded-full bg-primary transition-transform duration-700 ease-out"
                    style={{ transform: `scaleX(${(stage + 1) / PREVIEW_STAGES.length})` }}
                  />
                </span>
              </span>
              <span key={stage} className="animate-quote">
                <StatusBadge status={PREVIEW_STAGES[stage]} />
              </span>
            </li>
          )
        })}
      </ul>
    </div>
  )
}

function BrandPanel() {
  return (
    <PanelShell>
      <BrandMark className="brand-continuity" />
      <div className="max-w-md space-y-8">
        <div className="space-y-4">
          <h2 className="text-display text-4xl text-balance">
            <TypingText text="Your job search, kept in order." sessionKey="auth-panel-headline" />
          </h2>
          <p className="font-heading text-lg font-medium tracking-tight text-primary">
            <TypingText text={USE_LINES} />
          </p>
          <p className="text-base text-pretty text-muted-foreground">
            Applications, interviews and preparation in one calm place. What you write stays private to you.
          </p>
        </div>
        <PipelinePreview />
      </div>
      <RotatingQuote className="max-w-sm text-sm" />
    </PanelShell>
  )
}

function FormColumn({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col px-5 py-6 sm:px-10">
      <header className="flex items-center justify-between gap-4">
        <Link to="/" aria-label="Cohort home" className="rounded-lg lg:invisible">
          <BrandMark className="brand-continuity lg:[view-transition-name:none]" />
        </Link>
        <ThemeToggle />
      </header>
      <main className="flex flex-1 items-center justify-center py-10">
        <div className="stagger w-full max-w-[380px]">{children}</div>
      </main>
      <footer className="flex items-center justify-between text-xs text-muted-foreground">
        <span>Cohort</span>
        <Link to="/" className="rounded transition-colors hover:text-foreground">
          Back to home
        </Link>
      </footer>
    </div>
  )
}

export function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-2">
      <FormColumn>{children}</FormColumn>
      <BrandPanel />
    </div>
  )
}

// Holds the split layout in place while an auth route's chunk loads, so the swap never flashes a blank page.
export function AuthFrameSkeleton() {
  return (
    <div role="status" aria-label="Loading" className="grid min-h-screen bg-background lg:grid-cols-2">
      <RouteProgress />
      <div className="flex min-h-screen flex-col px-5 py-6 sm:px-10">
        <div className="flex items-center justify-between">
          <Skeleton className="size-8 rounded-lg lg:invisible" />
          <Skeleton className="h-8 w-24 rounded-md" />
        </div>
        <div className="flex flex-1 items-center justify-center py-10">
          <div className="w-full max-w-[380px] space-y-6">
            <div className="space-y-3">
              <Skeleton className="h-8 w-56" />
              <Skeleton className="h-4 w-64" />
            </div>
            <Skeleton className="h-11 w-full rounded-xl" />
            <div className="space-y-4">
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-10 w-full" />
              <Skeleton className="h-11 w-full rounded-xl" />
            </div>
          </div>
        </div>
      </div>
      <div className="hidden border-l bg-surface-2 lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-14">
        <Skeleton className="h-8 w-28" />
        <div className="max-w-md space-y-4">
          <Skeleton className="h-10 w-80" />
          <Skeleton className="h-4 w-96" />
          <Skeleton className="mt-6 h-44 w-full rounded-xl" />
        </div>
        <Skeleton className="h-4 w-64" />
      </div>
    </div>
  )
}

export function AuthIntro({ title, subtitle, eyebrow }: { title: string; subtitle?: string; eyebrow?: string }) {
  return (
    <div className="mb-8 space-y-2">
      {eyebrow && <p className="eyebrow">{eyebrow}</p>}
      <h1 className="text-display text-[2rem] text-balance">
        <TypingText text={title} sessionKey={`auth-title:${title}`} />
      </h1>
      {subtitle && (
        <p className="text-sm text-pretty text-muted-foreground">
          <TypingText text={subtitle} sessionKey={`auth-subtitle:${subtitle}`} startDelay={350} />
        </p>
      )}
    </div>
  )
}
