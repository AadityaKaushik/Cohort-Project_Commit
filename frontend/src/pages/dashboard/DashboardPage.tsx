import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowRightIcon, PlusIcon } from 'lucide-react'
import { format, isPast, isToday, isTomorrow, parseISO } from 'date-fns'
import { listApplications, listStalledApplications } from '@/lib/applications-api'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import { listPreparationTasks } from '@/lib/prep-api'
import { buildActionQueue } from '@/lib/action-queue'
import { summariseActivity, type ActivitySummary } from '@/lib/activity'
import { formatLabel } from '@/lib/labels'
import { ErrorState } from '@/components/shared/QueryStates'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { ReviewTodayCard } from '@/components/prep/ReviewTodayCard'
import { Ascent, type AscentStage } from '@/components/board/Ascent'
import { FlapText } from '@/components/board/FlapText'
import { PageHeader } from '@/components/shared/PageHeader'
import { STATUS_LINE } from '@/lib/stage-lines'
import type { ApplicationStatusValue, ApplicationSummary } from '@/lib/applications-api'
import { ActionQueueList } from '@/components/dashboard/ActionQueueList'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { cn } from '@/lib/utils'
import { LoadSampleDataButton } from '@/components/onboarding/LoadSampleDataButton'
import { SampleDataBanner } from '@/components/onboarding/SampleDataBanner'

const TERMINAL_STATUSES = new Set(['REJECTED', 'WITHDRAWN', 'ACCEPTED', 'DECLINED'])
const UPCOMING_LIMIT = 5
const PIPELINE_STAGES: { status: ApplicationStatusValue; label: string }[] = [
  { status: 'SAVED', label: 'Saved' },
  { status: 'APPLIED', label: 'Applied' },
  { status: 'OA', label: 'OA' },
  { status: 'INTERVIEW', label: 'Interview' },
  { status: 'FINAL_ROUND', label: 'Final' },
  { status: 'OFFER', label: 'Offer' },
]

function stagesFor(applications: ApplicationSummary[]): AscentStage[] {
  return PIPELINE_STAGES.map(({ status, label }) => ({
    key: status,
    label,
    count: applications.filter((application) => application.status === status && !application.archived).length,
    line: STATUS_LINE[status],
    to: '/applications/board',
  }))
}

function useClock(): Date {
  const [now, setNow] = useState(() => new Date())
  useEffect(() => {
    const timer = window.setInterval(() => setNow(new Date()), 30_000)
    return () => window.clearInterval(timer)
  }, [])
  return now
}

