import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApplicationFormPage } from '@/pages/applications/ApplicationFormPage'
import * as applicationsApi from '@/lib/applications-api'

vi.mock('@/lib/applications-api')

function renderForm() {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <MemoryRouter>
        <ApplicationFormPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ApplicationFormPage validation', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.createApplication).mockReset()
  })

  it('blocks an empty submit with friendly messages and never calls the API', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText('Company is required.')).toBeInTheDocument()
    expect(screen.getByText('Job title is required.')).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()
  })

  it('rejects a malformed job link before submitting', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.type(screen.getByLabelText('Company'), 'Acme')
    await user.type(screen.getByLabelText('Job title'), 'Engineer')
    await user.type(screen.getByLabelText('Date applied'), '2026-01-05')
    await user.type(screen.getByLabelText('Job URL'), 'not a link')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText(/enter a full link/i)).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()
  })

  it('sends every optional field the form collects', async () => {
    vi.mocked(applicationsApi.createApplication).mockResolvedValue({ id: 'new1' } as applicationsApi.ApplicationDetail)
    const user = userEvent.setup()
    renderForm()

    await user.type(screen.getByLabelText('Company'), 'Acme')
    await user.type(screen.getByLabelText('Job title'), 'Engineer')
    await user.type(screen.getByLabelText('Date applied'), '2026-01-05')
    await user.selectOptions(screen.getByLabelText('Work mode'), 'remote')
    await user.selectOptions(screen.getByLabelText('Priority'), 'high')
    await user.type(screen.getByLabelText('Minimum salary'), '90000')
    await user.type(screen.getByLabelText('Maximum salary'), '120000')
    await user.type(screen.getByLabelText('Currency'), 'usd')
    await user.type(screen.getByLabelText('Recruiter email'), 'rec@example.com')
    await user.click(screen.getByLabelText('I was referred'))
    await user.click(screen.getByRole('button', { name: /create application/i }))

    await waitFor(() =>
      expect(applicationsApi.createApplication).toHaveBeenCalledWith(
        expect.objectContaining({
          work_mode: 'remote',
          priority: 'high',
          salary_min: 90000,
          salary_max: 120000,
          salary_currency: 'USD',
          recruiter_email: 'rec@example.com',
          is_referral: true,
        }),
      ),
    )
  })

  it('rejects a maximum salary below the minimum', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.type(screen.getByLabelText('Company'), 'Acme')
    await user.type(screen.getByLabelText('Job title'), 'Engineer')
    await user.type(screen.getByLabelText('Date applied'), '2026-01-05')
    await user.type(screen.getByLabelText('Minimum salary'), '100')
    await user.type(screen.getByLabelText('Maximum salary'), '50')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText('Maximum salary must be at least the minimum.')).toBeInTheDocument()
  })
})
