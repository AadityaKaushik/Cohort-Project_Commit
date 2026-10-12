import { Outlet, useLocation } from 'react-router-dom'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { Separator } from '@/components/ui/separator'
import { ThemeToggle } from '@/components/layout/ThemeToggle'
import { NotificationBell } from '@/components/notifications/NotificationBell'
import { primaryNavItems, secondaryNavItems } from '@/lib/navigation'

/** Pages reachable from the header rather than the sidebar still get a proper title. */
const HEADER_ONLY_PAGES = [{ path: '/notifications', label: 'Notifications' }]

function currentPageTitle(pathname: string) {
  const item = [...primaryNavItems, ...secondaryNavItems, ...HEADER_ONLY_PAGES].find((navItem) =>
    navItem.path === '/' ? pathname === '/' : pathname.startsWith(navItem.path),
  )
  return item?.label ?? 'Cohort'
}

export function AppShell() {
  const { pathname } = useLocation()

  return (
    <SidebarProvider>
      <AppSidebar />
      <SidebarInset>
        <header className="flex h-14 shrink-0 items-center gap-2 border-b px-4">
          <SidebarTrigger />
          <Separator orientation="vertical" className="h-4" />
          <h1 className="text-sm font-medium">{currentPageTitle(pathname)}</h1>
          <div className="ml-auto flex items-center gap-1">
            <ThemeToggle className="hidden sm:inline-flex" />
            <NotificationBell />
          </div>
        </header>
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
