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
      {label && <p className="text-sm text-muted-foreground">{label}</p>}
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
            className={cn('rounded p-0.5 transition-colors disabled:cursor-not-allowed', !disabled && 'hover:text-primary')}
          >
            <StarIcon
              className={cn(
                'h-5 w-5',
                value !== null && star <= value ? 'fill-primary text-primary' : 'text-muted-foreground/40',
              )}
            />
          </button>
        ))}
      </div>
    </div>
  )
}
