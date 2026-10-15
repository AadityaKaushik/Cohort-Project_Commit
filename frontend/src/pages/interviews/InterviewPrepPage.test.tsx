import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { InterviewPrepPage } from '@/pages/interviews/InterviewPrepPage'
import * as aiApi from '@/lib/ai-api'

vi.mock('@/lib/ai-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/ai-api')>('@/lib/ai-api')
  return {
    ...actual,
    getAiStatus: vi.fn().mockResolvedValue({ provider: 'ollama', hosted: false }),
    getPrepPack: vi.fn(),
    createPrepSummary: vi.fn(),
  }
})

const pack: aiApi.PrepPack = {
  interview_id: 'i2',
  company_name: 'Acme',
  job_title: 'Engineer',
  round_number: 2,
  round_type: 'TECHNICAL',
  scheduled_at: null,
  interviewer_name: 'Sam',
  interviewer_role: null,
  company_notes: ['Review the payments service'],
  past_rounds: [
    {
      round_number: 1,
      round_type: 'PHONE_SCREEN',
      job_title: 'Engineer',
      outcome: 'passed',
      went_well: null,
      went_poorly: null,
      improvement_areas: 'Explain trade offs aloud',
    },
  ],
  questions_asked: [{ text: 'Design a rate limiter', category: 'technical', round_number: 1 }],
  weak_topics: [{ name: 'Graphs', has_evidence: false, mastery_percent: 0 }],
  resume_highlights: ['Built a Java service'],
  readiness_score: null,
  readiness_formatted: 'insufficient data',
}

function renderPage() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={['/interviews/i2/prep']}>
        <Routes>
          <Route path="/interviews/:id/prep" element={<InterviewPrepPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('InterviewPrepPage', () => {
  beforeEach(() => {
    vi.mocked(aiApi.getPrepPack).mockResolvedValue(pack)
    vi.mocked(aiApi.createPrepSummary).mockReset()
  })

  it('lists the pieces assembled from the user data', async () => {
    renderPage()
    expect(await screen.findByText('Prep pack: Acme')).toBeInTheDocument()
    expect(screen.getByText('Graphs')).toBeInTheDocument()
    expect(screen.getByText(/Explain trade offs aloud/)).toBeInTheDocument()
    expect(screen.getByText(/Design a rate limiter/)).toBeInTheDocument()
    expect(screen.getByText('Review the payments service')).toBeInTheDocument()
    expect(screen.getByText(/not enough practice yet/)).toBeInTheDocument()
  })

  it('says AI is unavailable and keeps the factual fallback summary', async () => {
    vi.mocked(aiApi.createPrepSummary).mockResolvedValue({
      draft: 'Round 2 at Acme for Engineer.',
      source: 'template',
      ai_available: false,
    })
    renderPage()
    await userEvent.click(await screen.findByRole('button', { name: 'Write a summary' }))
    expect(await screen.findByText(/AI is unavailable, try again later/)).toBeInTheDocument()
    expect(screen.getByLabelText('Prep summary')).toHaveValue('Round 2 at Acme for Engineer.')
  })

  it('shows the Gemini disclosure when the hosted provider is active', async () => {
    vi.mocked(aiApi.getAiStatus).mockResolvedValueOnce({ provider: 'gemini', hosted: true })
    renderPage()
    expect(await screen.findByText('Gemini is active')).toBeInTheDocument()
  })
})
