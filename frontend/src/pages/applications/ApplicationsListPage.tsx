import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertCircleIcon, BriefcaseIcon, PlusIcon } from 'lucide-react'
import { listApplications, type ApplicationSummary } from '@/lib/applications-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DataTable, type DataTableColumn } from '@/components/shared/DataTable'
import { EmptyState } from '@/components/shared/EmptyState'
import { StatusBadge } from '@/components/shared/StatusBadge'

const PAGE_SIZE = 25

const STAGE_LABELS: Record<string, string> = {
  SAVED: 'Saved',
  APPLIED: 'Applied',
  OA: 'OA',
  INTERVIEW: 'Interview',
  FINAL_ROUND: 'Final Round',
  OFFER: 'Offer',
}

type SortKey = 'date_applied' | 'status' | 'priority' | 'days_in_stage'

export function ApplicationsListPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') ?? '0')
  const sort = (searchParams.get('sort') as SortKey | null) ?? 'date_applied'
  const direction = (searchParams.get('direction') as 'asc' | 'desc' | null) ?? 'desc'

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['applications', page, sort, direction],
    queryFn: () => listApplications({ page, size: PAGE_SIZE, sort, direction }),
  })

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
      render: (row) => STAGE_LABELS[row.current_stage] ?? row.current_stage,
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
      render: (row) => row.priority ?? '—',
    },
  ]

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Applications</h2>
          <Skeleton className="h-9 w-40" />
        </div>
        <div className="space-y-2">
          {Array.from({ length: 6 }).map((_, index) => (
            <Skeleton key={index} className="h-10 w-full" />
          ))}
        </div>
      </div>
    )
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load your applications</AlertTitle>
        <AlertDescription className="flex items-center justify-between gap-4">
          <span>Something went wrong talking to the server.</span>
          <Button variant="outline" size="sm" onClick={() => refetch()}>
            Try again
          </Button>
        </AlertDescription>
      </Alert>
    )
  }

  if (!data || data.items.length === 0) {
    return (
      <EmptyState
        icon={BriefcaseIcon}
        title="No applications yet"
        description="Track every job you apply to in one place — status, timeline, and all."
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

  const totalPages = Math.max(1, Math.ceil(data.total / data.size))

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold">Applications</h2>
          <p className="text-sm text-muted-foreground">{data.total} total</p>
        </div>
        <Button asChild>
          <Link to="/applications/new">
            <PlusIcon className="h-4 w-4" />
            New application
          </Link>
        </Button>
      </div>

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
          <Button
            variant="outline"
            size="sm"
            disabled={page === 0}
            onClick={() => updateParams({ page: String(page - 1) })}
          >
            Previous
          </Button>
          <Button
            variant="outline"
            size="sm"
            disabled={page + 1 >= totalPages}
            onClick={() => updateParams({ page: String(page + 1) })}
          >
            Next
          </Button>
        </div>
      </div>
    </div>
  )
}
