import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StoryFormPage } from '@/pages/stories/StoryFormPage'
import { createStory, type Story } from '@/lib/stories-api'
import { ApiError } from '@/lib/api-client'

vi.mock('@/lib/stories-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/stories-api')>('@/lib/stories-api')
  return { ...actual, createStory: vi.fn() }
})

vi.mock('@/lib/interviews-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/interviews-api')>('@/lib/interviews-api')
  return { ...actual, listInterviews: vi.fn().mockResolvedValue({ items: [], total: 0, page: 0, size: 100 }) }
})

const saved: Story = {
  id: 's-1',
  title: 'Led the billing migration',
  situation: null,
  task: null,
  action: null,
  result: null,
  competencies: [],
  interview_id: null,
  created_at: '2026-09-01T00:00:00Z',
  updated_at: '2026-09-01T00:00:00Z',
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/stories/new']}>
        <Routes>
          <Route path="/stories/new" element={<StoryFormPage />} />
          <Route path="/stories/:id" element={<p>Story saved</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

// Typing four long STAR paragraphs keystroke by keystroke is slow in a full parallel run.
describe('StoryFormPage', { timeout: 40000 }, () => {
  beforeEach(() => {
    vi.mocked(createStory).mockReset()
  })

  it('sends the title, the four parts and the ticked competencies', async () => {
    vi.mocked(createStory).mockResolvedValue(saved)
    renderPage()

    await userEvent.type(screen.getByLabelText('Title'), 'Led the billing migration')
    await userEvent.type(screen.getByLabelText('Situation'), 'Billing ran on a legacy cron job.')
    await userEvent.type(screen.getByLabelText('Task'), 'Move it to the new queue.')
    await userEvent.type(screen.getByLabelText('Action'), 'I split the work and wrote the rollout plan.')
    await userEvent.type(screen.getByLabelText('Result'), 'Failed invoices dropped by 40 percent.')
    await userEvent.click(screen.getByLabelText('Leadership'))
    await userEvent.click(screen.getByLabelText('Problem Solving'))
    await userEvent.click(screen.getByRole('button', { name: 'Save story' }))

    await waitFor(() => expect(createStory).toHaveBeenCalledTimes(1))
    expect(vi.mocked(createStory).mock.calls[0][0]).toEqual({
      title: 'Led the billing migration',
      situation: 'Billing ran on a legacy cron job.',
      task: 'Move it to the new queue.',
      action: 'I split the work and wrote the rollout plan.',
      result: 'Failed invoices dropped by 40 percent.',
      competencies: ['leadership', 'problem_solving'],
      interview_id: undefined,
    })
    expect(await screen.findByText('Story saved')).toBeInTheDocument()
  }, 20000)

  it('does not submit without a title', async () => {
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: 'Save story' }))

    expect(await screen.findByText(/title/i, { selector: 'p' })).toBeInTheDocument()
    expect(createStory).not.toHaveBeenCalled()
  })

  it('shows server field errors under the field', async () => {
    vi.mocked(createStory).mockRejectedValue(
      new ApiError(400, 'Validation failed.', 'VALIDATION_FAILED', { result: 'Write a few real words, or leave this blank.' }),
    )
    renderPage()

    await userEvent.type(screen.getByLabelText('Title'), 'A story')
    await userEvent.click(screen.getByRole('button', { name: 'Save story' }))

    const message = await screen.findByText('Write a few real words, or leave this blank.')
    expect(screen.getByLabelText('Result')).toHaveAttribute('aria-describedby', message.id)
    expect(screen.getByLabelText('Result')).toHaveAttribute('aria-invalid', 'true')
  })
})
