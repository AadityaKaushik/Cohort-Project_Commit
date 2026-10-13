import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TooltipProvider } from '@/components/ui/tooltip'
import { AppShell } from '@/components/layout/AppShell'
import { useAuth } from '@/hooks/use-auth'
import { breadcrumbsFor, pageTitleFor } from '@/lib/navigation'

vi.mock('@/hooks/use-auth')
vi.mock('@/components/notifications/NotificationBell', () => ({ NotificationBell: () => null }))

const logout = vi.fn()

function Where() {
  return <p data-testid="where">{useLocation().pathname}</p>
}

function renderShell(initial = '/applications') {
  return render(
    <QueryClientProvider client={new QueryClient()}>
      <TooltipProvider>
      <MemoryRouter initialEntries={[initial]}>
        <Routes>
          <Route element={<AppShell />}>
            <Route path="*" element={<Where />} />
          </Route>
          <Route path="/login" element={<Where />} />
        </Routes>
      </MemoryRouter>
      </TooltipProvider>
    </QueryClientProvider>,
  )
}

describe('AppShell', () => {
  beforeEach(() => {
    logout.mockReset().mockResolvedValue(undefined)
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', email: 'jane@example.com', display_name: 'Jane', target_role: null, graduation_year: null, target_date: null, onboarded: true, timezone: null, fl_opt_in: false, fl_privacy_posture: 'balanced', created_at: '' },
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout,
    })
  })

  it('logs out from the user menu and returns to the login page', async () => {
    const user = userEvent.setup()
    renderShell()

    await user.click(screen.getByRole('button', { name: 'Account menu' }))
    await user.click(await screen.findByRole('menuitem', { name: /log out/i }))

    await waitFor(() => expect(logout).toHaveBeenCalled())
    await waitFor(() => expect(screen.getByTestId('where')).toHaveTextContent('/login'))
  })

  it('opens the command palette with the keyboard and navigates', async () => {
    const user = userEvent.setup()
    renderShell('/')

    await user.keyboard('{Control>}k{/Control}')
    await user.type(await screen.findByRole('combobox', { name: 'Search commands' }), 'resumes')
    await user.keyboard('{Enter}')

    await waitFor(() => expect(screen.getByTestId('where')).toHaveTextContent('/resumes'))
  })

  it('sends a user who has not onboarded to the onboarding page', async () => {
    vi.mocked(useAuth).mockReturnValue({
      ...vi.mocked(useAuth)(),
      user: { id: '1', email: 'new@example.com', display_name: 'New', target_role: null, graduation_year: null, target_date: null, onboarded: false, timezone: null, fl_opt_in: false, fl_privacy_posture: 'balanced', created_at: '' },
    })
    render(
      <QueryClientProvider client={new QueryClient()}>
        <TooltipProvider>
          <MemoryRouter initialEntries={['/']}>
            <Routes>
              <Route element={<AppShell />}>
                <Route path="*" element={<Where />} />
              </Route>
              <Route path="/onboarding" element={<Where />} />
            </Routes>
          </MemoryRouter>
        </TooltipProvider>
      </QueryClientProvider>,
    )

    await waitFor(() => expect(screen.getByTestId('where')).toHaveTextContent('/onboarding'))
  })

  it('shows breadcrumbs for detail pages', () => {
    expect(breadcrumbsFor('/applications/42/edit')).toEqual([
      { label: 'Applications', path: '/applications' },
      { label: 'Details', path: '/applications/42' },
      { label: 'Edit' },
    ])
    renderShell('/applications/42')
    expect(screen.getByRole('navigation', { name: 'Breadcrumb' })).toHaveTextContent('ApplicationsDetails')
  })

  it('gives every page a descriptive title', () => {
    expect(pageTitleFor('/')).toBe('Dashboard')
    expect(pageTitleFor('/applications')).toBe('Applications')
    expect(pageTitleFor('/applications/new')).toBe('New application')
    expect(pageTitleFor('/applications/42')).toBe('Application details')
    expect(pageTitleFor('/resumes/7/edit')).toBe('Edit resume')
    expect(pageTitleFor('/interviews/3/debrief')).toBe('Interview debrief')
    expect(pageTitleFor('/prep/questions')).toBe('Question bank')
  })
})
