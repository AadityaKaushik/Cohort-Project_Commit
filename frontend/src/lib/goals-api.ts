import { apiFetch } from '@/lib/api-client'

export type GoalMetric = 'applications' | 'interviews' | 'practice_attempts' | 'contacts_reached'

export const GOAL_METRICS: GoalMetric[] = ['applications', 'interviews', 'practice_attempts', 'contacts_reached']

export interface Goal {
  id: string
  metric: GoalMetric
  weekly_target: number
  progress: number
  achieved: boolean
}

export interface ActivityDay {
  date: string
  count: number
}

export interface Activity {
  days: ActivityDay[]
  current_streak: number
  longest_streak: number
}

export const goalKeys = {
  all: ['goals'] as const,
  activity: ['goals', 'activity'] as const,
}

export function listGoals() {
  return apiFetch<Goal[]>('/goals')
}

export function setGoal(metric: GoalMetric, weeklyTarget: number) {
  return apiFetch<Goal>('/goals', { method: 'PUT', body: { metric, weekly_target: weeklyTarget } })
}

export function deleteGoal(id: string) {
  return apiFetch<void>(`/goals/${id}`, { method: 'DELETE' })
}

export function getActivity(weeks = 26) {
  return apiFetch<Activity>(`/goals/activity?weeks=${weeks}`)
}
