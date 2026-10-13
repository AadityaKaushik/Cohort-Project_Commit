import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { AlertCircleIcon, ArrowRightIcon, BriefcaseIcon, CalendarClockIcon, FlameIcon, PlusIcon } from 'lucide-react'
import { format, isPast, isToday, isTomorrow, parseISO } from 'date-fns'
import { listApplications, listStalledApplications } from '@/lib/applications-api'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import { listPreparationTasks } from '@/lib/prep-api'
import { buildActionQueue } from '@/lib/action-queue'
import { summariseActivity, type ActivitySummary } from '@/lib/activity'
import { formatLabel } from '@/lib/labels'
import { useAuth } from '@/hooks/use-auth'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'
import { PipelineStrip } from '@/components/dashboard/PipelineStrip'
import { ActionQueueList } from '@/components/dashboard/ActionQueueList'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { CountUp } from '@/components/motion/Reveal'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { cn } from '@/lib/utils'

const TERMINAL_STATUSES = new Set(['REJECTED', 'WITHDRAWN', 'ACCEPTED', 'DECLINED'])
const UPCOMING_LIMIT = 4

function greetingFor(hour: number): string {
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

/** US-49: the dashboard tells me what to do today. Greeting, progress at a glance, the live pipeline, a ranked and capped action queue, and the next interviews. */
export function DashboardPage() {
  const { user } = useAuth()
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
  const firstName = (user?.display_name ?? '').trim().split(/\s+/).filter((part) => !part.startsWith('['))[0]

  return (
    <div className="space-y-6">
      <section aria-labelledby="dashboard-greeting" className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div className="space-y-2">
          <p className="text-sm text-muted-foreground">{format(new Date(), 'EEEE d MMMM')}</p>
          <h2 id="dashboard-greeting" className="text-display text-3xl sm:text-4xl">
            {greetingFor(new Date().getHours())}
            {firstName ? `, ${firstName}` : ''}.
          </h2>
          <RotatingQuote className="text-sm" />
        </div>
        <Button variant="cta" size="lg" asChild className="self-start sm:self-auto">
          <Link to="/applications/new">
            <PlusIcon aria-hidden />
            Log an application
          </Link>
        </Button>
      </section>

      <section aria-label="Progress at a glance" className="grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4 [&>*:nth-child(-n+2)]:col-span-2 sm:[&>*:nth-child(-n+2)]:col-span-1">
        <MetricCard title="Prep tasks" loading={tasksQuery.isLoading}>
          <div className="flex items-center gap-4">
            <ProgressRing value={completedTasks} max={tasks.length} label="Prep tasks completed" size={76} stroke={8} />
            <div className="min-w-0 text-sm">
              <p>
                <CountUp value={completedTasks} className="text-xl font-semibold" />
                <span className="text-muted-foreground"> of {tasks.length} done</span>
              </p>
              {tasks.length === 0 ? (
                <Link to="/prep" className="text-xs font-medium text-primary hover:underline">
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

        <MetricCard title="Active pipeline" loading={applicationsQuery.isLoading}>
          <div className="flex items-end justify-between gap-3">
            <div>
              <CountUp value={activeApplications.length} className="text-4xl font-semibold tracking-tight" />
              <p className="mt-1 text-xs text-muted-foreground">
                {offers > 0 ? (
                  <span className="font-medium text-success">
                    {offers} {offers === 1 ? 'offer' : 'offers'}
                  </span>
                ) : (
                  'applications in progress'
                )}
              </p>
            </div>
            <span className="hidden size-10 items-center justify-center rounded-xl bg-primary/12 text-primary sm:flex">
              <BriefcaseIcon aria-hidden className="size-5" />
            </span>
          </div>
        </MetricCard>

        <MetricCard title="Upcoming interviews" loading={upcomingInterviewsQuery.isLoading}>
          <div className="flex items-end justify-between gap-3">
            <div>
              <CountUp value={upcomingInterviews.length} className="text-4xl font-semibold tracking-tight" />
              <p className="mt-1 text-xs text-muted-foreground">
                {upcomingInterviews[0]?.scheduled_at ? `Next ${relativeDay(upcomingInterviews[0].scheduled_at)}` : 'None scheduled'}
              </p>
            </div>
            <span className="hidden size-10 items-center justify-center rounded-xl bg-info/12 text-info sm:flex">
              <CalendarClockIcon aria-hidden className="size-5" />
            </span>
          </div>
        </MetricCard>
      </section>

      <Panel title="Pipeline" action={<PanelLink to="/applications/board">Open board</PanelLink>}>
        {applicationsQuery.isLoading && <Skeleton className="h-28 w-full rounded-xl" data-testid="pipeline-loading" />}
        {applicationsQuery.isError && <ErrorAlert />}
        {applicationsQuery.data && <PipelineStrip applications={applicationsQuery.data.items} />}
      </Panel>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
        <Panel title="Action queue">
          {isLoading && <Skeleton className="h-48 w-full rounded-xl" data-testid="action-queue-loading" />}
          {!isLoading && hasError && <ErrorAlert />}
          {!isLoading && !hasError && <ActionQueueList items={actionQueue} />}
        </Panel>
        <Panel title="Next interviews" action={<PanelLink to="/interviews">All interviews</PanelLink>}>
          {upcomingInterviewsQuery.isLoading && <Skeleton className="h-48 w-full rounded-xl" />}
          {upcomingInterviewsQuery.data && <UpcomingInterviews interviews={upcomingInterviews.slice(0, UPCOMING_LIMIT)} />}
        </Panel>
      </div>
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
    <div className="surface-raised flex flex-col gap-4 rounded-xl border p-4 sm:p-5">
      <h3 className="text-xs font-medium tracking-wide text-muted-foreground uppercase">{title}</h3>
      <div className="flex flex-1 flex-col justify-center">{loading ? <Skeleton className="h-16 w-full" /> : children}</div>
    </div>
  )
}

function ActivityStrip({ activity }: { activity: ActivitySummary }) {
  const peak = Math.max(1, ...activity.days.map((day) => day.count))
  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2">
        <FlameIcon aria-hidden className={cn('size-5', activity.streak > 0 ? 'text-warning' : 'text-muted-foreground')} />
        <p className="text-sm">
          <CountUp value={activity.streak} className="text-xl font-semibold" />
          <span className="text-muted-foreground">
            {' '}
            day streak · {activity.activeDays} active {activity.activeDays === 1 ? 'day' : 'days'}
          </span>
        </p>
      </div>
      <div
        role="img"
        aria-label={`${activity.total} recorded actions over the last 14 days, on ${activity.activeDays} days`}
        className="flex h-10 items-end gap-1"
      >
        {activity.days.map((day, index) => (
          <span
            key={day.date}
            title={`${day.date}: ${day.count}`}
            className={cn('animate-grow-y flex-1 origin-bottom rounded-sm', day.count > 0 ? 'bg-primary' : 'bg-muted')}
            style={{ height: `${day.count > 0 ? 25 + (day.count / peak) * 75 : 12}%`, animationDelay: `${index * 30}ms` }}
          />
        ))}
      </div>
      {activity.streak === 0 && (
        <p className="text-xs text-muted-foreground">Log an application or finish a prep task today to start a streak.</p>
      )}
    </div>
  )
}

function Panel({ title, action, children }: { title: string; action?: ReactNode; children: ReactNode }) {
  return (
    <section aria-label={title} className="space-y-3">
      <div className="flex items-center justify-between gap-2">
        <h3 className="text-base font-semibold tracking-tight">{title}</h3>
        {action}
      </div>
      {children}
    </section>
  )
}

function PanelLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link to={to} className="group/link inline-flex items-center gap-1 text-sm font-medium text-primary">
      {children}
      <ArrowRightIcon aria-hidden className="size-3.5 transition-transform group-hover/link:translate-x-0.5" />
    </Link>
  )
}

