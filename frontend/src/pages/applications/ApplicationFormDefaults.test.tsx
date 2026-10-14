import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { format } from 'date-fns'
import { ApplicationFormPage } from '@/pages/applications/ApplicationFormPage'
import * as applicationsApi from '@/lib/applications-api'
import * as companiesApi from '@/lib/companies-api'

vi.mock('@/lib/applications-api')
vi.mock('@/lib/companies-api')

const lastUsed = {
  id: 'a1',
  company: { id: 'c1', name: 'Globex', website: null, industry: null },
  job_title: 'Analyst',
  status: 'REJECTED',
  date_applied: '2026-02-01',
  location: 'Pune',
  work_mode: 'hybrid',
  source: 'LinkedIn',
  is_referral: false,
} as unknown as applicationsApi.ApplicationDetail

function renderForm(path = '/applications/new') {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/applications/new" element={<ApplicationFormPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ApplicationFormPage defaults', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.createApplication).mockReset()
    vi.mocked(applicationsApi.listApplications).mockResolvedValue({
      items: [{ id: 'a1' } as applicationsApi.ApplicationSummary],
      total: 1,
      page: 0,
      size: 1,
    })
    vi.mocked(applicationsApi.getApplication).mockResolvedValue(lastUsed)
    vi.mocked(companiesApi.listCompanies).mockResolvedValue([
      { id: 'c1', name: 'Globex', website: null, industry: null, application_count: 1, active_application_count: 0, contact_count: 0, last_activity_at: null },
    ])
  })

  it('starts with today, Applied, and the last-used source, location and work mode', async () => {
    renderForm()

    expect(screen.getByLabelText('Date applied')).toHaveValue(format(new Date(), 'yyyy-MM-dd'))
    expect(screen.getByLabelText('Status')).toHaveValue('APPLIED')
    await waitFor(() => expect(screen.getByLabelText('Source')).toHaveValue('LinkedIn'))
    expect(screen.getByLabelText('Location')).toHaveValue('Pune')
    expect(screen.getByLabelText('Work mode')).toHaveValue('hybrid')
  })

  it('offers existing companies as suggestions', async () => {
    const { container } = renderForm()

    await waitFor(() => expect(container.querySelector('#company-options option[value="Globex"]')).not.toBeNull())
  })

  it('fills the company from a pasted job link only when the field is empty', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByLabelText('Job URL'))
    await user.paste('https://careers.stripe.com/listing/1')
    expect(screen.getByLabelText('Company')).toHaveValue('Stripe')

    await user.clear(screen.getByLabelText('Job URL'))
    await user.paste('https://www.acme.com/jobs/2')
    expect(screen.getByLabelText('Company')).toHaveValue('Stripe')
  })

  it('prefills from the application being duplicated', async () => {
    renderForm('/applications/new?from=a1')

    await waitFor(() => expect(screen.getByLabelText('Company')).toHaveValue('Globex'))
    expect(screen.getByLabelText('Job title')).toHaveValue('Analyst')
    expect(screen.getByLabelText('Status')).toHaveValue('APPLIED')
    expect(screen.getByLabelText('Date applied')).toHaveValue(format(new Date(), 'yyyy-MM-dd'))
    expect(screen.getByRole('heading', { name: 'Duplicate application' })).toBeInTheDocument()
  })
})
