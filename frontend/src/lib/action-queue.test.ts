import { describe, expect, it } from 'vitest'
import { ACTION_QUEUE_CAP, buildActionQueue } from '@/lib/action-queue'
import type { StalledApplication } from '@/lib/applications-api'
import type { InterviewSummary } from '@/lib/interviews-api'
import type { PreparationTask } from '@/lib/prep-api'

function stalledApp(overrides: Partial<StalledApplication> = {}): StalledApplication {
  return {
    application_id: 'app-1',
    company_name: 'Acme',
    job_title: 'Engineer',
    current_stage: 'APPLIED',
    days_in_stage: 20,
    threshold_days: 14,
    basis: 'system_default',
    sample_size: 0,
    ...overrides,
  }
}

function interview(overrides: Partial<InterviewSummary> = {}): InterviewSummary {
  return {
    id: 'interview-1',
    application_id: 'app-1',
    company_name: 'Acme',
    job_title: 'Engineer',
    round_number: 1,
    round_type: 'TECHNICAL',
    scheduled_at: '2026-01-01T10:00:00Z',
    completed_at: null,
    mode: 'video',
    outcome: 'pending',
    has_feedback: false,
    created_at: '2025-12-01T00:00:00Z',
    ...overrides,
  }
}

function task(overrides: Partial<PreparationTask> = {}): PreparationTask {
  return {
    id: 'task-1',
    topic_id: 'topic-1',
    topic_name: 'Arrays',
    title: 'Practice arrays',
    description: null,
    due_date: '2026-01-01',
    estimated_effort_minutes: null,
    status: 'open',
    completed_at: null,
    created_at: '2025-12-01T00:00:00Z',
    updated_at: '2025-12-01T00:00:00Z',
    ...overrides,
  }
}

describe('buildActionQueue', () => {
  it('caps the queue at 7 items even when far more than 7 actionable items exist', () => {
    const stalled = Array.from({ length: 5 }, (_, i) => stalledApp({ application_id: `stalled-${i}`, days_in_stage: 20 + i }))
    const upcoming = Array.from({ length: 5 }, (_, i) => interview({ id: `upcoming-${i}` }))
    const missingDebriefs = Array.from({ length: 5 }, (_, i) => interview({ id: `debrief-${i}` }))
    const dueTasks = Array.from({ length: 5 }, (_, i) => task({ id: `task-${i}` }))

    const queue = buildActionQueue({ stalled, upcomingInterviews: upcoming, missingDebriefs, dueTasks })

    expect(queue.length).toBe(ACTION_QUEUE_CAP)
    expect(queue.length).toBeLessThanOrEqual(7)
  })

  it('never exceeds the cap no matter how the 20 candidate items are distributed across categories', () => {
    const stalled = Array.from({ length: 20 }, (_, i) => stalledApp({ application_id: `s${i}` }))

    const queue = buildActionQueue({ stalled, upcomingInterviews: [], missingDebriefs: [], dueTasks: [] })

    expect(queue.length).toBe(7)
  })

  it('returns every item when there are fewer than 7 total, never padding', () => {
    const queue = buildActionQueue({
      stalled: [stalledApp()],
      upcomingInterviews: [interview()],
      missingDebriefs: [],
      dueTasks: [],
    })

    expect(queue).toHaveLength(2)
  })

  it('prioritises the most-stalled application first within the stalled category', () => {
    const stalled = [
      stalledApp({ application_id: 'less-stalled', days_in_stage: 15 }),
      stalledApp({ application_id: 'most-stalled', days_in_stage: 40 }),
    ]

    const queue = buildActionQueue({ stalled, upcomingInterviews: [], missingDebriefs: [], dueTasks: [] })

    expect(queue[0]!.id).toBe('stalled-most-stalled')
  })

  it('gives every item an href one click from its action', () => {
    const queue = buildActionQueue({
      stalled: [stalledApp()],
      upcomingInterviews: [interview({ id: 'up-1' })],
      missingDebriefs: [interview({ id: 'db-1' })],
      dueTasks: [task({ id: 'due-1' })],
    })

    expect(queue.find((item) => item.type === 'stalled')?.href).toBe('/applications/app-1')
    expect(queue.find((item) => item.type === 'upcoming_interview')?.href).toBe('/interviews/up-1')
    expect(queue.find((item) => item.type === 'missing_debrief')?.href).toBe('/interviews/db-1/debrief')
    expect(queue.find((item) => item.type === 'missing_debrief')?.title).toBe('Debrief: Acme interview')
    expect(queue.find((item) => item.type === 'due_task')?.href).toBe('/prep')
  })
})
