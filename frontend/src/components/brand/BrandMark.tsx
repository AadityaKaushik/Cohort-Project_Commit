import { cn } from '@/lib/utils'

interface GlyphProps {
  className?: string
}

export function BrandGlyph({ className }: GlyphProps) {
  return (
    <span
      aria-hidden
      className={cn(
        'inline-flex size-8 shrink-0 items-center justify-center rounded-sm bg-signal text-signal-foreground',
        className,
      )}
    >
      <svg viewBox="0 0 24 24" className="size-[68%]" fill="currentColor">
        <path d="M3 21v-5h6v-5h6V5h6v16z" />
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
