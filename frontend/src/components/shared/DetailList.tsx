import type { ReactNode } from 'react'
import { cn } from '@/lib/utils'

export interface DetailItem {
  label: string
  value: ReactNode
  wide?: boolean
}

/** Label-over-value pairs for a record's facts; long-form text spans the full row. */
export function DetailList({ items, className }: { items: DetailItem[]; className?: string }) {
  return (
    <dl className={cn('grid gap-x-6 gap-y-5 sm:grid-cols-2', className)}>
      {items.map((item) => (
        <div key={item.label} className={cn('min-w-0', item.wide && 'sm:col-span-2')}>
          <dt className="eyebrow">{item.label}</dt>
          <dd className={cn('mt-1 text-sm break-words', item.wide ? 'leading-relaxed whitespace-pre-wrap' : 'font-medium')}>
            {item.value}
          </dd>
        </div>
      ))}
    </dl>
  )
}
