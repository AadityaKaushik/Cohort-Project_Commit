import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DirectoryPage } from '@/pages/directory/DirectoryPage'
import { DirectoryProfilePage } from '@/pages/directory/DirectoryProfilePage'
import { getDirectoryCompany, listDirectoryCompanies, type DirectoryCompanyDetail } from '@/lib/directory-api'
import { listTopics } from '@/lib/prep-api'

vi.mock('@/lib/directory-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/directory-api')>('@/lib/directory-api')
  return { ...actual, listDirectoryCompanies: vi.fn(), getDirectoryCompany: vi.fn() }
})
vi.mock('@/lib/prep-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/prep-api')>('@/lib/prep-api')
  return { ...actual, listTopics: vi.fn() }
})

function renderAt(path: string) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/directory" element={<DirectoryPage />} />
          <Route path="/directory/:slug" element={<DirectoryProfilePage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

const profile: DirectoryCompanyDetail = {
  slug: 'payu',
  name: 'PayU',
  industry: null,
  locations: null,
  size_band: null,
  website: null,
  summary: null,
  sources: [],
  last_checked: '2026-10-15',
  community_rating: null,
  community_min_contributors: 5,
  role_patterns: [
    {
      role_family: 'software_engineer',
      level: 'fresher',
      rounds: [{ name: 'Online test', format: 'HirePro platform', tests: 'Aptitude and core computer science', duration: null }],
      topic_slugs: ['sorting'],
      notes: null,
      experience_summary: 'One on-campus account from 2021.',
      pay: null,
      difficulty_level: 3,
      difficulty_basis: 'The source describes the test as moderate.',
      sources: [{ url: 'https://example.org/payu', title: 'PayU interview experience' }],
      last_checked: '2026-10-15',
    },
  ],
}

describe('company directory', () => {
  it('lists companies and filters by role family', async () => {
    vi.mocked(listDirectoryCompanies).mockResolvedValue([
      { slug: 'payu', name: 'PayU', industry: null, website: null, role_families: ['software_engineer'], difficulty_level: 3, difficulty_basis: null, last_checked: '2026-10-15' },
      { slug: 'acme', name: 'Acme', industry: null, website: null, role_families: ['data'], difficulty_level: null, difficulty_basis: null, last_checked: '2026-10-15' },
    ])
    const user = userEvent.setup()
    renderAt('/directory')

    expect(await screen.findByText('PayU')).toBeInTheDocument()
    expect(screen.getByText(/Ratings appear when 5 or more candidates have shared/)).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Role family'), 'data')
    expect(screen.queryByText('PayU')).not.toBeInTheDocument()
    expect(screen.getByText('Acme')).toBeInTheDocument()
    expect(screen.getByText('No sourced rating')).toBeInTheDocument()
  })

  it('shows rounds, sources, the pay placeholder and the no-rating note', async () => {
    vi.mocked(getDirectoryCompany).mockResolvedValue(profile)
    vi.mocked(listTopics).mockResolvedValue([])
    renderAt('/directory/payu')

    expect(await screen.findByRole('heading', { name: 'PayU' })).toBeInTheDocument()
    expect(screen.getByText(/Online test/)).toBeInTheDocument()
    expect(screen.getByText('No sourced figure yet')).toBeInTheDocument()
    expect(screen.getByText(/Difficulty 3\/5/)).toBeInTheDocument()
    expect(screen.getByText('The source describes the test as moderate.')).toBeInTheDocument()
    expect(screen.getByText(/Figures are approximate/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /PayU interview experience/ })).toHaveAttribute('href', 'https://example.org/payu')
    expect(screen.getByRole('note')).toHaveTextContent('needs 5 candidates')
  })
})
