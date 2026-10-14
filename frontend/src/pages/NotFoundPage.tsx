import { useEffect } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeftIcon, CompassIcon } from 'lucide-react'
import { BrandMark } from '@/components/brand/BrandMark'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  useEffect(() => {
    document.title = 'Page not found · Cohort'
  }, [])

  return (
    <div className="relative isolate flex min-h-screen flex-col items-center justify-center gap-6 overflow-hidden bg-background px-6 text-center">
      <div aria-hidden className="hero-glow absolute inset-x-0 top-0 -z-10 h-96" />
      <div aria-hidden className="hero-grid absolute inset-0 -z-10" />
      <BrandMark />
      <div className="animate-enter flex size-14 items-center justify-center rounded-2xl border bg-card text-primary shadow-[var(--elevation)]">
        <CompassIcon aria-hidden className="size-6" />
      </div>
      <div className="animate-enter max-w-md space-y-2">
        <p className="eyebrow">Error 404</p>
        <h1 className="text-display text-3xl">This page doesn't exist</h1>
        <p className="text-sm text-muted-foreground">
          The link may be broken, or the page may have moved.
        </p>
      </div>
      <Button asChild>
        <Link to="/">
          <ArrowLeftIcon aria-hidden />
          Return home
        </Link>
      </Button>
    </div>
  )
}
