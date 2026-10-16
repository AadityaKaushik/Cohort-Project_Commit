import { cn } from '@/lib/utils'

function initialOf(name: string): string {
  return name.replace(/^\[[^\]]*\]\s*/, '').match(/[\p{L}\p{N}]/u)?.[0]?.toUpperCase() ?? '?'
}

export function CompanyMark({ name, className }: { name: string; className?: string }) {
  return (
    <span
      aria-hidden
      className={cn(
        'text-display flex size-8 shrink-0 items-center justify-center rounded-[3px] bg-foreground text-base text-background',
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
