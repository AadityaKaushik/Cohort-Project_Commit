import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { BrandMark } from '@/components/brand/BrandMark'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { Skeleton } from '@/components/ui/skeleton'

export function AuthFrame({ children }: { children: ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col bg-muted/40 px-4 py-6">
      <header className="flex justify-end">
        <ThemeToggle />
      </header>
      <main className="flex flex-1 items-start justify-center py-8 sm:items-center">
        <div className="w-full max-w-[400px] rounded-lg border bg-card p-6 shadow-sm sm:p-8">
          <Link to="/" aria-label="Cohort home" className="mb-6 inline-flex rounded-lg">
            <BrandMark />
          </Link>
          {children}
        </div>
      </main>
      <footer className="text-center text-xs text-muted-foreground">
        <Link to="/" className="hover:text-foreground hover:underline">
          Back to home
        </Link>
      </footer>
    </div>
  )
}

export function AuthFrameSkeleton() {
  return (
    <div role="status" aria-label="Loading" className="flex min-h-screen items-center justify-center bg-muted/40 px-4">
      <div className="w-full max-w-[400px] space-y-4 rounded-lg border bg-card p-6 sm:p-8">
        <Skeleton className="h-8 w-28" />
        <Skeleton className="h-7 w-48" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    </div>
  )
}

export function AuthIntro({ title, subtitle, eyebrow }: { title: string; subtitle?: string; eyebrow?: string }) {
  return (
    <div className="mb-6 space-y-1">
      {eyebrow && <p className="text-sm text-muted-foreground">{eyebrow}</p>}
      <h1 className="text-2xl font-semibold">{title}</h1>
      {subtitle && <p className="text-sm text-muted-foreground">{subtitle}</p>}
    </div>
  )
}
