import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ApplicationDetailPage } from '@/pages/applications/ApplicationDetailPage'
import * as applicationsApi from '@/lib/applications-api'
import * as interviewsApi from '@/lib/interviews-api'
import * as resumesApi from '@/lib/resumes-api'

vi.mock('@/lib/applications-api')
vi.mock('@/lib/resumes-api')
vi.mock('@/lib/interviews-api')

const detail: applicationsApi.ApplicationDetail = {
  id: 'a1',
  company: { id: 'c1', name: 'Acme', website: null, industry: null },
  resume_id: null,
  job_title: 'Backend Engineer',
  job_description: null,
  job_url: null,
  location: 'Remote',
  work_mode: 'remote',
  job_type: 'full_time',
  salary_min: null,
  salary_max: null,
  salary_currency: null,
  date_applied: '2026-01-05',
  status: 'FINAL_ROUND',
  current_stage: 'FINAL_ROUND',
  days_in_stage: 3,
  priority: 'high',
  source: null,
  is_referral: false,
  deadline: null,
  recruiter_name: null,
  recruiter_email: null,
  notes: null,
  cover_letter: null,
  archived_at: null,
  stage_entered_at: '2026-01-10T00:00:00Z',
  created_at: '2026-01-05T00:00:00Z',
  updated_at: '2026-01-10T00:00:00Z',
  allowed_next_statuses: [],
  correction_statuses: [],
  history: [
    { id: 'h1', from_status: 'SAVED', to_status: 'APPLIED', note: null, changed_at: '2026-01-05T00:00:00Z' },
    { id: 'h2', from_status: 'INTERVIEW', to_status: 'FINAL_ROUND', note: null, changed_at: '2026-01-10T00:00:00Z' },
  ],
  interviews: [
    { id: 'i1', round_number: 1, round_type: 'PHONE_SCREEN', scheduled_at: null, completed_at: null, outcome: 'pending' },
  ],
}

describe('ApplicationDetailPage', () => {
  beforeEach(() => {
    vi.mocked(applicationsApi.getApplication).mockResolvedValue(detail)
    vi.mocked(resumesApi.listResumes).mockResolvedValue({ items: [], total: 0, page: 0, size: 100 })
  })

  it('renders no raw enum strings', async () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={['/applications/a1']}>
          <Routes>
            <Route path="/applications/:id" element={<ApplicationDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(await screen.findByText('Interview → Final Round')).toBeInTheDocument()
    const text = document.body.textContent ?? ''
    expect(text).not.toMatch(/[A-Z]{2,}_[A-Z]+/)
    expect(text).not.toMatch(/\b[a-z]+_[a-z]+\b/)
    expect(text).toContain('Phone Screen')
  })
})

describe('ApplicationDetailPage status controls', () => {
  function renderDetail() {
    return render(
      <QueryClientProvider client={new QueryClient()}>
        <MemoryRouter initialEntries={['/applications/a1']}>
          <Routes>
            <Route path="/applications/:id" element={<ApplicationDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )
  }

  beforeEach(() => {
    vi.mocked(applicationsApi.getApplication).mockResolvedValue({
      ...detail,
      status: 'OA',
      allowed_next_statuses: ['INTERVIEW', 'REJECTED'],
      correction_statuses: ['APPLIED'],
    })
    vi.mocked(resumesApi.listResumes).mockResolvedValue({ items: [], total: 0, page: 0, size: 100 })
  })

  it('offers only the statuses the server allows', async () => {
    renderDetail()

    const select = await screen.findByLabelText('New status')
    const options = Array.from(select.querySelectorAll('option')).map((option) => option.textContent)
    expect(options).toEqual(['Choose a status', 'Interview', 'Rejected', 'Applied'])
  })

  it('opens the add interview dialog when moving to Interview', async () => {
    vi.mocked(interviewsApi.listInterviews).mockResolvedValue({ items: [], total: 0, page: 0, size: 100 })
    const user = userEvent.setup()
    renderDetail()

    await user.selectOptions(await screen.findByLabelText('New status'), 'INTERVIEW')
    await user.click(screen.getByRole('button', { name: 'Update status' }))

    expect(await screen.findByRole('dialog', { name: 'Add interview' })).toBeInTheDocument()
    expect(applicationsApi.changeApplicationStatus).not.toHaveBeenCalled()
  })

  it('links Duplicate to the prefilled form', async () => {
    renderDetail()

    expect(await screen.findByRole('link', { name: 'Duplicate' })).toHaveAttribute('href', '/applications/new?from=a1')
  })

  it('requires a note for a backward move and keeps a readable placeholder afterwards', async () => {
    vi.mocked(applicationsApi.changeApplicationStatus).mockResolvedValue({ ...detail, status: 'APPLIED' })
    const user = userEvent.setup()
    renderDetail()

    await user.selectOptions(await screen.findByLabelText('New status'), 'APPLIED')
    expect(screen.getByRole('button', { name: 'Update status' })).toBeDisabled()

    await user.type(screen.getByLabelText('Note'), 'logged too early')
    await user.click(screen.getByRole('button', { name: 'Update status' }))

    await waitFor(() =>
      expect(applicationsApi.changeApplicationStatus).toHaveBeenCalledWith('a1', {
        to_status: 'APPLIED',
        note: 'logged too early',
      }),
    )
    await waitFor(() => expect(screen.getByLabelText('New status')).toHaveValue(''))
  })
})
