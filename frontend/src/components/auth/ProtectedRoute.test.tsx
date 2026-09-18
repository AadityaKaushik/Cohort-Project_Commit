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
})
