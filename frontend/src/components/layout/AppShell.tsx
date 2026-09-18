import { Outlet, useLocation } from 'react-router-dom'
import { AppSidebar } from '@/components/layout/AppSidebar'
import { SidebarInset, SidebarProvider, SidebarTrigger } from '@/components/ui/sidebar'
import { Separator } from '@/components/ui/separator'
import { primaryNavItems, secondaryNavItems } from '@/lib/navigation'

function currentPageTitle(pathname: string) {
  const item = [...primaryNavItems, ...secondaryNavItems].find((navItem) =>
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
        </header>
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
