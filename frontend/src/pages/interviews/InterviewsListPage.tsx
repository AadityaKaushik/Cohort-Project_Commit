import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertCircleIcon, ClockIcon, MessagesSquareIcon, PlusIcon } from 'lucide-react'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { DataTable, type DataTableColumn } from '@/components/shared/DataTable'
import { EmptyState } from '@/components/shared/EmptyState'

const OUTCOME_LABELS: Record<InterviewSummary['outcome'], string> = {
  passed: 'Passed',
  failed: 'Not selected',
  pending: 'Pending',
  unknown: 'Unknown',
}

const ROUND_TYPE_LABELS: Record<InterviewSummary['round_type'], string> = {
  OA: 'OA',
  PHONE_SCREEN: 'Phone Screen',
  TECHNICAL: 'Technical',
  SYSTEM_DESIGN: 'System Design',
  BEHAVIOURAL: 'Behavioural',
  HR: 'HR',
  FINAL: 'Final',
}

function buildColumns(navigate: (path: string) => void): DataTableColumn<InterviewSummary>[] {
  return [
    { key: 'company', header: 'Company', render: (row) => row.company_name },
    { key: 'job_title', header: 'Role', render: (row) => row.job_title },
    { key: 'round', header: 'Round', render: (row) => `#${row.round_number} · ${ROUND_TYPE_LABELS[row.round_type]}` },
    {
      key: 'scheduled_at',
      header: 'Scheduled',
      render: (row) => (row.scheduled_at ? format(new Date(row.scheduled_at), 'MMM d, yyyy p') : 'Not scheduled'),
    },
    {
      key: 'outcome',
      header: 'Outcome',
      render: (row) => <Badge variant="outline">{OUTCOME_LABELS[row.outcome]}</Badge>,
    },
    {
      key: 'debrief',
      header: 'Debrief',
      render: (row) =>
        row.has_feedback ? (
          <Badge>Done</Badge>
        ) : (
          <Button
            variant="link"
            size="sm"
            className="h-auto p-0"
            onClick={(event) => {
              event.stopPropagation()
              navigate(`/interviews/${row.id}/debrief`)
            }}
          >
            Start debrief
          </Button>
        ),
    },
  ]
}

function InterviewSection({
  title,
  icon: Icon,
  emptyTitle,
  emptyDescription,
  upcoming,
}: {
  title: string
  icon: typeof ClockIcon
  emptyTitle: string
  emptyDescription: string
  upcoming: boolean
}) {
  const navigate = useNavigate()
  const { data, isLoading, isError, refetch } = useQuery({
    queryKey: ['interviews', { upcoming }],
    queryFn: () => listInterviews({ upcoming, size: 50, sort: upcoming ? 'scheduled_at' : 'scheduled_at', direction: upcoming ? 'asc' : 'desc' }),
  })

  if (isLoading) {
    return (
      <div className="space-y-2">
        <h3 className="text-sm font-medium">{title}</h3>
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-10 w-full" />
      </div>
    )
  }

  if (isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load {title.toLowerCase()}</AlertTitle>
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
      <div className="space-y-2">
        <h3 className="text-sm font-medium">{title}</h3>
        <EmptyState icon={Icon} title={emptyTitle} description={emptyDescription} />
      </div>
    )
  }

  return (
    <div className="space-y-2">
      <h3 className="text-sm font-medium">
        {title} <span className="font-normal text-muted-foreground">({data.total})</span>
      </h3>
      <DataTable
        columns={buildColumns(navigate)}
        data={data.items}
        getRowId={(row) => row.id}
        onRowClick={(row) => navigate(`/interviews/${row.id}`)}
      />
    </div>
  )
}

export function InterviewsListPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Interviews</h2>
        <Button asChild>
          <Link to="/interviews/new">
            <PlusIcon className="h-4 w-4" />
            New interview
          </Link>
        </Button>
      </div>

      <InterviewSection
        title="Upcoming"
        icon={ClockIcon}
        emptyTitle="No upcoming interviews"
        emptyDescription="Interviews you schedule for a future date will show up here."
        upcoming={true}
      />

      <InterviewSection
        title="Past"
        icon={MessagesSquareIcon}
        emptyTitle="No past interviews yet"
        emptyDescription="Once an interview's date has passed, it moves here — with a prompt to debrief if you haven't yet."
        upcoming={false}
      />
    </div>
  )
}
