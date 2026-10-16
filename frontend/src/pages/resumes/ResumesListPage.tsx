import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { FileTextIcon, PlusIcon } from 'lucide-react'
import { listResumes, type ResumeSummary } from '@/lib/resumes-api'
import { Button } from '@/components/ui/button'
import { DataTable, Pagination, type DataTableColumn } from '@/components/shared/DataTable'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { ToneBadge } from '@/components/shared/ToneBadge'
import { usePendingDeletes } from '@/hooks/use-undo-delete'
import { EmptyState } from '@/components/shared/EmptyState'

const PAGE_SIZE = 25

type SortKey = 'created_at' | 'name'

export function ResumesListPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') ?? '0')
  const sort = (searchParams.get('sort') as SortKey | null) ?? 'created_at'
  const direction = (searchParams.get('direction') as 'asc' | 'desc' | null) ?? 'desc'

  const pendingDeletes = usePendingDeletes()
  const { data: loaded, isLoading, isError, refetch } = useQuery({
    queryKey: ['resumes', page, sort, direction],
    queryFn: () => listResumes({ page, size: PAGE_SIZE, sort, direction }),
  })
  const data = loaded && { ...loaded, items: loaded.items.filter((row) => !pendingDeletes.has(row.id)) }

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

  const columns: DataTableColumn<ResumeSummary>[] = [
    {
      key: 'name',
      header: 'Name',
      sortKey: 'name',
      render: (row) => (
        <span className="flex min-w-0 items-center gap-3">
          <span aria-hidden className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-info/10 text-info ring-1 ring-info/15 ring-inset">
            <FileTextIcon className="size-4" />
          </span>
          <span className="truncate">{row.name}</span>
          {row.is_default && <ToneBadge tone="primary">Default</ToneBadge>}
        </span>
      ),
    },
    { key: 'version_label', header: 'Version', render: (row) => row.version_label ?? <span className="text-muted-foreground">-</span> },
    { key: 'target_role', header: 'Target role', render: (row) => row.target_role ?? <span className="text-muted-foreground">-</span> },
    {
      key: 'has_file',
      header: 'File',
      render: (row) => (row.has_file ? <ToneBadge tone="success" dot>Uploaded</ToneBadge> : <span className="text-muted-foreground">None yet</span>),
    },
    {
      key: 'created_at',
      header: 'Created',
      sortKey: 'created_at',
      render: (row) => <span className="text-muted-foreground tabular-nums">{format(new Date(row.created_at), 'MMM d, yyyy')}</span>,
    },
  ]

  const header = (
    <PageHeader
      title="Resumes"
      description={data && data.items.length > 0 ? `${data.total} ${data.total === 1 ? 'version' : 'versions'}` : 'Keep a version for each kind of role.'}
      actions={
        data && data.items.length > 0 ? (
          <Button asChild>
            <Link to="/resumes/new">
              <PlusIcon aria-hidden />
              New resume
            </Link>
          </Button>
        ) : undefined
      }
    />
  )

  if (isLoading) {
    return (
      <div className="space-y-5">
        {header}
        <LoadingRows rows={4} label="Loading resumes" />
      </div>
    )
  }

  if (isError) {
    return (
      <div className="space-y-5">
        {header}
        <ErrorState title="Couldn't load your resumes" onRetry={() => refetch()} />
      </div>
    )
  }

  if (!data || data.items.length === 0) {
    return (
      <div className="space-y-5">
        {header}
        <EmptyState
          icon={FileTextIcon}
          title="No resumes yet"
          description="Keep multiple versions and attach the right one to each application."
          quote={4}
          action={
            <Button asChild>
              <Link to="/resumes/new">
                <PlusIcon aria-hidden />
                Create your first resume
              </Link>
            </Button>
          }
        />
      </div>
    )
  }

  const totalPages = Math.max(1, Math.ceil(data.total / data.size))

  return (
    <div className="space-y-5">
      {header}
      <DataTable
        columns={columns}
        data={data.items}
        getRowId={(row) => row.id}
        rowLabel={(row) => row.name}
        onRowClick={(row) => navigate(`/resumes/${row.id}`)}
        sort={sort}
        direction={direction}
        onSortChange={handleSortChange}
      />
      <Pagination page={page} totalPages={totalPages} onPageChange={(next) => updateParams({ page: String(next) })} label="Resume pages" />
    </div>
  )
}
