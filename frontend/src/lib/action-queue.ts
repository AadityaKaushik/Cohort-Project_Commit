import type { StalledApplication } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import type { InterviewSummary } from '@/lib/interviews-api'
import type { PreparationTask } from '@/lib/prep-api'

export const ACTION_QUEUE_CAP = 7

export type ActionItemType = 'stalled' | 'upcoming_interview' | 'missing_debrief' | 'due_task'

export interface ActionItem {
  id: string
  type: ActionItemType
  title: string
  subtitle: string
  href: string
}

export interface ActionQueueInput {
  stalled: StalledApplication[]
  upcomingInterviews: InterviewSummary[]
  missingDebriefs: InterviewSummary[]
  dueTasks: PreparationTask[]
}

export function buildActionQueue(input: ActionQueueInput): ActionItem[] {
  const stalledItems: ActionItem[] = [...input.stalled]
    .sort((a, b) => b.days_in_stage - a.days_in_stage)
    .map((app) => ({
      id: `stalled-${app.application_id}`,
      type: 'stalled',
      title: `Follow up: ${app.company_name}`,
      subtitle: `Stalled at ${formatStage(app.current_stage)} for ${Math.round(app.days_in_stage)} days (threshold ${Math.round(app.threshold_days)})`,
      href: `/applications/${app.application_id}`,
    }))

  const upcomingInterviewItems: ActionItem[] = [...input.upcomingInterviews]
    .sort((a, b) => compareDates(a.scheduled_at, b.scheduled_at))
    .map((interview) => ({
      id: `upcoming-${interview.id}`,
      type: 'upcoming_interview',
      title: `Prepare: ${interview.company_name} interview`,
      subtitle: interview.scheduled_at ? `Scheduled ${formatDate(interview.scheduled_at)}` : 'Scheduled soon',
      href: `/interviews/${interview.id}`,
    }))

  const missingDebriefItems: ActionItem[] = [...input.missingDebriefs]
    .sort((a, b) => compareDates(a.scheduled_at, b.scheduled_at))
    .map((interview) => ({
      id: `debrief-${interview.id}`,
      type: 'missing_debrief',
      title: `Debrief: ${interview.company_name} interview`,
      subtitle: interview.scheduled_at ? `Happened ${formatDate(interview.scheduled_at)}, no debrief yet` : 'No debrief yet',
      href: `/interviews/${interview.id}/debrief`,
    }))

  const dueTaskItems: ActionItem[] = [...input.dueTasks]
    .sort((a, b) => compareDates(a.due_date, b.due_date))
    .map((task) => ({
      id: `task-${task.id}`,
      type: 'due_task',
      title: task.title,
      subtitle: task.due_date ? `Due ${formatDate(task.due_date)}, ${task.topic_name ?? 'Prep task'}` : task.topic_name ?? 'Prep task',
      href: '/prep',
    }))

  return [...stalledItems, ...upcomingInterviewItems, ...missingDebriefItems, ...dueTaskItems].slice(0, ACTION_QUEUE_CAP)
}

function compareDates(a: string | null, b: string | null): number {
  if (a === null && b === null) return 0
  if (a === null) return 1
  if (b === null) return -1
  return new Date(a).getTime() - new Date(b).getTime()
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
}

function formatStage(stage: string): string {
  return formatLabel(stage)
}
