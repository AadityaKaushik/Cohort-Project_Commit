import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { LoginPage } from '@/pages/LoginPage'
import { RegisterPage } from '@/pages/RegisterPage'
import { ThemeProvider, THEME_STORAGE_KEY } from '@/hooks/use-theme'
import { useAuth } from '@/hooks/use-auth'
import { listOAuthProviders } from '@/lib/auth-api'
import { criticalViolations } from '@/test/a11y'

vi.mock('@/hooks/use-auth')
vi.mock('@/lib/auth-api', async () => {
  const actual = await vi.importActual<typeof import('@/lib/auth-api')>('@/lib/auth-api')
  return { ...actual, listOAuthProviders: vi.fn() }
})

const PAGES = [
  ['login', LoginPage, /log in to cohort/i],
  ['register', RegisterPage, /create your cohort account/i],
] as const

function renderPage(Page: () => React.ReactNode) {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <Page />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('auth pages', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.mocked(listOAuthProviders).mockResolvedValue([])
    vi.mocked(useAuth).mockReturnValue({
      user: undefined,
      isLoading: false,
      isAuthenticated: false,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    })
  })

  describe.each(['light', 'dark'] as const)('in the %s theme', (theme) => {
    it.each(PAGES)('renders the %s form, brand panel and no critical a11y issues', async (_name, Page, heading) => {
      localStorage.setItem(THEME_STORAGE_KEY, theme)
      const { container } = renderPage(Page)

      expect(document.documentElement.classList.contains('dark')).toBe(theme === 'dark')
      expect(screen.getByRole('heading', { level: 1, name: heading })).toBeInTheDocument()
      expect(screen.getByRole('complementary', { name: 'About Cohort' })).toBeInTheDocument()
      expect(screen.getByRole('main')).toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /continue with/i })).not.toBeInTheDocument()
      expect(await criticalViolations(container)).toEqual([])
    }, 20_000)
  })

  it('toggles password visibility without losing the typed value', async () => {
    const user = userEvent.setup()
    renderPage(LoginPage)

    const password = screen.getByLabelText(/^password$/i)
    await user.type(password, 'secret-value')
    await user.click(screen.getByRole('button', { name: 'Show password' }))

    expect(password).toHaveAttribute('type', 'text')
    expect(password).toHaveValue('secret-value')
    await user.click(screen.getByRole('button', { name: 'Hide password' }))
    expect(password).toHaveAttribute('type', 'password')
  })

  it('validates the email inline when the field loses focus', async () => {
    const user = userEvent.setup()
    renderPage(LoginPage)

    await user.type(screen.getByLabelText(/email/i), 'not-an-email')
    await user.tab()

    expect(screen.getByText(/enter a valid email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toHaveAttribute('aria-invalid', 'true')
  })
})
