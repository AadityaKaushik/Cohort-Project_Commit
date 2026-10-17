import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { useAuth } from '@/hooks/use-auth'

vi.mock('@/hooks/use-auth')

const baseAuthValue = {
  user: undefined,
  sessionMessage: null,
  login: vi.fn(),
  register: vi.fn(),
  confirmEmail: vi.fn(),
  logout: vi.fn(),
}

function renderProtected() {
  return render(
    <MemoryRouter initialEntries={['/dashboard']}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route element={<ProtectedRoute />}>
          <Route path="/dashboard" element={<div>Secret dashboard</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

function renderWithHome(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/login" element={<div>Login page</div>} />
        <Route element={<ProtectedRoute signedOutHome={<div>Public landing</div>} />}>
          <Route index element={<div>Dashboard</div>} />
          <Route path="/applications" element={<div>Applications</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  )
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    vi.mocked(useAuth).mockReset()
  })

  it('shows a loading state while the session is resolving, revealing neither page', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: true, isAuthenticated: false })

    renderProtected()

    expect(screen.getByRole('status', { name: /checking session/i })).toBeInTheDocument()
    expect(screen.queryByText('Secret dashboard')).not.toBeInTheDocument()
    expect(screen.queryByText('Login page')).not.toBeInTheDocument()
  })

  it('redirects to /login when not authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: false, isAuthenticated: false })

    renderProtected()

    expect(screen.getByText('Login page')).toBeInTheDocument()
  })

  it('renders the protected content when authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: false, isAuthenticated: true })

    renderProtected()

    expect(screen.getByText('Secret dashboard')).toBeInTheDocument()
  })

  it('shows the public landing page to a signed-out visitor at the root', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: false, isAuthenticated: false })

    renderWithHome('/')

    expect(screen.getByText('Public landing')).toBeInTheDocument()
    expect(screen.queryByText('Dashboard')).not.toBeInTheDocument()
  })

  it('sends a signed-in visitor at the root to the dashboard, not the landing page', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: false, isAuthenticated: true })

    renderWithHome('/')

    expect(screen.getByText('Dashboard')).toBeInTheDocument()
    expect(screen.queryByText('Public landing')).not.toBeInTheDocument()
  })

  it('still redirects a signed-out visitor to /login from any other protected path', () => {
    vi.mocked(useAuth).mockReturnValue({ ...baseAuthValue, isLoading: false, isAuthenticated: false })

    renderWithHome('/applications')

    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.queryByText('Public landing')).not.toBeInTheDocument()
  })
})
