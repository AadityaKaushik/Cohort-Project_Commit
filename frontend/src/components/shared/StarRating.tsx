import { StarIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface StarRatingProps {
  value: number | null
  onChange: (value: number) => void
  label?: string
  disabled?: boolean
}

/** A 1-5 star picker (self-rating / perceived difficulty, US-29/US-30) — one click, no typing, which is what keeps the debrief fast (NFR-31). */
export function StarRating({ value, onChange, label, disabled = false }: StarRatingProps) {
  return (
    <div className="space-y-1">
      {label && <p className="text-sm font-medium">{label}</p>}
      <div className="flex items-center gap-1" role="radiogroup" aria-label={label}>
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            key={star}
            type="button"
            role="radio"
            aria-checked={value === star}
            aria-label={`${star} out of 5`}
            disabled={disabled}
            onClick={() => onChange(star)}
            className={cn('rounded-md p-1 transition-transform duration-150 disabled:cursor-not-allowed', !disabled && 'hover:scale-110 active:scale-95')}
          >
            <StarIcon
              className={cn(
                'size-6 transition-colors duration-150',
                value !== null && star <= value ? 'fill-warning text-warning' : 'text-muted-foreground/40',
              )}
            />
          </button>
        ))}
      </div>
    </div>
  )
}
