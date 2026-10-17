import { Link, useLocation } from 'react-router-dom'
import { ChevronDownIcon } from 'lucide-react'
import { BrandGlyph, BrandMark } from '@/components/brand/BrandMark'
import { UserMenu } from '@/components/layout/UserMenu'
import { isNavItemActive, navGroups, secondaryNavItems, type NavItem } from '@/lib/navigation'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  SidebarSeparator,
  useSidebar,
} from '@/components/ui/sidebar'

function NavList({ items }: { items: NavItem[] }) {
  const { pathname } = useLocation()
  const { isMobile, setOpenMobile } = useSidebar()

  return (
    <SidebarMenu>
      {items.map((item) => {
        const active = isNavItemActive(pathname, item.path)
        return (
          <SidebarMenuItem key={item.path} className="relative">
            <span
              aria-hidden
              data-active={active}
              className="absolute top-1.5 bottom-1.5 -left-2 w-[3px] origin-center scale-y-0 rounded-r-full bg-sidebar-primary opacity-0 transition-[transform,opacity] duration-300 ease-out data-[active=true]:scale-y-100 data-[active=true]:opacity-100 group-data-[collapsible=icon]:-left-2"
            />
            <SidebarMenuButton
              asChild
              isActive={active}
              tooltip={item.label}
              className="h-9 transition-colors duration-150 data-active:font-semibold data-active:text-sidebar-primary [&_svg]:text-sidebar-muted-foreground data-active:[&_svg]:text-sidebar-primary"
            >
              <Link
                to={item.path}
                aria-current={active ? 'page' : undefined}
                onClick={() => isMobile && setOpenMobile(false)}
              >
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

function CollapsedGroup({ label, items }: { label: string; items: NavItem[] }) {
  const { pathname } = useLocation()
  const hasActive = items.some((item) => isNavItemActive(pathname, item.path))

  return (
    <SidebarGroup className="py-1.5">
      <details open={hasActive} className="group/more">
        <summary className="board-label flex h-7 cursor-pointer list-none items-center justify-between rounded-sm px-2 text-sidebar-muted-foreground hover:text-sidebar-foreground group-data-[collapsible=icon]:hidden [&::-webkit-details-marker]:hidden">
          {label}
          <ChevronDownIcon aria-hidden className="size-3.5 transition-transform duration-200 group-open/more:rotate-180" />
        </summary>
        <SidebarGroupContent>
          <NavList items={items} />
        </SidebarGroupContent>
      </details>
    </SidebarGroup>
  )
}

export function AppSidebar() {
  return (
    <Sidebar collapsible="icon">
      <SidebarHeader className="h-14 justify-center border-b border-sidebar-border px-3">
        <Link to="/" aria-label="Cohort dashboard" className="flex items-center rounded-lg">
          <BrandMark className="group-data-[collapsible=icon]:hidden" glyphClassName="size-7" wordmarkClassName="text-[17px]" />
          <BrandGlyph className="hidden size-7 group-data-[collapsible=icon]:inline-flex" />
        </Link>
      </SidebarHeader>

      <SidebarContent className="gap-0 py-2">
        <nav aria-label="Sections">
          {navGroups.map((group) =>
            group.collapsed ? (
              <CollapsedGroup key={group.label} label={group.label} items={group.items} />
            ) : (
              <SidebarGroup key={group.label} className="py-1.5">
                <SidebarGroupLabel className="board-label h-7 text-sidebar-muted-foreground">{group.label}</SidebarGroupLabel>
                <SidebarGroupContent>
                  <NavList items={group.items} />
                </SidebarGroupContent>
              </SidebarGroup>
            ),
          )}
        </nav>
      </SidebarContent>

      <SidebarSeparator />

      <SidebarFooter>
        <NavList items={secondaryNavItems} />
        <UserMenu />
      </SidebarFooter>

      <SidebarRail />
    </Sidebar>
  )
}
