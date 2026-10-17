import type { ComponentType } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ThemeProvider, THEME_STORAGE_KEY } from '@/hooks/use-theme'
import { useAuth } from '@/hooks/use-auth'
import { criticalViolations } from '@/test/a11y'
import { AccountPage } from '@/pages/account/AccountPage'
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage'
import { ApplicationsListPage } from '@/pages/applications/ApplicationsListPage'
import { InterviewsListPage } from '@/pages/interviews/InterviewsListPage'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'
import { PrepPage } from '@/pages/prep/PrepPage'
import { PrivacyPage } from '@/pages/privacy/PrivacyPage'
import { RecommendationsPage } from '@/pages/recommendations/RecommendationsPage'
import { ReportsPage } from '@/pages/reports/ReportsPage'
import { ResumesListPage } from '@/pages/resumes/ResumesListPage'

vi.mock('@/hooks/use-auth')
vi.mock('@/lib/api-client', async () => {
  const actual = await vi.importActual<typeof import('@/lib/api-client')>('@/lib/api-client')
  return { ...actual, apiFetch: vi.fn((path: string) => Promise.resolve(fixtureFor(path))) }
})

const NOW = '2026-10-01T10:00:00Z'
const page = <T,>(items: T[]) => ({ items, total: items.length, page: 0, size: 25 })
const rate = { value: 0.5, formatted: '50.0%', numerator: 5, denominator: 10, still_open: 1, sufficient_data: true }

const application = {
  id: 'app-1',
  company: { id: 'c-1', name: 'Northwind Labs', website: null, industry: null },
  job_title: 'Backend Engineer',
  status: 'INTERVIEW',
  current_stage: 'INTERVIEW',
  days_in_stage: 4,
  date_applied: '2026-09-20',
  priority: 'high',
  archived: false,
  allowed_next_statuses: ['FINAL_ROUND'],
  correction_statuses: [],
}

const interview = {
  id: 'int-1',
  application_id: 'app-1',
  company_name: 'Northwind Labs',
  job_title: 'Backend Engineer',
  round_number: 1,
  round_type: 'TECHNICAL',
  scheduled_at: NOW,
  completed_at: null,
  mode: 'video',
  outcome: 'pending',
  has_feedback: false,
  created_at: NOW,
}

const FIXTURES: [RegExp, unknown][] = [
  [/^\/applications\/stalled/, []],
  [/^\/applications/, page([application])],
  [/^\/interviews/, page([interview])],
  [/^\/resumes/, page([{ id: 'r-1', name: 'Platform resume', version_label: 'v2', target_role: 'Backend', is_default: true, has_file: true, parent_resume_id: null, created_at: NOW, updated_at: NOW }])],
  [/^\/analytics\/funnel/, { applied: 10, reached_oa: 6, reached_interview: 4, reached_final: 2, reached_offer: 1, sample_size: 10 }],
  [/^\/analytics\/conversions/, { application_to_oa: rate, oa_to_interview: rate, interview_to_final: rate, final_to_offer: rate, overall_success: rate }],
  [/^\/analytics\/timeline/, { buckets: [{ week_start: '2026-09-07', applications: 3, interviews: 1 }], sample_size: 4 }],
  [/^\/analytics\/rejections/, { by_stage: { OA: 2 }, no_response_count: 1, sample_size: 3, sufficient_data: false }],
  [/^\/preparation\/progress/, { categories: [{ category: 'dsa', total_topics: 2, tasks_total: 2, tasks_completed: 1, average_mastery: 0.6 }], topics: [{ topic_id: 't-1', topic_name: 'Arrays', slug: 'arrays', category: 'dsa', mastery_score: 0.6, observation_count: 4, sufficient_evidence: true }] }],
  [/^\/preparation\/tasks/, [{ id: 'task-1', topic_id: 't-1', topic_name: 'Arrays', title: 'Practise sliding windows', description: null, due_date: null, estimated_effort_minutes: null, status: 'open', completed_at: null, created_at: NOW, updated_at: NOW }]],
  [/^\/topics/, [{ id: 't-1', name: 'Arrays', slug: 'arrays', category: 'dsa', description: null, default_difficulty: 'medium' }]],
  [/^\/recommendations/, []],
  [/^\/reports/, []],
  [/^\/notifications\/unread-count/, { unread_count: 1 }],
  [/^\/notifications/, page([{ id: 'n-1', type: 'upcoming_interview', title: 'Interview tomorrow', body: 'Round 1 with Northwind Labs.', related_entity_type: 'interview', related_entity_id: 'int-1', is_read: false, read_at: null, created_at: NOW }])],
  [/^\/fl\/consent/, { opted_in: false }],
  [/^\/fl\/schema/, { schema_version: 1, feature_count: 1, features: [{ name: 'mastery_mean', lower_bound: 0, upper_bound: 1 }], not_collected: ['free text'] }],
  [/^\/fl\/privacy-ledger/, []],
]

function fixtureFor(path: string): unknown {
  return FIXTURES.find(([pattern]) => pattern.test(path))?.[1] ?? {}
}

const PAGES: [string, ComponentType, string][] = [
  ['applications', ApplicationsListPage, 'Northwind Labs'],
  ['resumes', ResumesListPage, 'Platform resume'],
  ['interviews', InterviewsListPage, 'Northwind Labs'],
  ['prep', PrepPage, 'Practise sliding windows'],
  ['analytics', AnalyticsPage, 'Applied → OA'],
  ['recommendations', RecommendationsPage, 'No recommendations yet'],
  ['reports', ReportsPage, 'No exports yet'],
  ['notifications', NotificationsPage, 'Interview tomorrow'],
  ['privacy', PrivacyPage, 'mastery_mean'],
  ['profile', AccountPage, 'Change password'],
]

describe.each(['light', 'dark'] as const)('inner pages in the %s theme', (theme) => {
  beforeEach(() => {
    localStorage.clear()
    localStorage.setItem(THEME_STORAGE_KEY, theme)
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'u-1', email: 'nina@example.com', display_name: 'Nina', target_role: null, graduation_year: null, target_date: null, onboarded: true, timezone: null, fl_opt_in: false, fl_privacy_posture: 'balanced', created_at: NOW },
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      confirmEmail: vi.fn(),
      logout: vi.fn(),
    } as unknown as ReturnType<typeof useAuth>)
  })

  it.each(PAGES)('renders %s with a page heading and, in the light theme, no critical a11y issues', async (_name, Page, marker) => {
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const { container } = render(
      <ThemeProvider>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>
            <Page />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>,
    )

    expect((await screen.findAllByText(marker)).length).toBeGreaterThan(0)
    expect(document.documentElement.classList.contains('dark')).toBe(theme === 'dark')
    expect(screen.getAllByRole('heading', { level: 2 }).length).toBeGreaterThan(0)
    if (theme === 'light') {
      expect(await criticalViolations(container)).toEqual([])
    }
  }, 20_000)
})
