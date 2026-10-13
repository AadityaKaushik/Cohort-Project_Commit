import { useEffect, useState } from 'react'
import { SearchIcon, XIcon } from 'lucide-react'
import type { ApplicationStatusValue, Priority, WorkMode } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'

export interface ApplicationFilters {
  q: string
  status: ApplicationStatusValue | ''
  priority: Priority | ''
  work_mode: WorkMode | ''
  date_from: string
  date_to: string
}

export const NO_FILTERS: ApplicationFilters = { q: '', status: '', priority: '', work_mode: '', date_from: '', date_to: '' }

const STATUSES: ApplicationStatusValue[] = [
  'SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'FINAL_ROUND', 'OFFER', 'ACCEPTED', 'DECLINED', 'REJECTED', 'WITHDRAWN',
]
const PRIORITIES: Priority[] = ['low', 'medium', 'high']
const WORK_MODES: WorkMode[] = ['onsite', 'hybrid', 'remote']

const SEARCH_DEBOUNCE_MS = 300

export function hasActiveFilters(filters: ApplicationFilters): boolean {
  return Object.values(filters).some((value) => value !== '')
}

interface ApplicationFilterBarProps {
  filters: ApplicationFilters
  onChange: (filters: ApplicationFilters) => void
}

export function ApplicationFilterBar({ filters, onChange }: ApplicationFilterBarProps) {
  const [search, setSearch] = useState(filters.q)

  useEffect(() => {
    setSearch(filters.q)
  }, [filters.q])

  useEffect(() => {
    if (search === filters.q) {
      return
    }
    const timer = window.setTimeout(() => onChange({ ...filters, q: search }), SEARCH_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [search, filters, onChange])

  return (
    <form role="search" aria-label="Filter applications" onSubmit={(event) => event.preventDefault()} className="surface-raised grid grid-cols-2 gap-2 rounded-xl border p-2 sm:flex sm:flex-wrap sm:items-center">
      <div className="relative col-span-2 min-w-48 flex-1">
        <label htmlFor="filter-q" className="sr-only">
          Search company or role
        </label>
        <SearchIcon aria-hidden className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
        <Input
          id="filter-q"
          type="search"
          placeholder="Search company or role"
          className="pl-8"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
      </div>
      <div className="min-w-0 sm:w-36">
        <label htmlFor="filter-status" className="sr-only">
          Status
        </label>
        <NativeSelect id="filter-status" value={filters.status} onChange={(e) => onChange({ ...filters, status: e.target.value as ApplicationStatusValue | '' })}>
          <option value="">All statuses</option>
          {STATUSES.map((status) => (
            <option key={status} value={status}>
              {formatLabel(status)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <div className="min-w-0 sm:w-32">
        <label htmlFor="filter-priority" className="sr-only">
          Priority
        </label>
        <NativeSelect id="filter-priority" value={filters.priority} onChange={(e) => onChange({ ...filters, priority: e.target.value as Priority | '' })}>
          <option value="">Any priority</option>
          {PRIORITIES.map((priority) => (
            <option key={priority} value={priority}>
              {formatLabel(priority)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <div className="col-span-2 min-w-0 sm:col-span-1 sm:w-40">
        <label htmlFor="filter-work-mode" className="sr-only">
          Work mode
        </label>
        <NativeSelect id="filter-work-mode" value={filters.work_mode} onChange={(e) => onChange({ ...filters, work_mode: e.target.value as WorkMode | '' })}>
          <option value="">Any work mode</option>
          {WORK_MODES.map((mode) => (
            <option key={mode} value={mode}>
              {formatLabel(mode)}
            </option>
          ))}
        </NativeSelect>
      </div>
      <div className="min-w-0 sm:w-36">
        <label htmlFor="filter-from" className="sr-only">
          Applied from
        </label>
        <Input id="filter-from" type="date" value={filters.date_from} onChange={(e) => onChange({ ...filters, date_from: e.target.value })} />
      </div>
      <div className="min-w-0 sm:w-36">
        <label htmlFor="filter-to" className="sr-only">
          Applied until
        </label>
        <Input id="filter-to" type="date" value={filters.date_to} onChange={(e) => onChange({ ...filters, date_to: e.target.value })} />
      </div>
      {hasActiveFilters(filters) && (
        <Button type="button" variant="ghost" className="col-span-2 sm:col-span-1" onClick={() => onChange(NO_FILTERS)}>
          <XIcon className="h-4 w-4" />
          Clear filters
        </Button>
      )}
    </form>
  )
}
