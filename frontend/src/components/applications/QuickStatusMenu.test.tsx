import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from '@/hooks/use-toast'
import { QuickStatusMenu } from '@/components/applications/QuickStatusMenu'
import { ApiError } from '@/lib/api-client'
import { changeApplicationStatus, type ApplicationSummary } from '@/lib/applications-api'

vi.mock('@/lib/applications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-api')>('@/lib/applications-api')
  return { ...actual, changeApplicationStatus: vi.fn() }
})

const application: ApplicationSummary = {
  id: 'app-1',
  company: { id: 'co-1', name: 'Acme Corp', website: null, industry: null },
  job_title: 'Backend Engineer',
  status: 'APPLIED',
  current_stage: 'APPLIED',
  days_in_stage: 3,
  date_applied: '2026-01-10',
  priority: 'high',
  archived: false,
  allowed_next_statuses: ['OA', 'REJECTED'],
  correction_statuses: [],
}

function renderMenu() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <ToastProvider>
        <QuickStatusMenu application={application} />
      </ToastProvider>
    </QueryClientProvider>,
  )
}

describe('QuickStatusMenu', () => {
  beforeEach(() => {
    vi.mocked(changeApplicationStatus).mockReset()
  })

  it('moves the application to the chosen status', async () => {
    vi.mocked(changeApplicationStatus).mockResolvedValue({ ...application, status: 'OA' } as never)
    renderMenu()

    await userEvent.setup().selectOptions(screen.getByLabelText(/change status of acme corp/i), 'OA')

    await waitFor(() => expect(changeApplicationStatus).toHaveBeenCalledWith('app-1', { to_status: 'OA' }))
    expect(await screen.findByText('Moved to OA')).toBeInTheDocument()
  })

  it("shows the server's reason when a move is refused", async () => {
    vi.mocked(changeApplicationStatus).mockRejectedValue(new ApiError(409, 'Add a note before rejecting an application.'))
    renderMenu()

    await userEvent.setup().selectOptions(screen.getByLabelText(/change status of acme corp/i), 'REJECTED')

    expect(await screen.findByText('Move refused')).toBeInTheDocument()
    expect(screen.getByText('Add a note before rejecting an application.')).toBeInTheDocument()
  })
})
