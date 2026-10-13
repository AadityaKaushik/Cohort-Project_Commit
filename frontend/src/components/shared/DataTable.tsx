import type { KeyboardEvent, ReactNode } from 'react'
import { ArrowDownIcon, ArrowUpIcon, ArrowUpDownIcon, ChevronRightIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { useIsMobile } from '@/hooks/use-mobile'
import { Button } from '@/components/ui/button'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

export interface DataTableColumn<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** Present only for columns the caller can sort server-side by. */
  sortKey?: string
  className?: string
  align?: 'start' | 'end'
}

interface DataTableProps<T> {
  columns: DataTableColumn<T>[]
  data: T[]
  getRowId: (row: T) => string
  onRowClick?: (row: T) => void
  rowLabel?: (row: T) => string
  sort?: string
  direction?: 'asc' | 'desc'
  onSortChange?: (sortKey: string) => void
}

function activateOnKey<T>(event: KeyboardEvent, row: T, onRowClick?: (row: T) => void) {
  if (onRowClick && (event.key === 'Enter' || event.key === ' ') && event.target === event.currentTarget) {
    event.preventDefault()
    onRowClick(row)
  }
}

/**
 * A dense, sortable data table — the PROJECT_PLAN.md §2.3 anti-similarity
 * checklist explicitly prefers this over a Trello-style board as the
 * primary application view. On a phone each row becomes a card.
 */
export function DataTable<T>({ columns, data, getRowId, onRowClick, rowLabel, sort, direction, onSortChange }: DataTableProps<T>) {
  const isMobile = useIsMobile()

  if (isMobile) {
    return (
      <ul className="stagger space-y-2" data-testid="data-cards">
        {data.map((row) => (
          <li
            key={getRowId(row)}
            tabIndex={onRowClick ? 0 : undefined}
            aria-label={onRowClick && rowLabel ? rowLabel(row) : undefined}
            className={cn(
              'surface-raised rounded-xl border p-4 text-sm',
              onRowClick && 'lift cursor-pointer hover:border-primary/30 focus-visible:outline-2 focus-visible:outline-ring',
            )}
            onClick={() => onRowClick?.(row)}
            onKeyDown={(event) => activateOnKey(event, row, onRowClick)}
          >
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0 font-medium">{columns[0].render(row)}</div>
              {onRowClick && <ChevronRightIcon aria-hidden className="mt-0.5 size-4 shrink-0 text-muted-foreground" />}
            </div>
            <dl className="mt-3 grid grid-cols-2 gap-x-4 gap-y-3 border-t pt-3">
              {columns.slice(1).map((column) => (
                <div key={column.key} className="min-w-0">
                  <dt className="eyebrow">{column.header}</dt>
                  <dd className="mt-0.5 truncate">{column.render(row)}</dd>
                </div>
              ))}
            </dl>
          </li>
        ))}
      </ul>
    )
  }

  return (
    <div className="surface-raised overflow-hidden rounded-xl border">
      <Table>
        <TableHeader className="bg-surface-2/70">
          <TableRow className="hover:bg-transparent">
            {columns.map((column) => {
              const isSorted = Boolean(column.sortKey && column.sortKey === sort)
              const canSort = Boolean(column.sortKey && onSortChange)
              return (
                <TableHead
                  key={column.key}
                  aria-sort={isSorted ? (direction === 'asc' ? 'ascending' : 'descending') : undefined}
                  className={cn('eyebrow h-10 px-4 text-muted-foreground first:pl-5 last:pr-5', column.align === 'end' && 'text-right', column.className)}
                >
                  {canSort ? (
                    <button
                      type="button"
                      onClick={() => column.sortKey && onSortChange?.(column.sortKey)}
                      className={cn(
                        '-mx-1.5 inline-flex items-center gap-1 rounded-md px-1.5 py-1 uppercase transition-colors hover:bg-muted hover:text-foreground',
                        isSorted && 'text-foreground',
                      )}
                    >
                      {column.header}
                      {isSorted ? (
                        direction === 'asc' ? (
                          <ArrowUpIcon aria-hidden className="size-3.5 text-primary" />
                        ) : (
                          <ArrowDownIcon aria-hidden className="size-3.5 text-primary" />
                        )
                      ) : (
                        <ArrowUpDownIcon aria-hidden className="size-3.5 opacity-50" />
                      )}
                    </button>
                  ) : (
                    column.header
                  )}
                </TableHead>
              )
            })}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((row, index) => (
            <TableRow
              key={getRowId(row)}
              tabIndex={onRowClick ? 0 : undefined}
              aria-label={onRowClick && rowLabel ? rowLabel(row) : undefined}
              className={cn('animate-enter hover:bg-transparent', onRowClick && 'row-interactive cursor-pointer')}
              style={{ animationDelay: `${Math.min(index, 12) * 25}ms` }}
              onClick={() => onRowClick?.(row)}
              onKeyDown={(event) => activateOnKey(event, row, onRowClick)}
            >
              {columns.map((column, columnIndex) => (
                <TableCell
                  key={column.key}
                  className={cn(
                    'h-12 px-4 first:pl-5 last:pr-5',
                    columnIndex === 0 && 'font-medium',
                    column.align === 'end' && 'text-right tabular-nums',
                    column.className,
                  )}
                >
                  {column.render(row)}
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  label?: string
}

export function Pagination({ page, totalPages, onPageChange, label = 'Pagination' }: PaginationProps) {
  return (
    <nav aria-label={label} className="flex items-center justify-between gap-3">
      <p className="text-sm text-muted-foreground tabular-nums">
        Page {page + 1} of {totalPages}
      </p>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          Previous
        </Button>
        <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => onPageChange(page + 1)}>
          Next
        </Button>
      </div>
    </nav>
  )
}
