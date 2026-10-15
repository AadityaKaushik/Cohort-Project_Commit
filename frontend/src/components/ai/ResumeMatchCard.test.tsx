import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ResumeMatchCard } from '@/components/ai/ResumeMatchCard'
import * as aiApi from '@/lib/ai-api'
import * as applicationsApi from '@/lib/applications-api'

vi.mock('@/lib/ai-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/ai-api')>('@/lib/ai-api')
  return {
    ...actual,
    getAiStatus: vi.fn().mockResolvedValue({ provider: 'ollama', hosted: false }),
    getResumeMatch: vi.fn(),
    createCoverLetterDraft: vi.fn(),
  }
})
vi.mock('@/lib/applications-api')

const match: aiApi.ResumeMatch = {
  has_job_description: true,
  has_resume: true,
  required: ['Java', 'Docker'],
  matched: ['Java'],
  missing: ['Docker'],
  score: 50,
  explanation: 'Your resume covers 1 of 2 skills named in the posting (50%).',
  bullets: [{ text: 'Built a Java service', keywords: ['Java'], source: 'Developer' }],
}

function renderCard() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ResumeMatchCard applicationId="a1" />
    </QueryClientProvider>,
  )
}

describe('ResumeMatchCard', () => {
  beforeEach(() => {
    vi.mocked(aiApi.getResumeMatch).mockResolvedValue(match)
    vi.mocked(aiApi.createCoverLetterDraft).mockReset()
    vi.mocked(applicationsApi.updateApplication).mockReset()
  })

  it('shows score, matched and missing skills and the bullets', async () => {
    renderCard()
    expect(await screen.findByText('50%', { selector: 'p' })).toBeInTheDocument()
    expect(screen.getByText('Docker')).toBeInTheDocument()
    expect(screen.getByText(/Built a Java service/)).toBeInTheDocument()
  })

  it('says AI is unavailable and still shows the template draft', async () => {
    vi.mocked(aiApi.createCoverLetterDraft).mockResolvedValue({ draft: 'Dear hiring team,', source: 'template', ai_available: false })
    renderCard()
    await userEvent.click(await screen.findByRole('button', { name: 'Draft a cover letter' }))
    expect(await screen.findByText(/AI is unavailable, try again later/)).toBeInTheDocument()
    expect(screen.getByLabelText('Cover letter draft')).toHaveValue('Dear hiring team,')
  })

  it('saves the edited draft to the application cover letter', async () => {
    vi.mocked(aiApi.createCoverLetterDraft).mockResolvedValue({ draft: 'Hello', source: 'model', ai_available: true })
    vi.mocked(applicationsApi.updateApplication).mockResolvedValue({} as applicationsApi.ApplicationDetail)
    renderCard()
    await userEvent.click(await screen.findByRole('button', { name: 'Draft a cover letter' }))
    const box = await screen.findByLabelText('Cover letter draft')
    await userEvent.type(box, ' there')
    await userEvent.click(screen.getByRole('button', { name: 'Save to application' }))
    expect(applicationsApi.updateApplication).toHaveBeenCalledWith('a1', { cover_letter: 'Hello there' })
  })

  it('shows the Gemini disclosure when the hosted provider is active', async () => {
    vi.mocked(aiApi.getAiStatus).mockResolvedValueOnce({ provider: 'gemini', hosted: true })
    renderCard()
    expect(await screen.findByText('Gemini is active')).toBeInTheDocument()
  })
})
