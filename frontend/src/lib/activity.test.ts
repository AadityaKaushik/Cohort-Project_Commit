import { describe, expect, it } from 'vitest'
import { summariseActivity } from '@/lib/activity'
import type { ApplicationSummary } from '@/lib/applications-api'
import type { PreparationTask } from '@/lib/prep-api'

const NOW = new Date('2026-03-10T15:00:00')

function application(dateApplied: string): ApplicationSummary {
  return {
    id: dateApplied,
    company: { id: 'c', name: 'Acme', website: null, industry: null },
    job_title: 'Engineer',
    status: 'APPLIED',
    current_stage: 'APPLIED',
    days_in_stage: 1,
    date_applied: dateApplied,
    priority: null,
    archived: false,
    allowed_next_statuses: [],
    correction_statuses: [],
  }
}

function completedTask(completedAt: string): PreparationTask {
  return {
    id: completedAt,
    topic_id: 't',
    topic_name: 'Arrays',
    title: 'Practice',
    description: null,
    due_date: null,
    estimated_effort_minutes: null,
    status: 'completed',
    completed_at: completedAt,
    created_at: completedAt,
    updated_at: completedAt,
  }
}

describe('summariseActivity', () => {
  it('counts a streak of consecutive active days ending yesterday when today is still empty', () => {
    const summary = summariseActivity(
      {
        applications: [application('2026-03-09'), application('2026-03-08')],
        interviews: [],
        tasks: [completedTask('2026-03-07T10:00:00'), completedTask('2026-03-05T10:00:00')],
      },
      NOW,
    )

    expect(summary.streak).toBe(3)
    expect(summary.activeDays).toBe(4)
    expect(summary.days).toHaveLength(14)
    expect(summary.days.at(-1)).toEqual({ date: '2026-03-10', count: 0 })
  })

  it('reports no streak and no activity when nothing was recorded', () => {
    const summary = summariseActivity({ applications: [], interviews: [], tasks: [] }, NOW)

    expect(summary).toMatchObject({ streak: 0, activeDays: 0, total: 0 })
  })
})
