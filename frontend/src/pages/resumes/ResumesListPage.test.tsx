import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ResumesListPage } from '@/pages/resumes/ResumesListPage'
import { listResumes, type ResumeSummary } from '@/lib/resumes-api'
import type { PagedResponse } from '@/lib/applications-api'

vi.mock('@/lib/resumes-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/resumes-api')>('@/lib/resumes-api')
  return { ...actual, listResumes: vi.fn() }
})

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/resumes']}>
        <ResumesListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function sampleResume(overrides: Partial<ResumeSummary> = {}): ResumeSummary {
  return {
    id: 'resume-1',
    name: 'Backend Resume',
    version_label: 'v1',
    target_role: 'Backend Engineer',
    is_default: false,
    has_file: true,
    parent_resume_id: null,
    created_at: '2026-01-10T00:00:00Z',
    updated_at: '2026-01-10T00:00:00Z',
    ...overrides,
  }
}

describe('ResumesListPage', () => {
  beforeEach(() => {
    vi.mocked(listResumes).mockReset()
  })

  it('shows a loading state before data arrives', () => {
    vi.mocked(listResumes).mockReturnValue(new Promise(() => {}))
    renderPage()
    expect(screen.getAllByText('Resumes')[0]).toBeInTheDocument()
  })

  it('shows an empty state with a create action when there are no resumes', async () => {
    const empty: PagedResponse<ResumeSummary> = { items: [], total: 0, page: 0, size: 25 }
    vi.mocked(listResumes).mockResolvedValue(empty)

    renderPage()

    expect(await screen.findByText('No resumes yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /create your first resume/i })).toBeInTheDocument()
  })

  it('shows an error state with a retry action when the request fails', async () => {
    vi.mocked(listResumes).mockRejectedValue(new Error('network down'))

    renderPage()

    expect(await screen.findByText(/couldn't load your resumes/i)).toBeInTheDocument()
    const retryButton = screen.getByRole('button', { name: /try again/i })

    vi.mocked(listResumes).mockResolvedValue({ items: [], total: 0, page: 0, size: 25 })
    await userEvent.setup().click(retryButton)

    await waitFor(() => expect(listResumes).toHaveBeenCalledTimes(2))
  })

  it('renders a row per resume once data loads, flagging the default', async () => {
    const page: PagedResponse<ResumeSummary> = {
      items: [sampleResume(), sampleResume({ id: 'resume-2', name: 'Frontend Resume', is_default: true, has_file: false })],
      total: 2,
      page: 0,
      size: 25,
    }
    vi.mocked(listResumes).mockResolvedValue(page)

    renderPage()

    expect(await screen.findByText('Backend Resume')).toBeInTheDocument()
    expect(screen.getByText('Frontend Resume')).toBeInTheDocument()
    expect(screen.getByText('Default')).toBeInTheDocument()
    expect(screen.getByText('Uploaded')).toBeInTheDocument()
    expect(screen.getByText('None yet')).toBeInTheDocument()
  })
})
