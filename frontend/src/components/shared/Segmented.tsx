import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface SegmentedOption<T extends string> {
  value: T
  label: string
  icon?: LucideIcon
}

interface SegmentedProps<T extends string> {
  label: string
  value: T
  options: SegmentedOption<T>[]
  onChange: (value: T) => void
  disabled?: boolean
  className?: string
}

/** A pill-shaped set of mutually exclusive toggles; each option reports its state through aria-pressed. */
export function Segmented<T extends string>({ label, value, options, onChange, disabled, className }: SegmentedProps<T>) {
  return (
    <div role="group" aria-label={label} className={cn('inline-flex items-center gap-0.5 rounded-lg border bg-surface-2 p-0.5', className)}>
      {options.map(({ value: optionValue, label: optionLabel, icon: Icon }) => {
        const active = optionValue === value
        return (
          <button
            key={optionValue}
            type="button"
            aria-pressed={active}
            disabled={disabled}
            onClick={() => onChange(optionValue)}
            className={cn(
              'inline-flex h-7 items-center gap-1.5 rounded-md px-2.5 text-[0.8rem] font-medium transition-[background-color,color,box-shadow] duration-150 disabled:opacity-50',
              active ? 'bg-card text-foreground shadow-[var(--elevation)] ring-1 ring-border' : 'text-muted-foreground hover:text-foreground',
            )}
          >
            {Icon && <Icon aria-hidden className="size-3.5" />}
            {optionLabel}
          </button>
        )
      })}
    </div>
  )
}
