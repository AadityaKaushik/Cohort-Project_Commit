import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useCallback } from 'react'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { BriefcaseIcon, KanbanIcon, PlusIcon, SearchXIcon } from 'lucide-react'
import { formatLabel } from '@/lib/labels'
import { listApplications, type ApplicationSummary } from '@/lib/applications-api'
import { Button } from '@/components/ui/button'
import {
  ApplicationFilterBar,
  NO_FILTERS,
  hasActiveFilters,
  type ApplicationFilters,
} from '@/components/applications/ApplicationFilterBar'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { DataTable, type DataTableColumn } from '@/components/shared/DataTable'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'

const PAGE_SIZE = 25

type SortKey = 'date_applied' | 'status' | 'priority' | 'days_in_stage'

export function ApplicationsListPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') ?? '0')
  const sort = (searchParams.get('sort') as SortKey | null) ?? 'date_applied'
  const direction = (searchParams.get('direction') as 'asc' | 'desc' | null) ?? 'desc'

  const filters: ApplicationFilters = {
    q: searchParams.get('q') ?? '',
    status: (searchParams.get('status') as ApplicationFilters['status'] | null) ?? '',
    priority: (searchParams.get('priority') as ApplicationFilters['priority'] | null) ?? '',
    work_mode: (searchParams.get('work_mode') as ApplicationFilters['work_mode'] | null) ?? '',
    date_from: searchParams.get('date_from') ?? '',
    date_to: searchParams.get('date_to') ?? '',
  }

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['applications', page, sort, direction, filters],
    queryFn: () =>
      listApplications({
        page,
        size: PAGE_SIZE,
        sort,
        direction,
        q: filters.q || undefined,
        status: filters.status || undefined,
        priority: filters.priority || undefined,
        work_mode: filters.work_mode || undefined,
        date_from: filters.date_from || undefined,
        date_to: filters.date_to || undefined,
      }),
  })

  const handleFiltersChange = useCallback(
    (next: ApplicationFilters) => {
      const params = new URLSearchParams(searchParams)
      for (const [key, value] of Object.entries(next)) {
        if (value) {
          params.set(key, value)
        } else {
          params.delete(key)
        }
      }
      params.set('page', '0')
      setSearchParams(params)
    },
    [searchParams, setSearchParams],
  )

  function updateParams(next: Record<string, string>) {
    const params = new URLSearchParams(searchParams)
    for (const [key, value] of Object.entries(next)) {
      params.set(key, value)
    }
    setSearchParams(params)
  }

  function handleSortChange(sortKey: string) {
    if (sortKey === sort) {
      updateParams({ direction: direction === 'asc' ? 'desc' : 'asc', page: '0' })
    } else {
      updateParams({ sort: sortKey, direction: 'desc', page: '0' })
    }
  }

  const columns: DataTableColumn<ApplicationSummary>[] = [
    { key: 'company', header: 'Company', render: (row) => row.company.name },
    { key: 'job_title', header: 'Role', render: (row) => row.job_title },
    {
      key: 'status',
      header: 'Status',
      sortKey: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: 'current_stage',
      header: 'Stage',
      render: (row) => formatLabel(row.current_stage),
    },
    {
      key: 'days_in_stage',
      header: 'Days in stage',
      sortKey: 'days_in_stage',
      render: (row) => row.days_in_stage,
    },
    {
      key: 'date_applied',
      header: 'Date applied',
      sortKey: 'date_applied',
      render: (row) => format(new Date(row.date_applied), 'MMM d, yyyy'),
    },
    {
      key: 'priority',
      header: 'Priority',
      sortKey: 'priority',
      render: (row) => formatLabel(row.priority),
    },
  ]

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1
  const filtered = hasActiveFilters(filters)
  const isEmpty = !isLoading && !isError && data !== undefined && data.items.length === 0

  if (isEmpty && !filtered) {
    return (
      <EmptyState
        icon={BriefcaseIcon}
        title="No applications yet"
        description="Track every job you apply to in one place: status, timeline, and all."
        action={
          <Button asChild>
            <Link to="/applications/new">
              <PlusIcon className="h-4 w-4" />
              Create your first application
            </Link>
          </Button>
        }
      />
    )
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold">Applications</h2>
          <p className="text-sm text-muted-foreground">
            {data ? `${data.total} ${filtered ? 'matching' : 'total'}` : 'Loading'}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" asChild>
            <Link to="/applications/board">
              <KanbanIcon className="h-4 w-4" />
              Board
            </Link>
          </Button>
          <Button asChild>
            <Link to="/applications/new">
              <PlusIcon className="h-4 w-4" />
              New application
            </Link>
          </Button>
        </div>
      </div>

      <ApplicationFilterBar filters={filters} onChange={handleFiltersChange} />

      {isLoading && <LoadingRows label="Loading applications" />}
      {isError && <ErrorState title="Couldn't load your applications" onRetry={() => refetch()} />}
      {isEmpty && (
        <EmptyState
          icon={SearchXIcon}
          title="No applications match these filters"
          description="Try a different search or clear the filters to see everything."
          action={
            <Button variant="outline" onClick={() => handleFiltersChange(NO_FILTERS)}>
              Clear filters
            </Button>
          }
        />
      )}

      {data && data.items.length > 0 && (
        <>
          <DataTable
            columns={columns}
            data={data.items}
            getRowId={(row) => row.id}
            onRowClick={(row) => navigate(`/applications/${row.id}`)}
            sort={sort}
            direction={direction}
            onSortChange={handleSortChange}
          />

          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Page {page + 1} of {totalPages}
            </p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => updateParams({ page: String(page - 1) })}>
                Previous
              </Button>
              <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => updateParams({ page: String(page + 1) })}>
                Next
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
