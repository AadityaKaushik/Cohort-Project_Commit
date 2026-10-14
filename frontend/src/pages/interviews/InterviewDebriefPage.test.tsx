import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { InterviewDebriefPage } from '@/pages/interviews/InterviewDebriefPage'
import {
  getInterview,
  getInterviewFeedbackOrNull,
  upsertInterviewFeedback,
  type InterviewDetail,
} from '@/lib/interviews-api'

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return {
    ...actual,
    getInterview: vi.fn(),
    getInterviewFeedbackOrNull: vi.fn(),
    upsertInterviewFeedback: vi.fn(),
  }
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

function sampleInterview(): InterviewDetail {
  return {
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
  }
}

describe('InterviewDebriefPage', () => {
  beforeEach(() => {
    vi.mocked(getInterview).mockReset()
    vi.mocked(getInterviewFeedbackOrNull).mockReset()
    vi.mocked(upsertInterviewFeedback).mockReset()
    window.localStorage.clear()
  })

  it('renders a single-screen guided form with no existing feedback', async () => {
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue(null)

    renderPage()

    expect(await screen.findByText('Post-interview debrief')).toBeInTheDocument()
    expect(screen.getByText(/Round 2/)).toBeInTheDocument()
    expect(screen.getByLabelText('What went well?')).toBeInTheDocument()
    expect(screen.getByLabelText('What went poorly?')).toBeInTheDocument()
    expect(screen.getByLabelText('What would you improve next time?')).toBeInTheDocument()
    expect(screen.queryByText('Draft in progress')).not.toBeInTheDocument()
  })

  it('completing it in one pass rates, writes one note, and submits — the whole flow is a handful of clicks', async () => {
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue(null)
    vi.mocked(upsertInterviewFeedback).mockResolvedValue({
      id: 'feedback-1',
      interview_id: 'interview-1',
      self_rating: 4,
      what_went_well: 'Explained my approach clearly',
      what_went_poorly: null,
      improvement_areas: null,
      external_feedback: null,
      is_draft: false,
      submitted_at: '2026-02-01T12:00:00Z',
      created_at: '2026-02-01T11:59:00Z',
      updated_at: '2026-02-01T12:00:00Z',
    })

    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Post-interview debrief')

    await user.click(screen.getByRole('radio', { name: '4 out of 5' }))
    await user.type(screen.getByLabelText('What went well?'), 'Explained my approach clearly')
    await user.click(screen.getByRole('button', { name: /submit debrief/i }))

    await waitFor(() =>
      expect(upsertInterviewFeedback).toHaveBeenCalledWith('interview-1', {
        self_rating: 4,
        what_went_well: 'Explained my approach clearly',
        what_went_poorly: null,
        improvement_areas: null,
        is_draft: false,
      }),
    )

    expect(await screen.findByText('Debrief saved')).toBeInTheDocument()
  })

  it('saving as a draft does not show the completion screen and marks is_draft true', async () => {
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue(null)
    vi.mocked(upsertInterviewFeedback).mockResolvedValue({
      id: 'feedback-1',
      interview_id: 'interview-1',
      self_rating: null,
      what_went_well: null,
      what_went_poorly: null,
      improvement_areas: null,
      external_feedback: null,
      is_draft: true,
      submitted_at: null,
      created_at: '2026-02-01T11:59:00Z',
      updated_at: '2026-02-01T11:59:00Z',
    })

    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Post-interview debrief')
    await user.click(screen.getByRole('button', { name: /save draft/i }))

    await waitFor(() => expect(upsertInterviewFeedback).toHaveBeenCalledWith('interview-1', expect.objectContaining({ is_draft: true })))
    expect(screen.queryByText('Debrief saved')).not.toBeInTheDocument()
  })

  it('pre-fills an in-progress draft and flags it as one', async () => {
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue({
      id: 'feedback-1',
      interview_id: 'interview-1',
      self_rating: 3,
      what_went_well: 'Good start',
      what_went_poorly: null,
      improvement_areas: null,
      external_feedback: null,
      is_draft: true,
      submitted_at: null,
      created_at: '2026-02-01T11:59:00Z',
      updated_at: '2026-02-01T11:59:00Z',
    })

    renderPage()

    expect(await screen.findByText('Draft in progress')).toBeInTheDocument()
    expect(screen.getByDisplayValue('Good start')).toBeInTheDocument()
  })

  it('keeps an unsent debrief in local storage and restores it after a reload', async () => {
    const user = userEvent.setup()
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue(null)

    const first = renderPage()
    await user.type(await screen.findByLabelText('What went well?'), 'Clear answers')
    first.unmount()

    renderPage()
    expect(await screen.findByLabelText('What went well?')).toHaveValue('Clear answers')
  })

  it('still works when local storage is unavailable', async () => {
    const getItem = vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('blocked')
    })
    vi.mocked(getInterview).mockResolvedValue(sampleInterview())
    vi.mocked(getInterviewFeedbackOrNull).mockResolvedValue(null)

    renderPage()
    expect(await screen.findByLabelText('What went well?')).toHaveValue('')
    getItem.mockRestore()
  })
})
