import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PrepPage } from '@/pages/prep/PrepPage'
import {
  createPreparationTask,
  getPreparationProgress,
  listPreparationTasks,
  listTopics,
  updatePreparationTask,
  type PreparationProgress,
  type PreparationTask,
  type Topic,
} from '@/lib/prep-api'

vi.mock('@/lib/prep-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/prep-api')>('@/lib/prep-api')
  return {
    ...actual,
    getPreparationProgress: vi.fn(),
    listPreparationTasks: vi.fn(),
    listTopics: vi.fn(),
    createPreparationTask: vi.fn(),
    updatePreparationTask: vi.fn(),
    deletePreparationTask: vi.fn(),
    getReviewQueue: vi.fn().mockResolvedValue({ as_of: '2026-03-10', items: [] }),
  }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/prep']}>
        <PrepPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleTopic(overrides: Partial<Topic> = {}): Topic {
  return {
    id: 'topic-1',
    name: 'Arrays',
    slug: 'arrays',
    category: 'dsa',
    description: null,
    default_difficulty: 'easy',
    taxonomy_version: 1,
    created_at: '2026-01-01T00:00:00Z',
    ...overrides,
  }
}

function sampleProgress(): PreparationProgress {
  return {
    categories: [
      { category: 'dsa', total_topics: 30, tasks_total: 2, tasks_completed: 1, average_mastery: 0.62 },
      { category: 'behavioural', total_topics: 2, tasks_total: 0, tasks_completed: 0, average_mastery: null },
    ],
    topics: [
      { topic_id: 'topic-1', topic_name: 'Arrays', slug: 'arrays', category: 'dsa', mastery_score: 0.62, observation_count: 4, sufficient_evidence: true },
      { topic_id: 'topic-2', topic_name: 'Graphs', slug: 'graphs', category: 'dsa', mastery_score: 0, observation_count: 0, sufficient_evidence: false },
    ],
  }
}

function sampleTask(overrides: Partial<PreparationTask> = {}): PreparationTask {
  return {
    id: 'task-1',
    topic_id: 'topic-1',
    topic_name: 'Arrays',
    title: 'Practice 10 array problems',
    description: null,
    due_date: null,
    estimated_effort_minutes: null,
    status: 'open',
    completed_at: null,
    created_at: '2026-01-10T00:00:00Z',
    updated_at: '2026-01-10T00:00:00Z',
    ...overrides,
  }
}

describe('PrepPage', () => {
  beforeEach(() => {
    vi.mocked(getPreparationProgress).mockReset()
    vi.mocked(listPreparationTasks).mockReset()
    vi.mocked(listTopics).mockReset()
    vi.mocked(createPreparationTask).mockReset()
    vi.mocked(updatePreparationTask).mockReset()
  })

  it('shows loading skeletons before data arrives', () => {
    vi.mocked(getPreparationProgress).mockReturnValue(new Promise(() => {}))
    vi.mocked(listPreparationTasks).mockReturnValue(new Promise(() => {}))
    vi.mocked(listTopics).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByText('Interview Prep')).toBeInTheDocument()
  })

  it('shows category progress bars and the topic mastery grid once loaded', async () => {
    vi.mocked(getPreparationProgress).mockResolvedValue(sampleProgress())
    vi.mocked(listPreparationTasks).mockResolvedValue([])
    vi.mocked(listTopics).mockResolvedValue([sampleTopic()])

    renderPage()

    expect(await screen.findByText('Data Structures & Algorithms')).toBeInTheDocument()
    expect(screen.getByText('1/2 tasks · 62% avg. mastery')).toBeInTheDocument()
    expect(screen.getByText('0/0 tasks · not enough data yet')).toBeInTheDocument()

    expect(screen.getByRole('button', { name: /Arrays/ })).toBeInTheDocument()
    expect(screen.getByTitle('62% mastery from 4 observations')).toBeInTheDocument()
    expect(screen.getByText('Insufficient data')).toBeInTheDocument()
  })

  it('shows an empty state and lets the user add a task', async () => {
    vi.mocked(getPreparationProgress).mockResolvedValue(sampleProgress())
    vi.mocked(listPreparationTasks).mockResolvedValue([])
    vi.mocked(listTopics).mockResolvedValue([sampleTopic()])
    vi.mocked(createPreparationTask).mockResolvedValue(sampleTask())

    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText(/no preparation tasks yet/i)).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Add your first task' }))
    expect(screen.getByLabelText('New task')).toHaveFocus()

    await user.type(screen.getByLabelText('New task'), 'Practice 10 array problems')
    await user.click(screen.getByLabelText('Topic'))
    await user.click(await screen.findByRole('option', { name: 'Arrays' }))
    await user.click(screen.getByRole('button', { name: /add task/i }))

    await waitFor(() => expect(createPreparationTask).toHaveBeenCalledWith({ topic_id: 'topic-1', title: 'Practice 10 array problems' }))
  })

  it('toggles a task to completed', async () => {
    vi.mocked(getPreparationProgress).mockResolvedValue(sampleProgress())
    vi.mocked(listPreparationTasks).mockResolvedValue([sampleTask()])
    vi.mocked(listTopics).mockResolvedValue([sampleTopic()])
    vi.mocked(updatePreparationTask).mockResolvedValue(sampleTask({ status: 'completed' }))

    const user = userEvent.setup()
    renderPage()

    expect(await screen.findByText('Practice 10 array problems')).toBeInTheDocument()

    await user.click(screen.getByLabelText('Mark as completed'))

    await waitFor(() => expect(updatePreparationTask).toHaveBeenCalledWith('task-1', { status: 'completed' }))
  })
})
