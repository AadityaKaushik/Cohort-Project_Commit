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
import { DataTable, Pagination, type DataTableColumn } from '@/components/shared/DataTable'
import { CompanyCell } from '@/components/shared/CompanyMark'
import { PageHeader } from '@/components/shared/PageHeader'
import { PriorityBadge } from '@/components/shared/ToneBadge'
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
    { key: 'company', header: 'Company', render: (row) => <CompanyCell name={row.company.name} /> },
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
      render: (row) => <span className="text-muted-foreground">{formatLabel(row.current_stage)}</span>,
    },
    {
      key: 'days_in_stage',
      header: 'Days in stage',
      sortKey: 'days_in_stage',
      align: 'end',
      render: (row) => row.days_in_stage,
    },
    {
      key: 'date_applied',
      header: 'Date applied',
      sortKey: 'date_applied',
      render: (row) => <span className="text-muted-foreground tabular-nums">{format(new Date(row.date_applied), 'MMM d, yyyy')}</span>,
    },
    {
      key: 'priority',
      header: 'Priority',
      sortKey: 'priority',
      render: (row) => <PriorityBadge priority={row.priority} />,
    },
  ]

  const totalPages = data ? Math.max(1, Math.ceil(data.total / data.size)) : 1
  const filtered = hasActiveFilters(filters)
  const isEmpty = !isLoading && !isError && data !== undefined && data.items.length === 0
  const newApplication = (
    <Button asChild>
      <Link to="/applications/new">
        <PlusIcon aria-hidden />
        New application
      </Link>
    </Button>
  )

  if (isEmpty && !filtered) {
    return (
      <div className="space-y-6">
        <PageHeader title="Applications" description="Every role you are pursuing, with its status, stage and history." />
        <EmptyState
          icon={BriefcaseIcon}
          title="No applications yet"
          description="Track every job you apply to in one place: status, timeline, and all."
          quote={1}
          action={
            <Button asChild>
              <Link to="/applications/new">
                <PlusIcon aria-hidden />
                Create your first application
              </Link>
            </Button>
          }
        />
      </div>
    )
  }

  return (
    <div className="space-y-5">
      <PageHeader
        title="Applications"
        description={data ? `${data.total} ${filtered ? 'matching your filters' : 'tracked in total'}` : 'Every role you are pursuing, in one list.'}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to="/applications/board">
                <KanbanIcon aria-hidden />
                Board
              </Link>
            </Button>
            {newApplication}
          </>
        }
      />

      <ApplicationFilterBar filters={filters} onChange={handleFiltersChange} />

      {isLoading && <LoadingRows label="Loading applications" />}
      {isError && <ErrorState title="Couldn't load your applications" onRetry={() => refetch()} />}
      {isEmpty && (
        <EmptyState
          compact
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
            rowLabel={(row) => `${row.company.name}, ${row.job_title}`}
            onRowClick={(row) => navigate(`/applications/${row.id}`)}
            sort={sort}
            direction={direction}
            onSortChange={handleSortChange}
          />
          <Pagination page={page} totalPages={totalPages} onPageChange={(next) => updateParams({ page: String(next) })} label="Application pages" />
        </>
      )}
    </div>
  )
}
