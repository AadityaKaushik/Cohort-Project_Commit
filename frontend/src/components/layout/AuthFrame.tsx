import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { Ascent, type AscentStage } from '@/components/board/Ascent'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Skeleton } from '@/components/ui/skeleton'

const STAGES: AscentStage[] = [
  { key: 'saved', label: 'Saved', count: 0, line: 'saved' },
  { key: 'applied', label: 'Applied', count: 0, line: 'applied' },
  { key: 'interview', label: 'Interview', count: 0, line: 'interview' },
  { key: 'offer', label: 'Offer', count: 0, line: 'offer' },
]

export function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <aside className="flex flex-col justify-between gap-8 bg-brand px-6 py-6 text-brand-foreground sm:px-10 lg:py-10">
        <Link to="/" aria-label="Cohort home" className="w-fit rounded-sm">
          <BrandMark wordmarkClassName="text-[26px]" />
        </Link>
        <div className="hidden lg:block">
          <p className="text-display text-[4.5rem] xl:text-[5.5rem]">
            From saved <span className="text-signal">to signed.</span>
          </p>
          <div className="mt-10 max-w-md">
            <Ascent stages={STAGES} inverse showCounts={false} />
          </div>
        </div>
        <p className="board-label hidden opacity-75 lg:block">Job application tracker with interview preparation</p>
      </aside>

      <div className="flex flex-col px-4 py-6 sm:px-10">
        <header className="flex justify-end">
          <ThemeToggle />
        </header>
        <main className="flex flex-1 items-start justify-center py-10 sm:items-center">
          <div className="w-full max-w-[420px]">{children}</div>
        </main>
        <footer className="text-center text-xs text-muted-foreground">
          <Link to="/" className="hover:text-foreground hover:underline">
            Back to home
          </Link>
        </footer>
      </div>
    </div>
  )
}

export function AuthFrameSkeleton() {
  return (
    <div role="status" aria-label="Loading" className="flex min-h-screen items-center justify-center bg-background px-4">
      <div className="w-full max-w-[420px] space-y-4">
        <Skeleton className="h-8 w-28" />
        <Skeleton className="h-10 w-56" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    </div>
  )
}

export function AuthIntro({ title, subtitle, eyebrow }: { title: string; subtitle?: string; eyebrow?: string }) {
  return (
    <div className="mb-8 space-y-2 border-b-2 border-rule pb-5">
      {eyebrow && <p className="eyebrow">{eyebrow}</p>}
      <h1 className="text-display text-[2.6rem]">{title}</h1>
      {subtitle && <p className="text-sm text-muted-foreground">{subtitle}</p>}
    </div>
  )
}
