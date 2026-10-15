import { useNavigate } from 'react-router-dom'
import { ChevronsUpDown, CircleHelpIcon, LogOut, Monitor, Moon, Sun, UserRound } from 'lucide-react'
import { useAuth } from '@/hooks/use-auth'
import { replayTour } from '@/lib/tour'
import { useTheme, type ThemePreference } from '@/hooks/use-theme'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export function UserMenu() {
  const { user, logout } = useAuth()
  const { preference, setPreference } = useTheme()
  const navigate = useNavigate()
  const name = user?.display_name || user?.email || 'Account'
  const initial = name.replace(/[^\p{L}\p{N}]/gu, '').charAt(0).toUpperCase() || '?'

  async function handleLogout() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <button
          type="button"
          aria-label="Account menu"
          className="flex w-full items-center gap-2.5 rounded-lg p-1.5 text-left text-sm transition-colors hover:bg-sidebar-accent group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:p-0.5"
        >
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-primary/12 text-xs font-semibold text-primary ring-1 ring-primary/25">
            {initial}
          </span>
          <span className="min-w-0 flex-1 group-data-[collapsible=icon]:hidden">
            <span className="block truncate font-medium">{name}</span>
            {user?.email && user.display_name && (
              <span className="block truncate text-xs text-muted-foreground">{user.email}</span>
            )}
          </span>
          <ChevronsUpDown className="size-4 shrink-0 text-muted-foreground group-data-[collapsible=icon]:hidden" />
        </button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" side="top" className="w-60">
        <DropdownMenuLabel className="truncate font-normal text-muted-foreground">{user?.email ?? name}</DropdownMenuLabel>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={() => navigate('/profile')}>
          <UserRound />
          Profile
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={replayTour}>
          <CircleHelpIcon />
          Replay product tour
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuLabel className="text-xs font-medium text-muted-foreground">Theme</DropdownMenuLabel>
        <DropdownMenuRadioGroup value={preference} onValueChange={(value) => setPreference(value as ThemePreference)}>
          <DropdownMenuRadioItem value="light">
            <Sun />
            Light
          </DropdownMenuRadioItem>
          <DropdownMenuRadioItem value="dark">
            <Moon />
            Dark
          </DropdownMenuRadioItem>
          <DropdownMenuRadioItem value="system">
            <Monitor />
            System
          </DropdownMenuRadioItem>
        </DropdownMenuRadioGroup>
        <DropdownMenuSeparator />
        <DropdownMenuItem onSelect={handleLogout}>
          <LogOut />
          Log out
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
