import { CornerDownLeftIcon, SearchIcon } from 'lucide-react'
import { primaryNavItems } from '@/lib/navigation'
import { cn } from '@/lib/utils'

const QUERY = 'interv'

export function PaletteDemo() {
  const results = primaryNavItems.filter((item) => item.label.toLowerCase().includes(QUERY)).slice(0, 4)

  return (
    <div className="animate-enter overflow-hidden rounded-xl border bg-popover text-left shadow-lg">
      <div className="flex items-center gap-2 border-b px-3 py-2.5">
        <SearchIcon className="size-3.5 text-muted-foreground" />
        <span className="flex-1 text-sm">{QUERY}</span>
        <kbd className="kbd">Esc</kbd>
      </div>
      <ul className="space-y-0.5 p-1.5">
        {results.map((item, index) => (
          <li
            key={item.path}
            className={cn(
              'flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs transition-colors duration-150 hover:bg-accent hover:text-accent-foreground',
              index === 0 && 'bg-accent text-accent-foreground',
            )}
          >
            <item.icon className="size-3.5 shrink-0" />
            <span className="flex-1 font-medium">{item.label}</span>
            {index === 0 && <CornerDownLeftIcon className="size-3.5 opacity-70" />}
          </li>
        ))}
      </ul>
    </div>
  )
}
