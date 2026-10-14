import { format, isAfter, parseISO, startOfDay, subDays } from 'date-fns'
import type { ApplicationSummary } from '@/lib/applications-api'
import type { InterviewSummary } from '@/lib/interviews-api'
import type { PreparationTask } from '@/lib/prep-api'

export const ACTIVITY_WINDOW_DAYS = 14

export interface ActivityDay {
  date: string
  count: number
}

export interface ActivitySummary {
  days: ActivityDay[]
  streak: number
  activeDays: number
  total: number
}

export interface ActivityInput {
  applications: ApplicationSummary[]
  interviews: InterviewSummary[]
  tasks: PreparationTask[]
}

function dayKey(iso: string): string {
  return format(parseISO(iso), 'yyyy-MM-dd')
}

export function summariseActivity(input: ActivityInput, now: Date = new Date()): ActivitySummary {
  const counts = new Map<string, number>()
  const bump = (iso: string | null) => {
    if (iso) {
      const key = dayKey(iso)
      counts.set(key, (counts.get(key) ?? 0) + 1)
    }
  }

  input.applications.forEach((application) => bump(application.date_applied))
  input.tasks.forEach((task) => bump(task.status === 'completed' ? task.completed_at : null))
  input.interviews.forEach((interview) => {
    const held = interview.completed_at ?? interview.scheduled_at
    bump(held && !isAfter(parseISO(held), now) ? held : null)
  })

  const today = startOfDay(now)
  const days: ActivityDay[] = Array.from({ length: ACTIVITY_WINDOW_DAYS }, (_, index) => {
    const date = format(subDays(today, ACTIVITY_WINDOW_DAYS - 1 - index), 'yyyy-MM-dd')
    return { date, count: counts.get(date) ?? 0 }
  })

  let streak = 0
  let cursor = (counts.get(format(today, 'yyyy-MM-dd')) ?? 0) > 0 ? today : subDays(today, 1)
  while ((counts.get(format(cursor, 'yyyy-MM-dd')) ?? 0) > 0) {
    streak += 1
    cursor = subDays(cursor, 1)
  }

  return {
    days,
    streak,
    activeDays: days.filter((day) => day.count > 0).length,
    total: days.reduce((sum, day) => sum + day.count, 0),
  }
}
