import { Monitor, Moon, Sun, type LucideIcon } from 'lucide-react'
import { useTheme, type ThemePreference } from '@/hooks/use-theme'
import { cn } from '@/lib/utils'

const OPTIONS: { value: ThemePreference; label: string; icon: LucideIcon }[] = [
  { value: 'light', label: 'Light theme', icon: Sun },
  { value: 'system', label: 'System theme', icon: Monitor },
  { value: 'dark', label: 'Dark theme', icon: Moon },
]

export function ThemeToggle({ className }: { className?: string }) {
  const { preference, setPreference } = useTheme()

  return (
    <div role="group" aria-label="Theme" className={cn('inline-flex rounded-md border p-0.5', className)}>
      {OPTIONS.map(({ value, label, icon: Icon }) => (
        <button
          key={value}
          type="button"
          aria-label={label}
          aria-pressed={preference === value}
          onClick={() => setPreference(value)}
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
