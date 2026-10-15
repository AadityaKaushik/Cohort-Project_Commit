import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CompaniesPage } from '@/pages/companies/CompaniesPage'
import { CompanyDetailPage } from '@/pages/companies/CompanyDetailPage'
import { addCompanyNote, getCompany, listCompanies, type CompanyDetail } from '@/lib/companies-api'

vi.mock('@/lib/companies-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/companies-api')>('@/lib/companies-api')
  return { ...actual, listCompanies: vi.fn(), getCompany: vi.fn(), addCompanyNote: vi.fn() }
})

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/companies" element={<CompaniesPage />} />
          <Route path="/companies/:id" element={<CompanyDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const detail: CompanyDetail = {
  id: 'co-1',
  name: 'Globex',
  website: 'https://globex.example',
  industry: 'Logistics',
  stats: { applications: 2, active: 1, interviews: 1, offers: 0, last_activity_at: '2026-09-20T00:00:00Z' },
  applications: [
    {
      id: 'a-1',
      company: { id: 'co-1', name: 'Globex', website: null, industry: null },
      job_title: 'Backend Engineer',
      status: 'INTERVIEW',
      current_stage: 'INTERVIEW',
      days_in_stage: 3,
      date_applied: '2026-09-01',
      priority: null,
      archived: false,
      allowed_next_statuses: [],
      correction_statuses: [],
    },
  ],
  contacts: [],
  notes: [{ id: 'n-1', body: 'Ask about on-call', created_at: '2026-09-02T00:00:00Z', updated_at: '2026-09-02T00:00:00Z' }],
}

describe('company pages', () => {
  afterEach(() => vi.clearAllMocks())

  it('lists companies with their rollups', async () => {
    vi.mocked(listCompanies).mockResolvedValue([
      { id: 'co-1', name: 'Globex', website: null, industry: 'Logistics', application_count: 2, active_application_count: 1, contact_count: 0, last_activity_at: null },
    ])
    renderAt('/companies')

    expect(await screen.findByText('Globex')).toBeInTheDocument()
    expect(screen.getByText(/2 applications · 1 active · 0 contacts/)).toBeInTheDocument()
  })

  it('shows a designed empty state when there are no companies', async () => {
    vi.mocked(listCompanies).mockResolvedValue([])
    renderAt('/companies')

    expect(await screen.findByText('No companies yet')).toBeInTheDocument()
  })

  it('aggregates applications, contacts and notes and adds a note', async () => {
    vi.mocked(getCompany).mockResolvedValue(detail)
    vi.mocked(addCompanyNote).mockResolvedValue(detail.notes[0])
    const user = userEvent.setup()
    renderAt('/companies/co-1')

    expect(await screen.findByRole('heading', { name: 'Globex' })).toBeInTheDocument()
    expect(screen.getByText('Backend Engineer')).toBeInTheDocument()
    expect(screen.getByText('Ask about on-call')).toBeInTheDocument()
    expect(screen.getByText(/No contacts here yet/)).toBeInTheDocument()

    await user.type(screen.getByLabelText('New note'), 'Great team')
    await user.click(screen.getByRole('button', { name: 'Add note' }))
    await waitFor(() => expect(addCompanyNote).toHaveBeenCalledWith('co-1', 'Great team'))
  })

  it('blocks a mashed note and warns about an off-topic one before saving', async () => {
    vi.mocked(getCompany).mockResolvedValue(detail)
    vi.mocked(addCompanyNote).mockReset().mockResolvedValue(detail.notes[0])
    const user = userEvent.setup()
    renderAt('/companies/co-1')
    const field = await screen.findByLabelText('New note')

    await user.type(field, 'asdfghjkl')
    await user.click(screen.getByRole('button', { name: 'Add note' }))
    expect(await screen.findByText(/looks like random typing/)).toBeInTheDocument()
    expect(addCompanyNote).not.toHaveBeenCalled()

    await user.clear(field)
    await user.type(field, 'the tea there was not good')
    await user.click(screen.getByRole('button', { name: 'Add note' }))
    expect(await screen.findByText(/does not look like it is about the interview/)).toBeInTheDocument()
    expect(addCompanyNote).not.toHaveBeenCalled()

    await user.click(screen.getByRole('button', { name: 'Save anyway' }))
    await waitFor(() => expect(addCompanyNote).toHaveBeenCalledWith('co-1', 'the tea there was not good'))
  })
})
