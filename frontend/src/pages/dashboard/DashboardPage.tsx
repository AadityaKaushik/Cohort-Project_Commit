import { useQuery } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import { isPast, parseISO } from 'date-fns'
import { listApplications, listStalledApplications } from '@/lib/applications-api'
import { listInterviews } from '@/lib/interviews-api'
import { listPreparationTasks } from '@/lib/prep-api'
import { buildActionQueue } from '@/lib/action-queue'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { PipelineStrip } from '@/components/dashboard/PipelineStrip'
import { ActionQueueList } from '@/components/dashboard/ActionQueueList'

/** US-49: replaces the Sprint 1 placeholder dashboard with a live pipeline strip and a ranked, capped action queue -- "the dashboard tells me what to do today." */
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

  const missingDebriefs = (pastInterviewsQuery.data?.items ?? []).filter(
    (interview) => !interview.has_feedback && interview.scheduled_at !== null,
  )
  const dueTasks = (tasksQuery.data ?? []).filter(
    (task) => task.status === 'open' && task.due_date !== null && isPast(parseISO(task.due_date)),
  )

  const actionQueue = buildActionQueue({
    stalled: stalledQuery.data ?? [],
    upcomingInterviews: upcomingInterviewsQuery.data?.items ?? [],
    missingDebriefs,
    dueTasks,
  })

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Dashboard</h2>
        <p className="text-sm text-muted-foreground">Where your campaign stands today, and what to do next.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Pipeline</CardTitle>
        </CardHeader>
        <CardContent>
          {applicationsQuery.isLoading && <Skeleton className="h-20 w-full" data-testid="pipeline-loading" />}
          {applicationsQuery.isError && <ErrorAlert />}
          {applicationsQuery.data && <PipelineStrip applications={applicationsQuery.data.items} />}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Action queue</CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading && <Skeleton className="h-40 w-full" data-testid="action-queue-loading" />}
          {!isLoading && hasError && <ErrorAlert />}
          {!isLoading && !hasError && <ActionQueueList items={actionQueue} />}
        </CardContent>
      </Card>
    </div>
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
