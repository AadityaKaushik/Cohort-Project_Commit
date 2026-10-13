import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ContactsPage } from '@/pages/contacts/ContactsPage'
import { listContacts, markContactContacted, updateContact, type Contact } from '@/lib/contacts-api'

vi.mock('@/lib/contacts-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/contacts-api')>('@/lib/contacts-api')
  return { ...actual, listContacts: vi.fn(), updateContact: vi.fn(), markContactContacted: vi.fn() }
})

vi.mock('@/lib/applications-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/applications-api')>('@/lib/applications-api')
  return { ...actual, listApplications: vi.fn().mockResolvedValue({ items: [], total: 0, page: 0, size: 100 }) }
})

function contact(overrides: Partial<Contact> = {}): Contact {
  return {
    id: 'c-1',
    name: 'Priya Nair',
    role_title: 'Staff Engineer',
    email: null,
    linkedin_url: null,
    relationship: 'referrer',
    referral_status: 'requested',
    company_id: 'co-1',
    company_name: 'Acme',
    application_id: null,
    notes: null,
    last_contacted_at: null,
    cold: false,
    created_at: '2026-09-01T00:00:00Z',
    updated_at: '2026-09-01T00:00:00Z',
    ...overrides,
  }
}

function renderPage() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <ContactsPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ContactsPage', () => {
  beforeEach(() => {
    vi.mocked(updateContact).mockResolvedValue(contact())
    vi.mocked(markContactContacted).mockResolvedValue(contact())
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows an empty state with a call to action', async () => {
    vi.mocked(listContacts).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('No contacts yet')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Add your first contact' })).toBeInTheDocument()
  })

  it('lists contacts and flags the ones that went cold', async () => {
    vi.mocked(listContacts).mockResolvedValue([contact(), contact({ id: 'c-2', name: 'Carl Cold', cold: true })])
    renderPage()

    expect(await screen.findByText('Priya Nair')).toBeInTheDocument()
    expect(screen.getAllByText('Gone cold')).toHaveLength(1)
    expect(screen.getAllByText(/Staff Engineer · Acme/).length).toBeGreaterThan(0)
  })

  it('changes the referral status and marks a contact as contacted', async () => {
    vi.mocked(listContacts).mockResolvedValue([contact()])
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Priya Nair')
    await user.selectOptions(screen.getByLabelText('Referral status for Priya Nair'), 'submitted')
    await waitFor(() => expect(updateContact).toHaveBeenCalledWith('c-1', { referral_status: 'submitted' }))

    await user.click(screen.getByRole('button', { name: 'Mark contacted' }))
    await waitFor(() => expect(markContactContacted).toHaveBeenCalledWith('c-1'))
  })

  it('filters by referral status through the API', async () => {
    vi.mocked(listContacts).mockResolvedValue([contact()])
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('Priya Nair')
    await user.click(screen.getByRole('button', { name: 'Requested' }))
    await waitFor(() => expect(listContacts).toHaveBeenCalledWith('requested'))
  })
})
