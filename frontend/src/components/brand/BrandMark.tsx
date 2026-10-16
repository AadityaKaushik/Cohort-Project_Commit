import { cn } from '@/lib/utils'

interface GlyphProps {
  className?: string
}

export function BrandGlyph({ className }: GlyphProps) {
  return (
    <span
      aria-hidden
      className={cn(
        'inline-flex size-8 shrink-0 items-center justify-center rounded-[3px] bg-signal text-signal-foreground',
        className,
      )}
    >
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        className="size-[64%]"
      >
        <path d="M18.4 6.6A8.5 8.5 0 1 0 18.4 17.4" strokeWidth="2.2" />
        <path d="M15.2 9.3A4.3 4.3 0 1 0 15.2 14.7" strokeWidth="2.2" />
        <circle cx="12" cy="12" r="1.5" fill="currentColor" stroke="none" />
      </svg>
    </span>
  )
}

interface BrandMarkProps extends GlyphProps {
  glyphClassName?: string
  wordmarkClassName?: string
}

export function BrandMark({ className, glyphClassName, wordmarkClassName }: BrandMarkProps) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <BrandGlyph className={glyphClassName} />
      <span className={cn('text-display text-[22px] tracking-[0.01em]', wordmarkClassName)}>Cohort</span>
    </span>
  )
}
