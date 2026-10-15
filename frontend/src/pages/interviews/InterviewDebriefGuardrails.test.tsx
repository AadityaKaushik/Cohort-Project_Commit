import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InterviewDebriefPage } from '@/pages/interviews/InterviewDebriefPage'
import { getInterview, getInterviewFeedbackOrNull, upsertInterviewFeedback, type InterviewDetail } from '@/lib/interviews-api'

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, getInterview: vi.fn(), getInterviewFeedbackOrNull: vi.fn(), upsertInterviewFeedback: vi.fn() }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/interviews/interview-1/debrief']}>
        <Routes>
          <Route path="/interviews/:id/debrief" element={<InterviewDebriefPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const interview = {
  id: 'interview-1',
  application_id: 'app-1',
  company: { id: 'co-1', name: 'Acme Corp', website: null, industry: null },
  job_title: 'Backend Engineer',
  scheduled_at: '2026-02-01T10:00:00Z',
  completed_at: '2026-02-01T11:00:00Z',
  round_number: 2,
  round_type: 'TECHNICAL',
  duration_minutes: 60,
  mode: 'video',
  interviewer_name: null,
  interviewer_role: null,
  outcome: 'pending',
  perceived_difficulty: null,
  notes: null,
  has_feedback: false,
  created_at: '2026-01-10T00:00:00Z',
  updated_at: '2026-01-10T00:00:00Z',
} as InterviewDetail

const saved = {
  id: 'feedback-1',
  interview_id: 'interview-1',
  self_rating: null,
  what_went_well: null,
  what_went_poorly: null,
  improvement_areas: null,
  external_feedback: null,
  is_draft: false,
  submitted_at: null,
  created_at: '2026-02-01T00:00:00Z',
  updated_at: '2026-02-01T00:00:00Z',
}

describe('debrief input guardrails', () => {
  beforeEach(() => {
    vi.mocked(getInterview).mockReset().mockResolvedValue(interview)
    vi.mocked(getInterviewFeedbackOrNull).mockReset().mockResolvedValue(null)
    vi.mocked(upsertInterviewFeedback).mockReset().mockResolvedValue(saved)
  })

  it('blocks keyboard mash with a plain message and does not call the API', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(await screen.findByLabelText('What went well?'), 'asdfghjkl')
    await user.click(screen.getByRole('button', { name: 'Submit debrief' }))

    expect(await screen.findByText(/looks like random typing/)).toBeInTheDocument()
    expect(upsertInterviewFeedback).not.toHaveBeenCalled()
  })

  it('lets a short legitimate answer through without a warning', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(await screen.findByLabelText('What went poorly?'), 'Rejected after round 2')
    await user.click(screen.getByRole('button', { name: 'Submit debrief' }))

    await waitFor(() => expect(upsertInterviewFeedback).toHaveBeenCalledTimes(1))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  it('shows a soft warning for off-topic text and saves after Save anyway', async () => {
    const user = userEvent.setup()
    renderPage()
    await user.type(await screen.findByLabelText('What went well?'), 'the tea there was not good')
    await user.click(screen.getByRole('button', { name: 'Submit debrief' }))

    expect(await screen.findByText(/does not look like it is about the interview/)).toBeInTheDocument()
    expect(upsertInterviewFeedback).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Save anyway' }))
    await waitFor(() => expect(upsertInterviewFeedback).toHaveBeenCalledTimes(1))
    expect(vi.mocked(upsertInterviewFeedback).mock.calls[0][1].what_went_well).toBe('the tea there was not good')
  })

  it('Edit hides the warning and returns focus to the field', async () => {
    const user = userEvent.setup()
    renderPage()
    const field = await screen.findByLabelText('What went well?')
    await user.type(field, 'the tea there was not good')
    await user.tab()

    await user.click(await screen.findByRole('button', { name: 'Edit' }))
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(field).toHaveFocus()
  })
})
