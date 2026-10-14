import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CommandPalette } from '@/components/layout/CommandPalette'
import { search } from '@/lib/search-api'

vi.mock('@/lib/search-api', () => ({ search: vi.fn() }))
vi.mock('@/hooks/use-auth', () => ({ useAuth: () => ({ logout: vi.fn() }) }))

function renderPalette() {
  return render(
    <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <MemoryRouter>
        <Routes>
          <Route path="/" element={<CommandPalette open onOpenChange={() => {}} />} />
          <Route path="/applications/:id" element={<p>Application page</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('CommandPalette search', () => {
  afterEach(() => {
    vi.clearAllMocks()
  })

  it('shows matching records next to pages and opens one', async () => {
    vi.mocked(search).mockResolvedValue({
      applications: [{ id: 'a-1', title: 'Backend Engineer', subtitle: 'Acme', parent_id: null }],
      companies: [],
      questions: [],
      notes: [],
    })
    const user = userEvent.setup()
    renderPalette()

    await user.type(screen.getByRole('combobox', { name: 'Search commands' }), 'backend')
    const option = await screen.findByRole('option', { name: /Backend Engineer · Acme/ })
    expect(search).toHaveBeenCalledWith('backend')

    await user.click(option)
    expect(await screen.findByText('Application page')).toBeInTheDocument()
  })

  it('does not search for a single character', async () => {
    const user = userEvent.setup()
    renderPalette()

    await user.type(screen.getByRole('combobox', { name: 'Search commands' }), 'a')
    await new Promise((resolve) => setTimeout(resolve, 300))

    expect(search).not.toHaveBeenCalled()
  })
})
