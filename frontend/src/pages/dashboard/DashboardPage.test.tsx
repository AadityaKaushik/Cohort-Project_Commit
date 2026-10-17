import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DashboardPage } from '@/pages/dashboard/DashboardPage'
import { listApplications, listStalledApplications, type ApplicationSummary, type StalledApplication } from '@/lib/applications-api'
import { listInterviews, type InterviewSummary } from '@/lib/interviews-api'
import { listPreparationTasks, type PreparationTask } from '@/lib/prep-api'
import { useAuth } from '@/hooks/use-auth'

vi.mock('@/hooks/use-auth')
vi.mock('@/lib/applications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-api')>('@/lib/applications-api')
  return { ...actual, listApplications: vi.fn(), listStalledApplications: vi.fn() }
})
vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, listInterviews: vi.fn() }
})
vi.mock('@/lib/prep-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/prep-api')>('@/lib/prep-api')
  return { ...actual, listPreparationTasks: vi.fn(), getReviewQueue: vi.fn().mockResolvedValue({ as_of: '2026-03-10', items: [] }) }
})

function application(overrides: Partial<ApplicationSummary> = {}): ApplicationSummary {
  return {
    id: 'app-1',
    company: { id: 'company-1', name: 'Acme', website: null, industry: null },
    job_title: 'Engineer',
    status: 'APPLIED',
    current_stage: 'APPLIED',
    days_in_stage: 1,
    date_applied: '2026-01-01',
    priority: null,
    archived: false,
    allowed_next_statuses: [],
    correction_statuses: [],
    ...overrides,
  }
}

function stalledApp(id: string): StalledApplication {
  return {
    application_id: id,
    company_name: `Company ${id}`,
    job_title: 'Engineer',
    current_stage: 'APPLIED',
    days_in_stage: 30,
    threshold_days: 14,
    basis: 'system_default',
    sample_size: 0,
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
    scheduled_at: '2026-01-05T10:00:00Z',
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
    due_date: '2020-01-01',
    estimated_effort_minutes: null,
    status: 'open',
    completed_at: null,
    created_at: '2019-12-01T00:00:00Z',
    updated_at: '2019-12-01T00:00:00Z',
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/']}>
        <DashboardPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('DashboardPage', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'jane@example.com', display_name: 'Jane Doe', target_role: null, graduation_year: null, target_date: null, onboarded: true, timezone: null, fl_opt_in: false, fl_privacy_posture: 'balanced', created_at: '' },
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      confirmEmail: vi.fn(),
      logout: vi.fn(),
    })
    vi.mocked(listApplications).mockReset()
    vi.mocked(listStalledApplications).mockReset()
    vi.mocked(listInterviews).mockReset()
    vi.mocked(listPreparationTasks).mockReset()
  })

  it('shows loading skeletons before data arrives', () => {
    vi.mocked(listApplications).mockReturnValue(new Promise(() => {}))
    vi.mocked(listStalledApplications).mockReturnValue(new Promise(() => {}))
    vi.mocked(listInterviews).mockReturnValue(new Promise(() => {}))
    vi.mocked(listPreparationTasks).mockReturnValue(new Promise(() => {}))

    renderPage()

    expect(screen.getByTestId('pipeline-loading')).toBeInTheDocument()
    expect(screen.getByTestId('action-queue-loading')).toBeInTheDocument()
  })

  it('renders the pipeline strip and an empty action queue when nothing needs attention', async () => {
    vi.mocked(listApplications).mockResolvedValue({ items: [application()], total: 1, page: 0, size: 500 })
    vi.mocked(listStalledApplications).mockResolvedValue([])
    vi.mocked(listInterviews).mockResolvedValue({ items: [], total: 0, page: 0, size: 50 })
    vi.mocked(listPreparationTasks).mockResolvedValue([])

    renderPage()

    expect(await screen.findByTestId('pipeline-strip')).toBeInTheDocument()
    expect(screen.getByText(/nothing needs attention/i)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /schedule an interview/i })).toHaveAttribute('href', '/interviews/new')
    expect(screen.getByText(/start a streak/i)).toBeInTheDocument()
    expect(screen.getByText('Active days in a row: 0')).toBeInTheDocument()
  })

  it('caps the rendered action queue at 7 items even with far more than 7 actionable items across every source', async () => {
    vi.mocked(listApplications).mockResolvedValue({ items: [], total: 0, page: 0, size: 500 })
    vi.mocked(listStalledApplications).mockResolvedValue(Array.from({ length: 4 }, (_, i) => stalledApp(`s${i}`)))
    vi.mocked(listInterviews).mockImplementation((params) => {
      if (params?.upcoming) {
        return Promise.resolve({
          items: Array.from({ length: 4 }, (_, i) => interview({ id: `up-${i}` })),
          total: 4,
          page: 0,
          size: 50,
        })
      }
      return Promise.resolve({
        items: Array.from({ length: 4 }, (_, i) => interview({ id: `past-${i}`, has_feedback: false })),
        total: 4,
        page: 0,
        size: 200,
      })
    })
    vi.mocked(listPreparationTasks).mockResolvedValue(Array.from({ length: 4 }, (_, i) => task({ id: `task-${i}` })))

    renderPage()

    const list = await screen.findByRole('list', { name: 'Next actions' })
    const items = within(list).getAllByRole('listitem')
    expect(items.length).toBe(7)
  })
})
