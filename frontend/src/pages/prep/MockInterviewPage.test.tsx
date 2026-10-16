import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { MockInterviewPage } from '@/pages/prep/MockInterviewPage'
import * as prepApi from '@/lib/prep-api'

vi.mock('@/lib/prep-api')

function question(id: string, text: string, topic: string): prepApi.MockInterviewQuestion {
  return {
    question_id: id,
    question_text: text,
    topic_id: `t-${id}`,
    topic_name: topic,
    difficulty: 'medium',
    mastery_score: 0.4,
    reason: 'Mastery 40%',
  }
}

function renderPage() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <MockInterviewPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('MockInterviewPage', () => {
  beforeEach(() => {
    vi.resetAllMocks()
    vi.mocked(prepApi.getMockInterview).mockResolvedValue({
      questions: [question('q1', 'Detect a cycle', 'Graphs'), question('q2', 'Explain a heap', 'Heaps')],
    })
    vi.mocked(prepApi.recordPracticeAttempt).mockResolvedValue({} as prepApi.PracticeAttempt)
  })

  it('shows the start screen with the choices', () => {
    renderPage()

    expect(screen.getByRole('group', { name: 'Number of questions' })).toBeInTheDocument()
    for (const label of ['3', '5', '8']) {
      expect(within(screen.getByRole('group', { name: 'Number of questions' })).getByRole('button', { name: label })).toBeInTheDocument()
    }
    for (const label of ['5', '10', '15']) {
      expect(within(screen.getByRole('group', { name: 'Minutes per question' })).getByRole('button', { name: label })).toBeInTheDocument()
    }
    expect(screen.getByRole('button', { name: 'Start' })).toBeInTheDocument()
  })

  it('posts the attempt for a rating and moves to the next question', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(within(screen.getByRole('group', { name: 'Number of questions' })).getByRole('button', { name: '3' }))
    await user.click(screen.getByRole('button', { name: 'Start' }))
    expect(prepApi.getMockInterview).toHaveBeenCalledWith(3)

    expect(await screen.findByText('Detect a cycle')).toBeInTheDocument()
    await user.type(screen.getByLabelText('Notes'), 'Used a visited set')
    await user.click(screen.getByRole('button', { name: /^2/ }))
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(prepApi.recordPracticeAttempt).toHaveBeenCalledWith('q1', {
      status: 'needs_review',
      confidence: 2,
      time_taken_minutes: 1,
      notes: 'Used a visited set',
    })
    expect(await screen.findByText('Explain a heap')).toBeInTheDocument()
  })

  it('maps ratings to statuses and shows the summary at the end', async () => {
    const user = userEvent.setup()
    renderPage()

    await user.click(screen.getByRole('button', { name: 'Start' }))
    await screen.findByText('Detect a cycle')
    await user.click(screen.getByRole('button', { name: /^3/ }))
    await user.click(screen.getByRole('button', { name: 'Next' }))
    await screen.findByText('Explain a heap')
    await user.click(screen.getByRole('button', { name: /^5/ }))
    await user.click(screen.getByRole('button', { name: 'Finish' }))

    expect(prepApi.recordPracticeAttempt).toHaveBeenNthCalledWith(1, 'q1', expect.objectContaining({ status: 'in_progress', confidence: 3, notes: null }))
    expect(prepApi.recordPracticeAttempt).toHaveBeenNthCalledWith(2, 'q2', expect.objectContaining({ status: 'confident', confidence: 5 }))

    const table = await screen.findByRole('table')
    const rows = within(table).getAllByRole('row')
    expect(rows).toHaveLength(3)
    expect(within(rows[1]).getByText('Detect a cycle')).toBeInTheDocument()
    expect(within(rows[1]).getByText('Graphs')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Heaps')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to Interview Prep' })).toHaveAttribute('href', '/prep')
  })
})
