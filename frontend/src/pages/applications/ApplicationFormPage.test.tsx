import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
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
})
