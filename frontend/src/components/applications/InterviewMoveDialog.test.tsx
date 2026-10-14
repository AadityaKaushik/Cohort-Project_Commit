import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { addDays, format } from 'date-fns'
import { InterviewMoveDialog } from '@/components/applications/InterviewMoveDialog'
import * as applicationsApi from '@/lib/applications-api'
import * as interviewsApi from '@/lib/interviews-api'

vi.mock('@/lib/applications-api')
vi.mock('@/lib/interviews-api')

function renderDialog(onClose = vi.fn()) {
  render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <InterviewMoveDialog applicationId="a1" companyName="Acme" onClose={onClose} />
    </QueryClientProvider>,
  )
  return onClose
}

describe('InterviewMoveDialog', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.changeApplicationStatus).mockReset().mockResolvedValue({} as applicationsApi.ApplicationDetail)
    vi.mocked(interviewsApi.createInterview).mockReset().mockResolvedValue({} as interviewsApi.InterviewDetail)
    vi.mocked(interviewsApi.listInterviews).mockResolvedValue({
      items: [{ round_number: 1 } as interviewsApi.InterviewSummary, { round_number: 2 } as interviewsApi.InterviewSummary],
      total: 2,
      page: 0,
      size: 100,
    })
  })

  it('pre-picks the next round and a date a week out', async () => {
    renderDialog()

    await waitFor(() => expect(screen.getByLabelText('Round')).toHaveValue(3))
    expect(screen.getByLabelText('Type')).toHaveValue('TECHNICAL')
    expect(screen.getByLabelText('Date')).toHaveValue(format(addDays(new Date(), 7), 'yyyy-MM-dd'))
  })

  it('moves the application and creates the interview', async () => {
    const user = userEvent.setup()
    const onClose = renderDialog()
    await waitFor(() => expect(screen.getByLabelText('Round')).toHaveValue(3))

    await user.click(screen.getByRole('button', { name: 'Add interview' }))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(applicationsApi.changeApplicationStatus).toHaveBeenCalledWith('a1', { to_status: 'INTERVIEW', note: undefined })
    expect(interviewsApi.createInterview).toHaveBeenCalledWith(
      expect.objectContaining({ application_id: 'a1', round_number: 3, round_type: 'TECHNICAL' }),
    )
  })

  it('can move without creating an interview', async () => {
    const user = userEvent.setup()
    const onClose = renderDialog()

    await user.click(screen.getByRole('button', { name: 'Move without interview' }))

    await waitFor(() => expect(onClose).toHaveBeenCalled())
    expect(interviewsApi.createInterview).not.toHaveBeenCalled()
  })
})
