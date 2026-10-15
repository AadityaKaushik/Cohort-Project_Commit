import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { ApplicationFormPage } from '@/pages/applications/ApplicationFormPage'
import * as applicationsApi from '@/lib/applications-api'

vi.mock('@/lib/applications-api')

async function fillRequired(user: ReturnType<typeof userEvent.setup>, company = 'Acme') {
  await user.type(screen.getByLabelText('Company'), company)
  await user.type(screen.getByLabelText('Job title'), 'Engineer')
  await user.clear(screen.getByLabelText('Date applied'))
  await user.type(screen.getByLabelText('Date applied'), '2026-01-05')
}

describe('ApplicationFormPage input guardrails', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.createApplication).mockReset()
    vi.mocked(applicationsApi.createApplication).mockResolvedValue({ id: 'new1' } as applicationsApi.ApplicationDetail)
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter>
          <ApplicationFormPage />
        </MemoryRouter>
      </QueryClientProvider>,
    )
  })

  it('rejects a symbols-only company name', async () => {
    const user = userEvent.setup()
    await fillRequired(user, '!!!')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText(/needs at least one letter or number/)).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()
  })

  it('asks for a currency when a salary is entered', async () => {
    const user = userEvent.setup()
    await fillRequired(user)
    await user.type(screen.getByLabelText('Minimum salary'), '90000')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText(/Add the currency for this salary/)).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()
  })

  it('rejects a date before 2000', async () => {
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Company'), 'Acme')
    await user.type(screen.getByLabelText('Job title'), 'Engineer')
    await user.clear(screen.getByLabelText('Date applied'))
    await user.type(screen.getByLabelText('Date applied'), '1990-05-01')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText(/from the year 2000 onwards/)).toBeInTheDocument()
  })

  it('rejects mashed notes but accepts a short real note', async () => {
    const user = userEvent.setup()
    await fillRequired(user)
    await user.type(screen.getByLabelText('Notes'), 'asdfghjkl')
    await user.click(screen.getByRole('button', { name: /create application/i }))
    expect(await screen.findByText(/looks like random typing/)).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()

    await user.clear(screen.getByLabelText('Notes'))
    await user.type(screen.getByLabelText('Notes'), 'Rejected after round 2')
    await user.click(screen.getByRole('button', { name: /create application/i }))
    await waitFor(() => expect(applicationsApi.createApplication).toHaveBeenCalledTimes(1))
  })

  it('warns about contact details in notes and saves after Save anyway', async () => {
    const user = userEvent.setup()
    await fillRequired(user)
    await user.type(screen.getByLabelText('Notes'), 'Recruiter is priya@acme.com')
    await user.click(screen.getByRole('button', { name: /create application/i }))

    expect(await screen.findByText(/email address or phone number/)).toBeInTheDocument()
    expect(applicationsApi.createApplication).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Save anyway' }))
    await waitFor(() => expect(applicationsApi.createApplication).toHaveBeenCalledTimes(1))
  })
})
