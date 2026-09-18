import type { ReactNode } from 'react'
import { ArrowDownIcon, ArrowUpIcon, ArrowUpDownIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'

export interface DataTableColumn<T> {
  key: string
  header: string
  render: (row: T) => ReactNode
  /** Present only for columns the caller can sort server-side by. */
  sortKey?: string
  className?: string
}

interface DataTableProps<T> {
  columns: DataTableColumn<T>[]
  data: T[]
  getRowId: (row: T) => string
  onRowClick?: (row: T) => void
  sort?: string
  direction?: 'asc' | 'desc'
  onSortChange?: (sortKey: string) => void
}

/**
 * A dense, sortable data table — the PROJECT_PLAN.md §2.3 anti-similarity
 * checklist explicitly prefers this over a Trello-style board as the
 * primary application view.
 */
export function DataTable<T>({
  columns,
  data,
  getRowId,
  onRowClick,
  sort,
  direction,
  onSortChange,
}: DataTableProps<T>) {
  return (
    <div className="overflow-hidden rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            {columns.map((column) => {
              const isSorted = column.sortKey && column.sortKey === sort
              const canSort = Boolean(column.sortKey && onSortChange)
              return (
                <TableHead
                  key={column.key}
                  className={cn(canSort && 'cursor-pointer select-none', column.className)}
                  onClick={() => {
                    if (canSort && column.sortKey) {
                      onSortChange?.(column.sortKey)
                    }
                  }}
                >
                  <span className="inline-flex items-center gap-1">
                    {column.header}
                    {canSort &&
                      (isSorted ? (
                        direction === 'asc' ? (
                          <ArrowUpIcon className="h-3.5 w-3.5" />
                        ) : (
                          <ArrowDownIcon className="h-3.5 w-3.5" />
                        )
                      ) : (
                        <ArrowUpDownIcon className="h-3.5 w-3.5 text-muted-foreground/50" />
                      ))}
                  </span>
                </TableHead>
              )
            })}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((row) => (
            <TableRow
              key={getRowId(row)}
              className={cn(onRowClick && 'cursor-pointer')}
              onClick={() => onRowClick?.(row)}
            >
              {columns.map((column) => (
                <TableCell key={column.key} className={column.className}>
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
