import { useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Search } from 'lucide-react'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { Breadcrumbs } from '@/components/layout/Breadcrumbs'
import { CommandPalette } from '@/components/layout/CommandPalette'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { NotificationBell } from '@/components/notifications/NotificationBell'
import { Separator } from '@/components/ui/separator'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { breadcrumbsFor, pageTitleFor } from '@/lib/navigation'

export function AppShell() {
  const { pathname } = useLocation()
  const [paletteOpen, setPaletteOpen] = useState(false)

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

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger />
          <Separator orientation="vertical" className="h-4" />
          <Breadcrumbs crumbs={breadcrumbsFor(pathname)} />
          <div className="ml-auto flex items-center gap-2">
            <button
              type="button"
              onClick={() => setPaletteOpen(true)}
              aria-label="Open command palette"
              className="inline-flex h-8 items-center gap-2 rounded-md border px-2 text-sm text-muted-foreground hover:text-foreground"
            >
              <Search className="size-3.5" />
              <span className="hidden sm:inline">Search</span>
              <kbd className="hidden rounded border px-1 text-[10px] sm:inline">Ctrl K</kbd>
            </button>
            <ThemeToggle className="hidden sm:inline-flex" />
            <NotificationBell />
          </div>
        </header>
        <main className="flex-1 overflow-auto p-4 sm:p-6">
          <Outlet />
        </main>
      </SidebarInset>
      <CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} />
    </SidebarProvider>
  )
}
