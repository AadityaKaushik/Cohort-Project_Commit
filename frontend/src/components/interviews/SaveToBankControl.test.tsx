import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SaveToBankControl } from '@/components/interviews/SaveToBankControl'
import { saveInterviewQuestionToBank, type InterviewQuestion } from '@/lib/interviews-api'
import { listTopics } from '@/lib/prep-api'
import { ApiError } from '@/lib/api-client'

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, saveInterviewQuestionToBank: vi.fn() }
})
vi.mock('@/lib/prep-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/prep-api')>('@/lib/prep-api')
  return { ...actual, listTopics: vi.fn() }
})

function question(overrides: Partial<InterviewQuestion> = {}): InterviewQuestion {
  return {
    id: 'asked-1',
    interview_id: 'interview-1',
    question_text: 'Find the shortest route between two stations',
    topic_id: 'topic-graphs',
    category: 'dsa',
    difficulty: 'medium',
    self_rating: null,
    time_taken_minutes: null,
    linked_question_id: null,
    notes: null,
    created_at: '2026-10-01T00:00:00Z',
    updated_at: '2026-10-01T00:00:00Z',
    ...overrides,
  }
}

function renderControl(asked: InterviewQuestion) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <SaveToBankControl question={asked} interviewId="interview-1" />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('SaveToBankControl', () => {
  beforeEach(() => {
    vi.mocked(saveInterviewQuestionToBank).mockReset()
    vi.mocked(listTopics).mockResolvedValue([])
  })

  it('shows that a linked question is already in the bank', () => {
    renderControl(question({ linked_question_id: 'bank-1' }))

    expect(screen.getByRole('link', { name: /in your question bank/i })).toHaveAttribute('href', '/prep/questions')
    expect(screen.queryByRole('button', { name: /add to question bank/i })).not.toBeInTheDocument()
  })

  it('saves straight away when the question already has a topic', async () => {
    vi.mocked(saveInterviewQuestionToBank).mockResolvedValue({} as never)
    const user = userEvent.setup()
    renderControl(question())

    await user.click(screen.getByRole('button', { name: /add to question bank/i }))

    await waitFor(() => expect(saveInterviewQuestionToBank).toHaveBeenCalledWith('asked-1', null))
  })

  it('asks for a topic when the question has none', async () => {
    const user = userEvent.setup()
    renderControl(question({ topic_id: null }))

    await user.click(screen.getByRole('button', { name: /add to question bank/i }))

    expect(screen.getByRole('combobox', { name: 'Topic' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled()
    expect(saveInterviewQuestionToBank).not.toHaveBeenCalled()
  })

  it('shows the server message when the topic is missing', async () => {
    vi.mocked(saveInterviewQuestionToBank).mockRejectedValue(
      new ApiError(400, 'One or more fields are invalid.', 'VALIDATION_ERROR', { topicId: 'Pick a topic for this question.' }),
    )
    const user = userEvent.setup()
    renderControl(question())

    await user.click(screen.getByRole('button', { name: /add to question bank/i }))

    expect(await screen.findByText('Pick a topic for this question.')).toBeInTheDocument()
  })
})
