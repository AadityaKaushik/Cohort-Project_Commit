import { Link, useLocation } from 'react-router-dom'
import { Menu } from 'lucide-react'
import { useSidebar } from '@/components/ui/sidebar'
import { isNavItemActive, mobileNavItems } from '@/lib/navigation'
import { cn } from '@/lib/utils'

const LABELS: Record<string, string> = {
  '/': 'Home',
  '/applications': 'Applications',
  '/interviews': 'Interviews',
  '/prep': 'Prep',
}

export function MobileNav() {
  const { pathname } = useLocation()
  const { isMobile, setOpenMobile } = useSidebar()

  if (!isMobile) {
    return null
  }

  return (
    <nav
      aria-label="Quick navigation"
      className="frosted fixed inset-x-0 bottom-0 z-40 border-t pb-[env(safe-area-inset-bottom)]"
    >
      <ul className="grid grid-cols-5">
        {mobileNavItems.map((item) => {
          const active = isNavItemActive(pathname, item.path)
          return (
            <li key={item.path}>
              <Link
                to={item.path}
                aria-current={active ? 'page' : undefined}
                className={cn(
                  'flex h-16 flex-col items-center justify-center gap-1 text-[11px] font-medium transition-colors',
                  active ? 'text-primary' : 'text-muted-foreground hover:text-foreground',
                )}
              >
                <item.icon aria-hidden className={cn('size-5 transition-transform duration-200', active && 'scale-110')} />
                {LABELS[item.path]}
              </Link>
            </li>
          )
        })}
        <li>
          <button
            type="button"
            onClick={() => setOpenMobile(true)}
            className="flex h-16 w-full flex-col items-center justify-center gap-1 text-[11px] font-medium text-muted-foreground hover:text-foreground"
          >
            <Menu aria-hidden className="size-5" />
            More
          </button>
        </li>
      </ul>
    </nav>
  )
}