export function DashboardPage() {
  const applicationsQuery = useQuery({
    queryKey: ['dashboard-applications'],
    queryFn: () => listApplications({ size: 500 }),
  })
  const stalledQuery = useQuery({ queryKey: ['dashboard-stalled'], queryFn: listStalledApplications })
  const upcomingInterviewsQuery = useQuery({
    queryKey: ['dashboard-upcoming-interviews'],
    queryFn: () => listInterviews({ upcoming: true, size: 50, sort: 'scheduled_at', direction: 'asc' }),
  })
  const pastInterviewsQuery = useQuery({
    queryKey: ['dashboard-past-interviews'],
    queryFn: () => listInterviews({ upcoming: false, size: 200 }),
  })
  const tasksQuery = useQuery({ queryKey: ['dashboard-tasks'], queryFn: listPreparationTasks })

  const isLoading =
    applicationsQuery.isLoading ||
    stalledQuery.isLoading ||
    upcomingInterviewsQuery.isLoading ||
    pastInterviewsQuery.isLoading ||
    tasksQuery.isLoading

  const hasError =
    applicationsQuery.isError ||
    stalledQuery.isError ||
    upcomingInterviewsQuery.isError ||
    pastInterviewsQuery.isError ||
    tasksQuery.isError

  const applications = applicationsQuery.data?.items ?? []
  const upcomingInterviews = upcomingInterviewsQuery.data?.items ?? []
  const pastInterviews = pastInterviewsQuery.data?.items ?? []
  const tasks = tasksQuery.data ?? []

  const missingDebriefs = pastInterviews.filter((interview) => !interview.has_feedback && interview.scheduled_at !== null)
  const dueTasks = tasks.filter((task) => task.status === 'open' && task.due_date !== null && isPast(parseISO(task.due_date)))

  const actionQueue = buildActionQueue({
    stalled: stalledQuery.data ?? [],
    upcomingInterviews,
    missingDebriefs,
    dueTasks,
  })

  const activeApplications = applications.filter((application) => !application.archived && !TERMINAL_STATUSES.has(application.status))
  const offers = applications.filter((application) => application.status === 'OFFER' || application.status === 'ACCEPTED').length
  const completedTasks = tasks.filter((task) => task.status === 'completed').length
  const activity = summariseActivity({ applications, interviews: pastInterviews, tasks })

  return (
    <div className="space-y-8">
      <PageHeader
        title="Dashboard"
        eyebrow={<DashboardClock />}
        description="Where each application stands, what is coming up, and what needs a nudge."
        actions={
          <Button variant="cta" size="lg" asChild>
            <Link to="/applications/new">
              <PlusIcon aria-hidden />
              Log an application
            </Link>
          </Button>
        }
      />

      <SampleDataBanner />

      {!isLoading && !hasError && applications.length === 0 && (
        <div className="flex flex-col items-start gap-3 rounded-md border border-dashed border-foreground/30 p-5">
          <p className="text-display text-xl">Nothing on the climb yet</p>
          <p className="text-sm text-muted-foreground">
            Log your first application, or fill the app with sample data to look around. Sample rows are marked and can be removed in one click.
          </p>
          <LoadSampleDataButton />
        </div>
      )}

      <section aria-labelledby="line-heading" className="space-y-4">
        <BoardHeading id="line-heading" action={<PanelLink to="/applications/board">Open board</PanelLink>}>
          Where you stand
        </BoardHeading>
        {applicationsQuery.isLoading && <Skeleton className="h-28 w-full" data-testid="pipeline-loading" />}
        {applicationsQuery.isError && <ErrorState title="Could not load data." description="Try again in a moment." />}
        {applicationsQuery.data && (
          <div data-testid="pipeline-strip" className="rounded-md border bg-card px-4 pt-6 pb-4 sm:px-6">
            <Ascent stages={stagesFor(applications)} />
          </div>
        )}
      </section>

      <div className="grid gap-8 xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
        <section aria-labelledby="interviews-heading" className="min-w-0">
          <div className="flex items-center justify-between gap-3 rounded-t-md bg-brand px-4 py-2.5 text-brand-foreground">
            <h3 id="interviews-heading" className="text-display text-2xl">
              Next interviews
            </h3>
            <Link to="/interviews" className="board-label inline-flex items-center gap-1 hover:underline hover:decoration-signal hover:decoration-2 hover:underline-offset-4">
              All interviews
              <ArrowRightIcon aria-hidden className="size-3.5" />
            </Link>
          </div>
          <div className="rounded-b-md border border-t-0 bg-card">
            {upcomingInterviewsQuery.isLoading && <Skeleton className="h-48 w-full" />}
            {upcomingInterviewsQuery.data && <UpcomingInterviews interviews={upcomingInterviews.slice(0, UPCOMING_LIMIT)} />}
          </div>
        </section>

        <section aria-labelledby="queue-heading" className="min-w-0 space-y-3">
          <BoardHeading id="queue-heading">Action queue</BoardHeading>
          {isLoading && <Skeleton className="h-48 w-full" data-testid="action-queue-loading" />}
          {!isLoading && hasError && <ErrorState title="Could not load data." description="Try again in a moment." />}
          {!isLoading && !hasError && <ActionQueueList items={actionQueue} />}
        </section>
      </div>

      <ReviewTodayCard />

      <section aria-label="Progress" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <MetricCard title="Prep tasks" loading={tasksQuery.isLoading}>
          <div className="flex items-center gap-4">
            <ProgressRing value={completedTasks} max={tasks.length} label="Prep tasks completed" size={72} stroke={8} />
            <div className="min-w-0">
              <p className="flex items-baseline gap-1.5">
                <FlapText value={completedTasks} className="text-display text-5xl" />
                <span className="text-sm text-muted-foreground">of {tasks.length} done</span>
              </p>
              {tasks.length === 0 ? (
                <Link to="/prep" className="text-xs font-semibold underline decoration-signal decoration-2 underline-offset-4">
                  Add your first prep task
                </Link>
              ) : (
                <p className="text-xs text-muted-foreground">{dueTasks.length} overdue</p>
              )}
            </div>
          </div>
        </MetricCard>

        <MetricCard title="Activity, last 14 days" loading={isLoading}>
          <ActivityStrip activity={activity} />
        </MetricCard>

        <MetricCard title="Still in play" loading={applicationsQuery.isLoading}>
          <p className="flex items-baseline gap-2">
            <FlapText value={activeApplications.length} className="text-display text-5xl" />
            <span className="text-sm text-muted-foreground">active applications</span>
          </p>
          <p className="mt-2 text-sm">
            {offers > 0 ? (
              <span className="font-semibold">
                {offers} {offers === 1 ? 'offer' : 'offers'} on the table
              </span>
            ) : (
              <span className="text-muted-foreground">No offers yet</span>
            )}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {upcomingInterviews[0]?.scheduled_at ? `Next interview ${relativeDay(upcomingInterviews[0].scheduled_at)}` : 'No interviews scheduled'}
          </p>
        </MetricCard>
      </section>
    </div>
  )
}

function DashboardClock() {
  const now = useClock()
  return (
    <span className="inline-flex items-center gap-3">
      <span>{format(now, 'EEEE d MMMM')}</span>
      <span aria-hidden className="h-3 w-px bg-border" />
      <time dateTime={now.toISOString()} className="tabular text-foreground">
        {format(now, 'HH:mm')}
      </time>
    </span>
  )
}

