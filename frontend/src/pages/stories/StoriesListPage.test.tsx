import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StoriesListPage } from '@/pages/stories/StoriesListPage'
import { listStories, type Story } from '@/lib/stories-api'

vi.mock('@/lib/stories-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/stories-api')>('@/lib/stories-api')
  return { ...actual, listStories: vi.fn() }
})

function story(overrides: Partial<Story> = {}): Story {
  return {
    id: 's-1',
    title: 'Led the billing migration',
    situation: null,
    task: null,
    action: null,
    result: 'Cut failed invoices by 40 percent.\nSecond line.',
    competencies: ['leadership'],
    interview_id: null,
    created_at: '2026-09-01T00:00:00Z',
    updated_at: '2026-09-01T00:00:00Z',
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <StoriesListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('StoriesListPage', () => {
  beforeEach(() => {
    vi.mocked(listStories).mockReset()
  })

  it('shows the title, tags and first line of the result', async () => {
    vi.mocked(listStories).mockResolvedValue([story()])
    renderPage()

    expect(await screen.findByText('Led the billing migration')).toBeInTheDocument()
    expect(screen.getByText('Cut failed invoices by 40 percent.')).toBeInTheDocument()
    expect(screen.queryByText(/Second line/)).not.toBeInTheDocument()
  })

  it('asks the server for the chosen competency', async () => {
    vi.mocked(listStories).mockImplementation(async (competency) =>
      competency === 'teamwork' ? [story({ id: 's-2', title: 'Team story', competencies: ['teamwork'] })] : [story()],
    )
    renderPage()
    await screen.findByText('Led the billing migration')

    await userEvent.click(screen.getByRole('button', { name: 'Teamwork' }))

    expect(await screen.findByText('Team story')).toBeInTheDocument()
    await waitFor(() => expect(listStories).toHaveBeenLastCalledWith('teamwork'))
    expect(screen.getByRole('button', { name: 'Teamwork' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('offers one button to add a story when there are none', async () => {
    vi.mocked(listStories).mockResolvedValue([])
    renderPage()

    expect(await screen.findByRole('link', { name: 'Add a story' })).toHaveAttribute('href', '/stories/new')
  })
})
