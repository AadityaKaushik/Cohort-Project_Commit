import { useEffect, useState } from 'react'
import { CornerDownLeftIcon, SearchIcon } from 'lucide-react'
import { useInView, useReducedMotion } from '@/hooks/use-motion'
import { primaryNavItems } from '@/lib/navigation'
import { cn } from '@/lib/utils'

const QUERIES = ['interv', 'analy', 'resum']
const TYPE_MS = 110
const HOLD_MS = 1500
const PRESS_MS = 450

export function PaletteDemo() {
  const reduced = useReducedMotion()
  const [ref, inView] = useInView<HTMLDivElement>()
  const [queryIndex, setQueryIndex] = useState(0)
  const [length, setLength] = useState(reduced ? QUERIES[0].length : 0)
  const [pressed, setPressed] = useState(false)

  const query = QUERIES[queryIndex]

  useEffect(() => {
    if (reduced || !inView || document.hidden) return
    let timer: number
    if (length < query.length) {
      timer = window.setTimeout(() => setLength((current) => current + 1), TYPE_MS)
    } else if (!pressed) {
      timer = window.setTimeout(() => setPressed(true), HOLD_MS)
    } else {
      timer = window.setTimeout(() => {
        setPressed(false)
        setLength(0)
        setQueryIndex((current) => (current + 1) % QUERIES.length)
      }, PRESS_MS)
    }
    return () => window.clearTimeout(timer)
  }, [reduced, inView, length, pressed, query.length])

  const typed = query.slice(0, length)
  const results = primaryNavItems.filter((item) => item.label.toLowerCase().includes(typed.toLowerCase())).slice(0, 4)

  return (
    <div ref={ref} className="overflow-hidden rounded-xl border bg-popover text-left shadow-lg">
      <div className="flex items-center gap-2 border-b px-3 py-2.5">
        <SearchIcon className="size-3.5 text-muted-foreground" />
        <span className="flex-1 text-sm">
          {typed}
          <span className="ml-px inline-block h-3.5 w-px translate-y-0.5 animate-caret bg-foreground" />
        </span>
        <kbd className="kbd">Esc</kbd>
      </div>
      <ul className="space-y-0.5 p-1.5">
        {results.map((item, index) => (
          <li
            key={item.path}
            className={cn(
              'flex items-center gap-2.5 rounded-md px-2.5 py-2 text-xs transition-colors duration-150',
              index === 0 && 'bg-accent text-accent-foreground',
              index === 0 && pressed && 'bg-primary text-primary-foreground',
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
