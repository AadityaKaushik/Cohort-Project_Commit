import { cn } from '@/lib/utils'

function initialOf(name: string): string {
  return name.replace(/^\[[^\]]*\]\s*/, '').match(/[\p{L}\p{N}]/u)?.[0]?.toUpperCase() ?? '?'
}

/** A company's initial in a soft tile, so rows and cards scan by shape before text. */
export function CompanyMark({ name, className }: { name: string; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        'flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10 font-heading text-sm font-semibold text-primary ring-1 ring-primary/15 ring-inset',
        className,
      )}
    >
      {initialOf(name)}
    </span>
  )
}

export function CompanyCell({ name, detail }: { name: string; detail?: string }) {
  return (
    <span className="flex min-w-0 items-center gap-3">
      <CompanyMark name={name} />
      <span className="min-w-0">
        <span className="block truncate font-medium">{name}</span>
        {detail && <span className="block truncate text-xs font-normal text-muted-foreground">{detail}</span>}
      </span>
    </span>
  )
}
