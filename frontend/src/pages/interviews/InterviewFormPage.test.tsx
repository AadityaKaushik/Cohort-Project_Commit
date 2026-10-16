import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InterviewFormPage } from '@/pages/interviews/InterviewFormPage'
import { getInterview, updateInterview, type InterviewDetail } from '@/lib/interviews-api'
import { ApiError } from '@/lib/api-client'

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, getInterview: vi.fn(), updateInterview: vi.fn() }
})

const interview: InterviewDetail = {
  id: 'interview-1',
  application_id: 'app-1',
  company: { id: 'company-1', name: 'Acme Corp', website: null, industry: null },
  job_title: 'Backend Engineer',
  scheduled_at: '2026-11-02T10:00:00Z',
  completed_at: null,
  round_number: 1,
  round_type: 'TECHNICAL',
  duration_minutes: 60,
  mode: 'onsite',
  interviewer_name: null,
  interviewer_role: null,
  outcome: 'pending',
  perceived_difficulty: null,
  notes: null,
  has_feedback: false,
  created_at: '2026-10-01T00:00:00Z',
  updated_at: '2026-10-01T00:00:00Z',
}

function renderEditPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/interviews/interview-1/edit']}>
        <Routes>
          <Route path="/interviews/:id/edit" element={<InterviewFormPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('InterviewFormPage', () => {
  beforeEach(() => {
    vi.mocked(getInterview).mockReset()
    vi.mocked(updateInterview).mockReset()
  })

  it('shows the server error for the scheduled date under that field', async () => {
    vi.mocked(getInterview).mockResolvedValue(interview)
    vi.mocked(updateInterview).mockRejectedValue(
      new ApiError(400, 'One or more fields are invalid.', 'VALIDATION_ERROR', {
        scheduledAt: 'Enter a date from the year 2000 onwards and no more than a year ahead.',
      }),
    )
    const user = userEvent.setup()
    renderEditPage()

    await user.click(await screen.findByRole('button', { name: /save|record interview/i }))

    expect(
      await screen.findByText('Enter a date from the year 2000 onwards and no more than a year ahead.'),
    ).toBeInTheDocument()
  })
})
