import { Link, useLocation } from 'react-router-dom'
import { primaryNavItems, secondaryNavItems, type NavItem } from '@/lib/navigation'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  SidebarSeparator,
} from '@/components/ui/sidebar'

function isNavItemActive(pathname: string, itemPath: string) {
  if (itemPath === '/') {
    return pathname === '/'
  }
  return pathname === itemPath || pathname.startsWith(`${itemPath}/`)
}

function NavGroup({ items }: { items: NavItem[] }) {
  const { pathname } = useLocation()

  return (
    <SidebarMenu>
      {items.map((item) => {
        const active = isNavItemActive(pathname, item.path)
        return (
          <SidebarMenuItem key={item.path}>
            <SidebarMenuButton asChild isActive={active} tooltip={item.label}>
              <Link to={item.path}>
                <item.icon />
                <span>{item.label}</span>
              </Link>
            </SidebarMenuButton>
            {item.count !== undefined && <SidebarMenuBadge>{item.count}</SidebarMenuBadge>}
          </SidebarMenuItem>
        )
      })}
    </SidebarMenu>
  )
}

export function AppSidebar() {
  return (
    <Sidebar collapsible="icon">
      <SidebarHeader>
        <div className="flex items-center gap-2 px-2 py-1.5">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-md bg-primary text-sm font-semibold text-primary-foreground">
            C
          </div>
          <span className="text-sm font-semibold tracking-tight group-data-[collapsible=icon]:hidden">
            Cohort
          </span>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarGroupContent>
            <NavGroup items={primaryNavItems} />
          </SidebarGroupContent>
        </SidebarGroup>
      </SidebarContent>

      <SidebarSeparator />

      <SidebarFooter>
        <NavGroup items={secondaryNavItems} />
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  )
}
