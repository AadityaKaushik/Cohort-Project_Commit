import { useId, type ReactNode } from 'react'
import { cn } from '@/lib/utils'

interface SectionCardProps {
  title?: ReactNode
  description?: ReactNode
  action?: ReactNode
  tone?: 'default' | 'danger'
  className?: string
  children?: ReactNode
}

/** The one card treatment for page sections: raised surface, heading row with an optional action, and a padded body. */
export function SectionCard({ title, description, action, tone = 'default', className, children }: SectionCardProps) {
  const headingId = useId()
  return (
    <section
      aria-labelledby={title ? headingId : undefined}
      className={cn(
        'surface-raised min-w-0 overflow-hidden rounded-xl border',
        tone === 'danger' && 'border-destructive/40',
        className,
      )}
    >
      {(title || action) && (
        <div className={cn('flex flex-wrap items-start justify-between gap-x-4 gap-y-2 px-4 pt-4 sm:px-5 sm:pt-5', !children && 'pb-4 sm:pb-5')}>
          <div className="min-w-0 space-y-1">
            {title && (
              <h3 id={headingId} className={cn('font-heading text-base font-semibold tracking-tight', tone === 'danger' && 'text-destructive')}>
                {title}
              </h3>
            )}
            {description && <p className="text-sm text-pretty text-muted-foreground">{description}</p>}
          </div>
          {action && <div className="flex shrink-0 flex-wrap items-center gap-2">{action}</div>}
        </div>
      )}
      {children && (
        <div className={cn('p-4 sm:p-5', (title || action) && 'pt-4 sm:pt-4')}>{children}</div>
      )}
    </section>
  )
}
