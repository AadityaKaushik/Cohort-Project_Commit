import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { format } from 'date-fns'
import { CalendarClockIcon, ClockIcon, MessagesSquareIcon, NotebookPenIcon, PlusIcon, TrophyIcon, type LucideIcon } from 'lucide-react'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import { Button } from '@/components/ui/button'
import { CalendarDownloadButton } from '@/components/shared/CalendarDownloadButton'
import { downloadInterviewCalendar, downloadUpcomingCalendar } from '@/lib/calendar-api'
import { DataTable, type DataTableColumn } from '@/components/shared/DataTable'
import { usePendingDeletes } from '@/hooks/use-undo-delete'
import { EmptyState } from '@/components/shared/EmptyState'
import { CompanyCell } from '@/components/shared/CompanyMark'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, LoadingRows } from '@/components/shared/QueryStates'
import { StatGrid, StatTile } from '@/components/shared/StatTile'
import { OutcomeBadge, ToneBadge } from '@/components/shared/ToneBadge'

const ROUND_TYPE_LABELS: Record<InterviewSummary['round_type'], string> = {
  OA: 'OA',
  PHONE_SCREEN: 'Phone Screen',
  TECHNICAL: 'Technical',
  SYSTEM_DESIGN: 'System Design',
  BEHAVIOURAL: 'Behavioural',
  HR: 'HR',
  FINAL: 'Final',
}

function interviewsQuery(upcoming: boolean) {
  return {
    queryKey: ['interviews', { upcoming }],
    queryFn: () => listInterviews({ upcoming, size: 50, sort: 'scheduled_at', direction: upcoming ? 'asc' : 'desc' }),
  } as const
}

function buildColumns(navigate: (path: string) => void): DataTableColumn<InterviewSummary>[] {
  return [
    { key: 'company', header: 'Company', render: (row) => <CompanyCell name={row.company_name} detail={row.job_title} /> },
    {
      key: 'round',
      header: 'Round',
      render: (row) => (
        <span>
          <span className="text-muted-foreground tabular-nums">#{row.round_number}</span> {ROUND_TYPE_LABELS[row.round_type]}
        </span>
      ),
    },
    {
      key: 'scheduled_at',
      header: 'Scheduled',
      render: (row) =>
        row.scheduled_at ? (
          <span className="tabular-nums">{format(new Date(row.scheduled_at), 'EEE d MMM, p')}</span>
        ) : (
          <span className="text-muted-foreground">Not scheduled</span>
        ),
    },
    { key: 'outcome', header: 'Outcome', render: (row) => <OutcomeBadge outcome={row.outcome} /> },
    {
      key: 'calendar',
      header: 'Calendar',
      render: (row) =>
        row.scheduled_at && new Date(row.scheduled_at) > new Date() ? (
          <CalendarDownloadButton
            label="Add to calendar"
            variant="link"
            size="sm"
            className="h-auto p-0"
            stopPropagation
            download={() => downloadInterviewCalendar(row.id)}
          />
        ) : null,
    },
    {
      key: 'debrief',
      header: 'Debrief',
      render: (row) =>
        row.has_feedback ? (
          <ToneBadge tone="success" dot>
            Done
          </ToneBadge>
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
  description,
  icon,
  emptyTitle,
  emptyDescription,
  emptyActionLabel,
  upcoming,
}: {
  title: string
  description: string
  icon: LucideIcon
  emptyTitle: string
  emptyDescription: string
  emptyActionLabel: string
  upcoming: boolean
}) {
  const navigate = useNavigate()
  const { data: loaded, isLoading, isError, refetch } = useQuery(interviewsQuery(upcoming))
  const pendingDeletes = usePendingDeletes()
  const data = loaded && { ...loaded, items: loaded.items.filter((row) => !pendingDeletes.has(row.id)) }

  return (
    <section aria-label={title} className="space-y-3">
      <div className="flex items-baseline justify-between gap-3">
        <h3 className="font-heading text-base font-semibold tracking-tight">
          {title}
          {data && data.total > 0 && <span className="ml-2 text-sm font-normal text-muted-foreground tabular-nums">{data.total}</span>}
        </h3>
        <p className="hidden text-xs text-muted-foreground sm:block">{description}</p>
      </div>
      {isLoading && <LoadingRows rows={3} label={`Loading ${title.toLowerCase()} interviews`} />}
      {isError && <ErrorState title={`Couldn't load ${title.toLowerCase()} interviews`} onRetry={() => refetch()} />}
      {data && data.items.length === 0 && (
        <EmptyState
          compact
          icon={icon}
          title={emptyTitle}
          description={emptyDescription}
          action={
            <Button asChild>
              <Link to="/interviews/new">
                <PlusIcon aria-hidden />
                {emptyActionLabel}
              </Link>
            </Button>
          }
        />
      )}
      {data && data.items.length > 0 && (
        <DataTable
          columns={buildColumns(navigate)}
          data={data.items}
          getRowId={(row) => row.id}
          rowLabel={(row) => `${row.company_name}, round ${row.round_number}`}
          onRowClick={(row) => navigate(`/interviews/${row.id}`)}
        />
      )}
    </section>
  )
}

export function InterviewsListPage() {
  const upcomingQuery = useQuery(interviewsQuery(true))
  const pastQuery = useQuery(interviewsQuery(false))
  const debriefsDue = pastQuery.data?.items.filter((interview) => !interview.has_feedback).length ?? null
  const passed = pastQuery.data?.items.filter((interview) => interview.outcome === 'passed').length ?? null

  return (
    <div className="space-y-6">
      <PageHeader
        title="Interviews"
        description="Every round you have scheduled or sat, with a debrief while it is still fresh."
        actions={
          <>
            <CalendarDownloadButton label="Download all upcoming" download={downloadUpcomingCalendar} />
            <Button asChild>
              <Link to="/interviews/new">
                <PlusIcon aria-hidden />
                New interview
              </Link>
            </Button>
          </>
        }
      />

      <StatGrid>
        <StatTile label="Upcoming" icon={CalendarClockIcon} tone="info" value={upcomingQuery.data?.total ?? null} loading={upcomingQuery.isLoading} hint="Scheduled from today on" />
        <StatTile label="Completed" icon={MessagesSquareIcon} value={pastQuery.data?.total ?? null} loading={pastQuery.isLoading} hint="Rounds already sat" />
        <StatTile label="Passed" icon={TrophyIcon} tone="success" value={passed} loading={pastQuery.isLoading} hint="Moved on to the next round" />
        <StatTile
          label="Debriefs due"
          icon={NotebookPenIcon}
          tone="warning"
          value={debriefsDue}
          loading={pastQuery.isLoading}
          hint={debriefsDue ? 'Capture them while you remember' : 'All caught up'}
        />
      </StatGrid>

      <InterviewSection
        title="Upcoming"
        description="Soonest first"
        icon={ClockIcon}
        emptyTitle="No upcoming interviews"
        emptyDescription="Interviews you schedule for a future date will show up here."
        emptyActionLabel="Schedule an interview"
        upcoming={true}
      />

      <InterviewSection
        title="Past"
        description="Most recent first"
        icon={MessagesSquareIcon}
        emptyTitle="No past interviews yet"
        emptyDescription="Once an interview's date has passed, it moves here — with a prompt to debrief if you haven't yet."
        emptyActionLabel="Log an interview"
        upcoming={false}
      />
    </div>
  )
}
