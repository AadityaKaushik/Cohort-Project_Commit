import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApplicationsListPage } from '@/pages/applications/ApplicationsListPage'
import * as applicationsApi from '@/lib/applications-api'

vi.mock('@/lib/applications-api')

const row: applicationsApi.ApplicationSummary = {
  id: 'a1',
  company: { id: 'c1', name: 'Acme', website: null, industry: null },
  job_title: 'Engineer',
  status: 'APPLIED',
  current_stage: 'APPLIED',
  days_in_stage: 2,
  date_applied: '2026-01-05',
  priority: 'high',
  archived: false,
  allowed_next_statuses: [],
  correction_statuses: [],
}

function renderPage(initial = '/applications') {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[initial]}>
        <ApplicationsListPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('application filters', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.listApplications).mockReset()
    vi.mocked(applicationsApi.listApplications).mockResolvedValue({ items: [row], total: 1, page: 0, size: 25 })
  })

  it('sends the chosen status and priority to the API', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Acme')

    await user.selectOptions(screen.getByLabelText('Status'), 'APPLIED')
    await user.selectOptions(screen.getByLabelText('Priority'), 'high')

    await waitFor(() =>
      expect(applicationsApi.listApplications).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'APPLIED', priority: 'high' }),
      ),
    )
  })

  it('searches by text after a short pause', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByText('Acme')

    await user.type(screen.getByLabelText('Search company or role'), 'platform')

    await waitFor(() =>
      expect(applicationsApi.listApplications).toHaveBeenLastCalledWith(expect.objectContaining({ q: 'platform' })),
    )
  })

  it('shows a filter-specific empty state that can be cleared', async () => {
    vi.mocked(applicationsApi.listApplications).mockResolvedValue({ items: [], total: 0, page: 0, size: 25 })
    const user = userEvent.setup()
    renderPage('/applications?status=OFFER')

    expect(await screen.findByText('No applications match these filters')).toBeInTheDocument()
    await user.click(screen.getAllByRole('button', { name: 'Clear filters' })[0])

    await waitFor(() =>
      expect(applicationsApi.listApplications).toHaveBeenLastCalledWith(expect.objectContaining({ status: undefined })),
    )
  })
})
