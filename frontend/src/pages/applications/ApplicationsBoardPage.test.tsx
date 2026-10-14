import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApplicationsBoardPage } from '@/pages/applications/ApplicationsBoardPage'
import { ToastProvider } from '@/hooks/use-toast'
import { ApiError } from '@/lib/api-client'
import * as applicationsApi from '@/lib/applications-api'

vi.mock('@/lib/applications-api')

const card: applicationsApi.ApplicationSummary = {
  id: 'a1',
  company: { id: 'c1', name: 'Acme', website: null, industry: null },
  job_title: 'Engineer',
  status: 'OA',
  current_stage: 'OA',
  days_in_stage: 4,
  date_applied: '2026-01-05',
  priority: null,
  archived: false,
  allowed_next_statuses: ['INTERVIEW', 'REJECTED'],
  correction_statuses: ['APPLIED'],
}

function renderBoard() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <ToastProvider>
        <MemoryRouter>
          <ApplicationsBoardPage />
        </MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>,
  )
}

async function drag(column: string) {
  const source = (await screen.findByText('Acme')).closest('li')!
  const target = screen.getByRole('region', { name: `${column} column` })
  const dataTransfer = { setData: vi.fn(), getData: () => 'a1', effectAllowed: '' }
  fireEvent.dragStart(source, { dataTransfer })
  fireEvent.dragOver(target, { dataTransfer })
  fireEvent.drop(target, { dataTransfer })
}

describe('ApplicationsBoardPage', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.listApplications).mockResolvedValue({ items: [card], total: 1, page: 0, size: 500 })
    vi.mocked(applicationsApi.changeApplicationStatus).mockReset()
  })

  it('places each application in its stage column', async () => {
    renderBoard()

    const column = await screen.findByRole('region', { name: 'OA column' })
    expect(within(column).getByText('Acme')).toBeInTheDocument()
  })

  it('persists a legal drop through the status endpoint', async () => {
    vi.mocked(applicationsApi.changeApplicationStatus).mockResolvedValue({
      status: 'INTERVIEW',
    } as applicationsApi.ApplicationDetail)
    renderBoard()

    await drag('Interview')
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Move without interview' }))

    await waitFor(() =>
      expect(applicationsApi.changeApplicationStatus).toHaveBeenCalledWith('a1', {
        to_status: 'INTERVIEW',
        note: undefined,
      }),
    )
    expect(await screen.findByText('Moved to Interview')).toBeInTheDocument()
  })

  it('refuses an illegal drop, keeps the card in place and says why', async () => {
    renderBoard()

    await drag('Accepted')

    expect(await screen.findByText('An application cannot move from OA to Accepted.')).toBeInTheDocument()
    expect(applicationsApi.changeApplicationStatus).not.toHaveBeenCalled()
    expect(within(screen.getByRole('region', { name: 'OA column' })).getByText('Acme')).toBeInTheDocument()
  })

  it('asks for a note before a backward move', async () => {
    vi.mocked(applicationsApi.changeApplicationStatus).mockResolvedValue({
      status: 'APPLIED',
    } as applicationsApi.ApplicationDetail)
    const user = userEvent.setup()
    renderBoard()

    await drag('Applied')
    expect(applicationsApi.changeApplicationStatus).not.toHaveBeenCalled()
    await user.type(await screen.findByLabelText('Correction note'), 'logged too early')
    await user.click(screen.getByRole('button', { name: 'Move back' }))

    await waitFor(() =>
      expect(applicationsApi.changeApplicationStatus).toHaveBeenCalledWith('a1', {
        to_status: 'APPLIED',
        note: 'logged too early',
      }),
    )
  })

  it('shows the server reason when the server refuses a move', async () => {
    vi.mocked(applicationsApi.changeApplicationStatus).mockRejectedValue(
      new ApiError(409, 'An application cannot move from OA to Interview.', 'INVALID_STATUS_TRANSITION'),
    )
    renderBoard()

    await drag('Interview')
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Move without interview' }))

    expect(await screen.findByText('An application cannot move from OA to Interview.')).toBeInTheDocument()
  })
})
