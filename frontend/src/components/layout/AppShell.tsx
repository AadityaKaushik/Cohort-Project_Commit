import { Suspense, useEffect, useState } from 'react'
import { Link, Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '@/hooks/use-auth'
import { Search } from 'lucide-react'
import { BrandGlyph } from '@/components/brand/BrandMark'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { MobileNav } from '@/components/layout/MobileNav'
import { Breadcrumbs } from '@/components/layout/Breadcrumbs'
import { CommandPalette } from '@/components/layout/CommandPalette'
import { ProductTour } from '@/components/onboarding/ProductTour'
import { ShortcutsDialog } from '@/components/layout/ShortcutsDialog'
import { useShortcuts, type ShortcutAction } from '@/hooks/use-shortcuts'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { NotificationBell } from '@/components/notifications/NotificationBell'
import { Separator } from '@/components/ui/separator'
import { Skeleton } from '@/components/ui/skeleton'
import { ErrorBoundary } from '@/components/shared/ErrorBoundary'
import { RouteProgress } from '@/components/motion/RouteProgress'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { breadcrumbsFor, pageTitleFor } from '@/lib/navigation'

const WIDE_LAYOUT_MIN_PX = 1280

function prefersExpandedSidebar(): boolean {
  return typeof window === 'undefined' || window.innerWidth >= WIDE_LAYOUT_MIN_PX
}

export function AppShell() {
  const { pathname } = useLocation()
  const { user } = useAuth()
  const navigate = useNavigate()
  const [paletteOpen, setPaletteOpen] = useState(false)
  const [shortcutsOpen, setShortcutsOpen] = useState(false)

  useShortcuts((action: ShortcutAction) => {
    if (action === 'help') {
      setShortcutsOpen(true)
    } else if (action === 'search') {
      const filter = document.getElementById('filter-q')
      if (filter) {
        filter.focus()
      } else {
        setPaletteOpen(true)
      }
    } else if (action === 'new-application') {
      navigate('/applications/new')
    } else if (action === 'go-applications') {
      navigate('/applications')
    } else if (action === 'go-dashboard') {
      navigate('/')
    } else {
      navigate('/prep')
    }
  })

  useEffect(() => {
    document.title = `${pageTitleFor(pathname)} · Cohort`
  }, [pathname])

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey)) {
        event.preventDefault()
        setPaletteOpen((open) => !open)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  if (user?.onboarded === false) {
    return <Navigate to="/onboarding" replace />
  }

  return (
    <SidebarProvider defaultOpen={prefersExpandedSidebar()}>
      <a
        href="#main"
        className="sr-only z-[110] rounded-md bg-primary px-3 py-2 text-primary-foreground focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Skip to content
      </a>
      <AppSidebar />
      <SidebarInset className="min-w-0">
        <header className="frosted sticky top-0 z-30 flex h-14 shrink-0 items-center gap-2 border-b px-3 sm:px-4">
          <SidebarTrigger className="hidden md:inline-flex" aria-label="Toggle sidebar" />
          <Link to="/" aria-label="Cohort dashboard" className="rounded-lg md:hidden">
            <BrandGlyph className="size-7" />
          </Link>
          <Separator orientation="vertical" className="h-4" />
          <Breadcrumbs crumbs={breadcrumbsFor(pathname)} />
          <div className="ml-auto flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPaletteOpen(true)}
              aria-label="Open command palette"
              className="inline-flex h-8 items-center gap-2 rounded-lg border bg-card/60 max-sm:h-10 max-sm:min-w-10 max-sm:justify-center px-2.5 text-sm text-muted-foreground transition-colors hover:border-primary/30 hover:text-foreground"
            >
              <Search aria-hidden className="size-3.5" />
              <span className="hidden pr-6 sm:inline">Search</span>
              <span className="hidden items-center gap-0.5 sm:inline-flex">
                <kbd className="kbd">Ctrl</kbd>
                <kbd className="kbd">K</kbd>
              </span>
            </button>
            <ThemeToggle className="hidden sm:inline-flex" />
            <NotificationBell />
          </div>
        </header>
        <main id="main" tabIndex={-1} className="flex-1 px-4 pt-5 pb-24 sm:px-6 sm:pt-6 md:pb-8 outline-none lg:px-8">
          <div key={pathname} className="animate-route mx-auto w-full max-w-7xl">
            <ErrorBoundary variant="inline" resetKey={pathname}>
              <Suspense fallback={<PageFallback />}>
                <Outlet />
              </Suspense>
            </ErrorBoundary>
          </div>
        </main>
      </SidebarInset>
      <MobileNav />
      <CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} />
      <ProductTour />
      <ShortcutsDialog open={shortcutsOpen} onOpenChange={setShortcutsOpen} />
    </SidebarProvider>
  )
}

function PageFallback() {
  return (
    <div role="status" aria-label="Loading page" className="space-y-6">
      <RouteProgress />
      <div className="space-y-2">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-4 w-96 max-w-full" />
      </div>
      <Skeleton className="h-40 w-full rounded-xl" />
      <Skeleton className="h-64 w-full rounded-xl" />
    </div>
  )
}
