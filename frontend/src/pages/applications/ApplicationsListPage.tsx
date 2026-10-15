import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { BriefcaseIcon, KanbanIcon, PlusIcon, SearchXIcon, UploadIcon } from 'lucide-react'
import { usePendingDeletes } from '@/hooks/use-undo-delete'
import { formatLabel } from '@/lib/labels'
import { listApplications, listStalledApplications, type ApplicationSummary } from '@/lib/applications-api'
import { readPreference, writePreference } from '@/lib/preferences'
import { exactDate, relativeDate } from '@/lib/relative-date'
import { QuickStatusMenu } from '@/components/applications/QuickStatusMenu'
import { ToneBadge } from '@/components/shared/ToneBadge'
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
import { PageGuide } from '@/components/onboarding/PageGuide'
import { PriorityBadge } from '@/components/shared/ToneBadge'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'

const PAGE_SIZE = 25

type SortKey = 'date_applied' | 'status' | 'priority' | 'days_in_stage'

interface ListPreferences {
  sort: string
  direction: string
  filters: ApplicationFilters
}

const VIEW_KEY = 'applications-view'
const LIST_KEY = 'applications-list'

export function ApplicationsListPage() {
  const navigate = useNavigate()
  const pendingDeletes = usePendingDeletes()
  const [searchParams, setSearchParams] = useSearchParams()
  const [remembered] = useState(() => ({
    view: readPreference<string>(VIEW_KEY),
    list: readPreference<ListPreferences>(LIST_KEY),
  }))
  const stalledQuery = useQuery({ queryKey: ['applications', 'stalled'], queryFn: listStalledApplications })
  const stalledIds = new Set(stalledQuery.data?.map((item) => item.application_id))

  const page = Number(searchParams.get('page') ?? '0')
  const sort = (searchParams.get('sort') as SortKey | null) ?? 'date_applied'
  const direction = (searchParams.get('direction') as 'asc' | 'desc' | null) ?? 'desc'

  const filters = useMemo<ApplicationFilters>(() => ({
    q: searchParams.get('q') ?? '',
    status: (searchParams.get('status') as ApplicationFilters['status'] | null) ?? '',
    priority: (searchParams.get('priority') as ApplicationFilters['priority'] | null) ?? '',
    work_mode: (searchParams.get('work_mode') as ApplicationFilters['work_mode'] | null) ?? '',
    date_from: searchParams.get('date_from') ?? '',
    date_to: searchParams.get('date_to') ?? '',
  }), [searchParams])

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

  const wantsBoard = remembered.view === 'board' && !searchParams.has('view')

  useEffect(() => {
    const onlyView = [...searchParams.keys()].every((key) => key === 'view')
    if (remembered.list && onlyView) {
      const next = new URLSearchParams(searchParams)
      next.set('sort', remembered.list.sort)
      next.set('direction', remembered.list.direction)
      for (const [key, value] of Object.entries(remembered.list.filters)) {
        if (value) next.set(key, value as string)
      }
      setSearchParams(next, { replace: true })
    }
    // restore once, on arrival
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (!wantsBoard) {
      writePreference(VIEW_KEY, 'list')
      writePreference(LIST_KEY, { sort, direction, filters })
    }
  }, [wantsBoard, sort, direction, filters])

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
    {
      key: 'company',
      header: 'Company',
      render: (row) => (
        <span className="flex flex-wrap items-center gap-2">
          <CompanyCell name={row.company.name} />
          {stalledIds.has(row.id) && <ToneBadge tone="warning">Needs follow-up</ToneBadge>}
        </span>
      ),
    },
    { key: 'job_title', header: 'Role', render: (row) => row.job_title },
    {
      key: 'status',
      header: 'Status',
      sortKey: 'status',
      render: (row) => <StatusBadge status={row.status} />,
    },
    { key: 'quick_status', header: 'Change status', render: (row) => <QuickStatusMenu application={row} /> },
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
      render: (row) => (
        <time dateTime={row.date_applied} title={exactDate(row.date_applied)} className="text-muted-foreground">
          Applied {relativeDate(row.date_applied)}
        </time>
      ),
    },
    {
      key: 'priority',
      header: 'Priority',
      sortKey: 'priority',
      render: (row) => <PriorityBadge priority={row.priority} />,
    },
  ]

  const items = data?.items.filter((row) => !pendingDeletes.has(row.id)) ?? []
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

  if (wantsBoard) {
    return <Navigate to="/applications/board" replace />
  }

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
          secondaryAction={
            <Button variant="outline" asChild>
              <Link to="/applications/import">Import a spreadsheet</Link>
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
            <PageGuide guideId="applications" />
            <span data-guide="view-switch" className="contents">
            <Button variant="outline" asChild>
              <Link to="/applications/board" onClick={() => writePreference(VIEW_KEY, 'board')}>
                <KanbanIcon aria-hidden />
                Board
              </Link>
            </Button>
            </span>
            <span data-guide="import" className="contents">
            <Button variant="outline" asChild>
              <Link to="/applications/import">
                <UploadIcon aria-hidden />
                Import CSV
              </Link>
            </Button>
            </span>
            {newApplication}
          </>
        }
      />

      <div data-guide="filters">
        <ApplicationFilterBar filters={filters} onChange={handleFiltersChange} />
      </div>

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

      {data && items.length > 0 && (
        <>
          <div data-guide="list">
          <DataTable
            columns={columns}
            data={items}
            getRowId={(row) => row.id}
            rowLabel={(row) => `${row.company.name}, ${row.job_title}`}
            onRowClick={(row) => navigate(`/applications/${row.id}`)}
            sort={sort}
            direction={direction}
            onSortChange={handleSortChange}
          />
          </div>
          <Pagination page={page} totalPages={totalPages} onPageChange={(next) => updateParams({ page: String(next) })} label="Application pages" />
        </>
      )}
    </div>
  )
}
