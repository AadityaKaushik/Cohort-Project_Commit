import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Check, Flame } from 'lucide-react'
import { BrandMark } from '@/components/brand/BrandMark'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { TypingText } from '@/components/motion/TypingText'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { StatusBadge } from '@/components/shared/StatusBadge'

const BENEFITS = [
  'Every application, interview and debrief in one place',
  'Prep tasks that come from your own interviews',
  'Benchmarks learned privately, never from your raw data',
]

function BrandPanel() {
  return (
    <aside
      aria-label="About Cohort"
      className="relative isolate hidden overflow-hidden border-l bg-surface-2 lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16"
    >
      <div aria-hidden className="hero-glow absolute inset-0 -z-10" />
      <div aria-hidden className="hero-grid absolute inset-0 -z-10" />
      <BrandMark animated />
      <div className="animate-enter max-w-md" style={{ animationDelay: '150ms' }}>
        <h2 className="text-display text-4xl xl:text-5xl">Your search, organised and in motion.</h2>
        <p className="mt-4 font-heading text-lg font-medium tracking-tight text-primary">
          <TypingText phrases={['Log it. Prepare for it. Learn from it.']} loop={false} />
        </p>
        <ul className="mt-8 space-y-3">
          {BENEFITS.map((benefit) => (
            <li key={benefit} className="flex items-start gap-3 text-sm text-muted-foreground">
              <span className="mt-0.5 flex size-5 shrink-0 items-center justify-center rounded-full bg-primary/12 text-primary">
                <Check aria-hidden className="size-3" />
              </span>
              {benefit}
            </li>
          ))}
        </ul>
        <div inert aria-hidden className="surface-raised animate-float mt-10 flex items-center gap-5 rounded-2xl border p-5">
          <ProgressRing value={12} max={20} label="Sample prep progress" size={76} stroke={7}>
            <span className="text-base font-semibold tabular-nums">60%</span>
          </ProgressRing>
          <div className="min-w-0 flex-1 space-y-2">
            <div className="flex items-center justify-between gap-2">
              <span className="truncate text-sm font-medium">Northwind Labs</span>
              <StatusBadge status="INTERVIEW" />
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="truncate text-sm font-medium">Atlas Health</span>
              <StatusBadge status="OFFER" />
            </div>
            <p className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Flame className="size-3.5 text-warning" />
              <span>
                  <span className="tabular-nums">4</span>-day streak
                </span>
            </p>
          </div>
        </div>
      </div>
      <RotatingQuote className="max-w-sm text-sm" />
    </aside>
  )
}

export function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-[minmax(0,1fr)_minmax(0,1.05fr)]">
      <div className="relative isolate flex min-h-screen flex-col px-4 py-6 sm:px-8">
        <div aria-hidden className="hero-glow absolute inset-x-0 top-0 -z-10 h-80 lg:hidden" />
        <header className="flex items-center justify-between gap-4">
          <Link to="/" aria-label="Cohort home" className="rounded-lg lg:invisible">
            <BrandMark animated />
          </Link>
          <ThemeToggle />
        </header>
        <main className="flex flex-1 items-center justify-center py-10">
          <div className="animate-enter w-full max-w-sm">{children}</div>
        </main>
        <RotatingQuote rotate={false} className="mx-auto mb-6 max-w-xs text-sm lg:hidden" />
        <footer className="text-center text-xs text-muted-foreground lg:text-left">
          <Link to="/" className="hover:text-foreground">
            Back to home
          </Link>
        </footer>
      </div>
      <BrandPanel />
    </div>
  )
}
