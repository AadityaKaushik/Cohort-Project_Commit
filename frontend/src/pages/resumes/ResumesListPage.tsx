import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertCircleIcon, FileTextIcon, PlusIcon } from 'lucide-react'
import { listResumes, type ResumeSummary } from '@/lib/resumes-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DataTable, type DataTableColumn } from '@/components/shared/DataTable'
import { EmptyState } from '@/components/shared/EmptyState'

const PAGE_SIZE = 25

type SortKey = 'created_at' | 'name'

export function ResumesListPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()

  const page = Number(searchParams.get('page') ?? '0')
  const sort = (searchParams.get('sort') as SortKey | null) ?? 'created_at'
  const direction = (searchParams.get('direction') as 'asc' | 'desc' | null) ?? 'desc'

  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['resumes', page, sort, direction],
    queryFn: () => listResumes({ page, size: PAGE_SIZE, sort, direction }),
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

  const columns: DataTableColumn<ResumeSummary>[] = [
    {
      key: 'name',
      header: 'Name',
      sortKey: 'name',
      render: (row) => (
        <span className="inline-flex items-center gap-2">
          {row.name}
          {row.is_default && <Badge variant="outline">Default</Badge>}
        </span>
      ),
    },
    { key: 'version_label', header: 'Version', render: (row) => row.version_label ?? '—' },
    { key: 'target_role', header: 'Target role', render: (row) => row.target_role ?? '—' },
    {
      key: 'has_file',
      header: 'File',
      render: (row) => (row.has_file ? <Badge>Uploaded</Badge> : <span className="text-muted-foreground">None yet</span>),
    },
    {
      key: 'created_at',
      header: 'Created',
      sortKey: 'created_at',
      render: (row) => format(new Date(row.created_at), 'MMM d, yyyy'),
    },
  ]

  if (isLoading) {
    return (
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Resumes</h2>
          <Skeleton className="h-9 w-40" />
        </div>
        <div className="space-y-2">
          {Array.from({ length: 4 }).map((_, index) => (
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
        <AlertTitle>Couldn&apos;t load your resumes</AlertTitle>
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
        icon={FileTextIcon}
        title="No resumes yet"
        description="Keep multiple tailored versions and attach the right one to each application."
        action={
          <Button asChild>
            <Link to="/resumes/new">
              <PlusIcon className="h-4 w-4" />
              Create your first resume
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
          <h2 className="text-lg font-semibold">Resumes</h2>
          <p className="text-sm text-muted-foreground">{data.total} total</p>
        </div>
        <Button asChild>
          <Link to="/resumes/new">
            <PlusIcon className="h-4 w-4" />
            New resume
          </Link>
        </Button>
      </div>

      <DataTable
        columns={columns}
        data={data.items}
        getRowId={(row) => row.id}
        onRowClick={(row) => navigate(`/resumes/${row.id}`)}
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