function UpcomingInterviews({ interviews }: { interviews: InterviewSummary[] }) {
  if (interviews.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3 rounded-xl border border-dashed bg-surface-2/50 p-5">
        <p className="text-sm font-medium">No interviews on the calendar</p>
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
    <ul aria-label="Upcoming interviews" className="space-y-2">
      {interviews.map((interview, index) => {
        const date = interview.scheduled_at ? parseISO(interview.scheduled_at) : null
        return (
          <li key={interview.id} className="animate-enter" style={{ animationDelay: `${index * 50}ms` }}>
            <Link
              to={`/interviews/${interview.id}`}
              className="lift flex items-center gap-3 rounded-xl border bg-card p-3 hover:border-primary/30"
            >
              <span className="flex w-12 shrink-0 flex-col items-center rounded-lg border bg-surface-2 py-1.5 leading-none">
                <span className="text-[10px] font-medium text-muted-foreground uppercase">{date ? format(date, 'MMM') : '—'}</span>
                <span className="mt-1 text-lg font-semibold tabular-nums">{date ? format(date, 'd') : '?'}</span>
              </span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-sm font-medium">{interview.company_name}</span>
                <span className="block truncate text-xs text-muted-foreground">
                  Round {interview.round_number} · {formatLabel(interview.round_type)}
                  {date ? ` · ${format(date, 'HH:mm')}` : ''}
                </span>
              </span>
            </Link>
          </li>
        )
      })}
    </ul>
  )
}

function ErrorAlert() {
  return (
    <Alert variant="destructive">
      <AlertCircleIcon className="h-4 w-4" />
      <AlertTitle>Couldn&apos;t load this</AlertTitle>
      <AlertDescription>Something went wrong talking to the server.</AlertDescription>
    </Alert>
  )
}
