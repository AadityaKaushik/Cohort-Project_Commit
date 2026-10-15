import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { QuestionBankPage } from '@/pages/prep/QuestionBankPage'
import { ToastProvider } from '@/hooks/use-toast'
import * as prepApi from '@/lib/prep-api'

vi.mock('@/lib/prep-api')

const question: prepApi.QuestionBankItem = {
  id: 'q1',
  topic_id: 't1',
  topic_name: 'Graphs',
  question_text: 'Explain breadth-first search.',
  category: 'dsa',
  difficulty: 'medium',
  is_system: true,
  source_question_id: null,
  created_at: '',
  updated_at: '',
}

function progress(score: number, observations: number): prepApi.PreparationProgress {
  return {
    categories: [],
    topics: [
      {
        topic_id: 't1',
        topic_name: 'Graphs',
        slug: 'graphs',
        category: 'dsa',
        mastery_score: score,
        observation_count: observations,
        sufficient_evidence: observations >= 2,
      },
    ],
  }
}

function renderPage(path = '/prep/questions') {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ToastProvider>
        <MemoryRouter initialEntries={[path]}>
          <QuestionBankPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('QuestionBankPage', () => {
  beforeEach(() => {
    vi.mocked(prepApi.listQuestions).mockReset()
    vi.mocked(prepApi.listQuestions).mockResolvedValue({ items: [question], total: 1, page: 0, size: 20 })
    vi.mocked(prepApi.listTopics).mockResolvedValue([
      { id: 't1', name: 'Graphs', slug: 'graphs', category: 'dsa', description: null, default_difficulty: 'medium', taxonomy_version: 1, created_at: '' },
    ])
    vi.mocked(prepApi.getPreparationProgress).mockResolvedValue(progress(0, 0))
    vi.mocked(prepApi.recordPracticeAttempt).mockReset()
  })

  it('lists questions with readable labels and filters by the topic in the URL', async () => {
    renderPage('/prep/questions?topic_id=t1')

    expect(await screen.findByText('Explain breadth-first search.')).toBeInTheDocument()
    const item = within(screen.getByRole('list'))
    expect(item.getByText('DSA')).toBeInTheDocument()
    expect(item.getByText('Medium')).toBeInTheDocument()
    expect(prepApi.listQuestions).toHaveBeenCalledWith(expect.objectContaining({ topic_id: 't1' }))
  })

  it('logs an attempt and reports the updated topic mastery', async () => {
    vi.mocked(prepApi.recordPracticeAttempt).mockResolvedValue({} as prepApi.PracticeAttempt)
    vi.mocked(prepApi.getPreparationProgress)
      .mockResolvedValueOnce(progress(0, 0))
      .mockResolvedValue(progress(0.82, 2))
    const user = userEvent.setup()
    renderPage()

    await user.click(await screen.findByRole('button', { name: 'Log attempt' }))
    const dialog = await screen.findByRole('dialog')
    await user.selectOptions(within(dialog).getByLabelText('Confidence'), '5')
    await user.click(within(dialog).getByRole('button', { name: 'Log attempt' }))

    await waitFor(() =>
      expect(prepApi.recordPracticeAttempt).toHaveBeenCalledWith(
        'q1',
        expect.objectContaining({ status: 'in_progress', confidence: 5 }),
      ),
    )
    expect(await screen.findByText('Graphs mastery is now 82%.')).toBeInTheDocument()
  })

  it('opens the attempt dialog for the question named in the practise param', async () => {
    vi.mocked(prepApi.getQuestion).mockResolvedValue(question)
    renderPage('/prep/questions?topic_id=t1&practise=q1')

    const dialog = await screen.findByRole('dialog')
    expect(prepApi.getQuestion).toHaveBeenCalledWith('q1')
    expect(within(dialog).getByRole('button', { name: 'Log attempt' })).toBeInTheDocument()
  })

  it('shows an empty state when nothing matches', async () => {
    vi.mocked(prepApi.listQuestions).mockResolvedValue({ items: [], total: 0, page: 0, size: 20 })
    renderPage()

    expect(await screen.findByText('No questions match')).toBeInTheDocument()
  })
})
