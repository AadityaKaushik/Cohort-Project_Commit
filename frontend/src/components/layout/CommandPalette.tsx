import { useEffect, useMemo, useState, type KeyboardEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, Search, type LucideIcon } from 'lucide-react'
import { useAuth } from '@/hooks/use-auth'
import { primaryNavItems, secondaryNavItems } from '@/lib/navigation'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'

interface Command {
  id: string
  label: string
  group: 'Go to' | 'Create' | 'Account'
  icon: LucideIcon
  run: () => void
}

export function CommandPalette({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const navigate = useNavigate()
  const { logout } = useAuth()
  const [query, setQuery] = useState('')
  const [active, setActive] = useState(0)

  const commands = useMemo<Command[]>(() => {
    const go = (path: string) => () => navigate(path)
    return [
      ...[...primaryNavItems, ...secondaryNavItems].map<Command>((item) => ({
        id: `go-${item.path}`,
        label: item.label,
        group: 'Go to',
        icon: item.icon,
        run: go(item.path),
      })),
      { id: 'new-application', label: 'New application', group: 'Create', icon: Plus, run: go('/applications/new') },
      { id: 'new-resume', label: 'New resume', group: 'Create', icon: Plus, run: go('/resumes/new') },
      { id: 'new-interview', label: 'New interview', group: 'Create', icon: Plus, run: go('/interviews/new') },
      {
        id: 'logout',
        label: 'Log out',
        group: 'Account',
        icon: Search,
        run: () => {
          void logout().then(() => navigate('/login', { replace: true }))
        },
      },
    ]
  }, [navigate, logout])

  const results = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle ? commands.filter((command) => command.label.toLowerCase().includes(needle)) : commands
  }, [commands, query])

  useEffect(() => {
    if (!open) {
      setQuery('')
      setActive(0)
    }
  }, [open])

  function execute(command: Command) {
    onOpenChange(false)
    command.run()
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setActive((index) => Math.min(index + 1, results.length - 1))
    } else if (event.key === 'ArrowUp') {
      event.preventDefault()
      setActive((index) => Math.max(index - 1, 0))
    } else if (event.key === 'Enter' && results[active]) {
      event.preventDefault()
      execute(results[active])
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent showCloseButton={false} className="top-[20%] translate-y-0 gap-0 p-0 sm:max-w-lg">
        <DialogTitle className="sr-only">Command palette</DialogTitle>
        <DialogDescription className="sr-only">Search for a page or an action.</DialogDescription>
        <div className="flex items-center gap-2 border-b px-3">
          <Search aria-hidden className="size-4 text-muted-foreground" />
          <input
            autoFocus
            role="combobox"
            aria-expanded
            aria-controls="command-results"
            aria-label="Search commands"
            placeholder="Search pages and actions"
            value={query}
            onChange={(event) => {
              setQuery(event.target.value)
              setActive(0)
            }}
            onKeyDown={handleKeyDown}
            className="h-11 flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
          />
        </div>
        <ul id="command-results" role="listbox" className="max-h-72 overflow-y-auto p-1">
          {results.length === 0 && <li className="px-3 py-6 text-center text-sm text-muted-foreground">No matches</li>}
          {results.map((command, index) => (
            <li
              key={command.id}
              role="option"
              aria-selected={index === active}
              onMouseEnter={() => setActive(index)}
              onClick={() => execute(command)}
              className={cn(
                'flex cursor-pointer items-center gap-2 rounded-md px-3 py-2 text-sm',
                index === active && 'bg-muted',
              )}
            >
              <command.icon aria-hidden className="size-4 text-muted-foreground" />
              <span className="flex-1">{command.label}</span>
              <span className="text-xs text-muted-foreground">{command.group}</span>
            </li>
          ))}
        </ul>
      </DialogContent>
    </Dialog>
  )
}
