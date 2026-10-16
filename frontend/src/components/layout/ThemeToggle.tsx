import type { MouseEvent } from 'react'
import { flushSync } from 'react-dom'
import { Monitor, Moon, Sun, type LucideIcon } from 'lucide-react'
import { prefersReducedMotion } from '@/lib/motion'
import { useTheme, type ThemePreference } from '@/hooks/use-theme'
import { cn } from '@/lib/utils'

const OPTIONS: { value: ThemePreference; label: string; icon: LucideIcon }[] = [
  { value: 'light', label: 'Light theme', icon: Sun },
  { value: 'system', label: 'System theme', icon: Monitor },
  { value: 'dark', label: 'Dark theme', icon: Moon },
]

const REVEAL_MS = 520

function resolves(preference: ThemePreference): boolean {
  return preference === 'dark' || (preference === 'system' && window.matchMedia?.('(prefers-color-scheme: dark)').matches)
}

export function ThemeToggle({ className }: { className?: string }) {
  const { preference, setPreference } = useTheme()

  function choose(value: ThemePreference, event: MouseEvent<HTMLButtonElement>) {
    const root = document.documentElement
    const changes = resolves(value) !== root.classList.contains('dark')
    if (!changes || typeof document.startViewTransition !== 'function' || prefersReducedMotion()) {
      setPreference(value)
      return
    }
    const x = event.clientX || window.innerWidth / 2
    const y = event.clientY || 0
    const radius = Math.hypot(Math.max(x, window.innerWidth - x), Math.max(y, window.innerHeight - y))
    const transition = document.startViewTransition(() => {
      flushSync(() => setPreference(value))
      root.classList.toggle('dark', resolves(value))
      root.classList.remove('theme-transition')
    })
    transition.ready
      .then(() =>
        root.animate(
          { clipPath: [`circle(0px at ${x}px ${y}px)`, `circle(${radius}px at ${x}px ${y}px)`] },
          { duration: REVEAL_MS, easing: 'cubic-bezier(0.65, 0, 0.35, 1)', pseudoElement: '::view-transition-new(root)' },
        ),
      )
      .catch(() => undefined)
  }

  return (
    <div role="group" aria-label="Theme" className={cn('inline-flex rounded-md border p-0.5', className)}>
      {OPTIONS.map(({ value, label, icon: Icon }) => (
        <button
          key={value}
          type="button"
          aria-label={label}
          aria-pressed={preference === value}
          onClick={(event) => choose(value, event)}
          className={cn(
            'inline-flex size-7 items-center justify-center rounded-[5px] text-muted-foreground transition-colors hover:text-foreground',
            preference === value && 'bg-muted text-foreground',
          )}
        >
          <Icon className="size-3.5" />
        </button>
      ))}
    </div>
  )
}