function BoardHeading({ id, action, children }: { id: string; action?: ReactNode; children: ReactNode }) {
  return (
    <div className="flex items-end justify-between gap-3">
      <h3 id={id} className="text-display text-2xl">
        {children}
      </h3>
      {action}
    </div>
  )
}

function relativeDay(iso: string): string {
  const date = parseISO(iso)
  if (isToday(date)) return `today at ${format(date, 'HH:mm')}`
  if (isTomorrow(date)) return `tomorrow at ${format(date, 'HH:mm')}`
  return format(date, 'EEE d MMM')
}

function MetricCard({ title, loading, children }: { title: string; loading: boolean; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-4 rounded-md border border-t-[3px] border-t-rule bg-card p-4 sm:p-5">
      <h3 className="board-label text-muted-foreground">{title}</h3>
      <div className="flex flex-1 flex-col justify-center">{loading ? <Skeleton className="h-16 w-full" /> : children}</div>
    </div>
  )
}

function ActivityStrip({ activity }: { activity: ActivitySummary }) {
  const peak = Math.max(1, ...activity.days.map((day) => day.count))
  return (
    <div className="space-y-3">
      <p className="text-sm font-semibold">Active days in a row: {activity.streak}</p>
      <p className="text-xs text-muted-foreground">
        {activity.activeDays} active {activity.activeDays === 1 ? 'day' : 'days'} in the last 14
      </p>
      <div
        role="img"
        aria-label={`${activity.total} recorded actions over the last 14 days, on ${activity.activeDays} days`}
        className="flex h-12 items-end gap-[3px]"
      >
        {activity.days.map((day) => (
          <span
            key={day.date}
            title={`${day.date}: ${day.count}`}
            className={cn('flex-1 rounded-[1px]', day.count > 0 ? 'bg-primary' : 'bg-muted')}
            style={{ height: `${day.count > 0 ? 25 + (day.count / peak) * 75 : 10}%` }}
          />
        ))}
      </div>
      {activity.streak === 0 && (
        <p className="text-xs text-muted-foreground">Log an application or finish a prep task to start a streak.</p>
      )}
    </div>
  )
}

function PanelLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link
      to={to}
      className="board-label inline-flex items-center gap-1 text-foreground underline decoration-signal decoration-2 underline-offset-4 hover:decoration-foreground"
    >
      {children}
      <ArrowRightIcon aria-hidden className="size-3.5" />
    </Link>
  )
}

function UpcomingInterviews({ interviews }: { interviews: InterviewSummary[] }) {
  if (interviews.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3 p-5">
        <p className="text-display text-xl">No interviews on the board</p>
        <p className="text-sm text-muted-foreground">
          When an application reaches an interview, schedule the round so prep and reminders can follow.
        </p>
        <Button variant="outline" size="sm" asChild>
          <Link to="/interviews/new">Schedule an interview</Link>
        </Button>
      </div>
    )
  }

  return (
    <div>
      <div
        aria-hidden
        className="board-label hidden grid-cols-[4.5rem_3.5rem_minmax(0,1fr)_minmax(0,9rem)_6.5rem] gap-3 border-b px-4 py-2 text-muted-foreground sm:grid"
      >
        <span>Date</span>
        <span>Time</span>
        <span>Company</span>
        <span>Round</span>
        <span className="text-right">Prep</span>
      </div>
      <ul aria-label="Upcoming interviews" className="divide-y">
        {interviews.map((interview) => {
          const date = interview.scheduled_at ? parseISO(interview.scheduled_at) : null
          return (
            <li
              key={interview.id}
              className="row-interactive grid grid-cols-[4.5rem_minmax(0,1fr)_auto] items-center gap-3 px-4 py-3 sm:grid-cols-[4.5rem_3.5rem_minmax(0,1fr)_minmax(0,9rem)_6.5rem]"
            >
              <FlapText value={date ? format(date, 'dd MMM').toUpperCase() : 'TBC'} className="text-sm font-semibold" />
              <span className="tabular hidden text-sm text-muted-foreground sm:block">{date ? format(date, 'HH:mm') : '--:--'}</span>
              <Link to={`/interviews/${interview.id}`} className="min-w-0">
                <span className="block truncate text-[15px] font-semibold">{interview.company_name}</span>
                <span className="block truncate text-xs text-muted-foreground sm:hidden">
                  Round {interview.round_number} · {formatLabel(interview.round_type)}
                  {date ? ` · ${format(date, 'HH:mm')}` : ''}
                </span>
              </Link>
              <span className="hidden truncate text-sm sm:block">
                R{interview.round_number} · {formatLabel(interview.round_type)}
              </span>
              <Link
                to={`/interviews/${interview.id}/prep`}
                className="board-label justify-self-end rounded-sm border border-foreground/25 px-2 py-1 hover:border-foreground hover:bg-signal hover:text-signal-foreground"
              >
                Prep pack
              </Link>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
