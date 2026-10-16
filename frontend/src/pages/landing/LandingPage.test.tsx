import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { LandingPage } from '@/pages/landing/LandingPage'
import { ThemeProvider, THEME_STORAGE_KEY } from '@/hooks/use-theme'
import { criticalViolations } from '@/test/a11y'

function renderLanding() {
  return render(
    <ThemeProvider>
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>
    </ThemeProvider>,
  )
}

describe('LandingPage', () => {
  beforeEach(() => {
    localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  it.each(['light', 'dark'] as const)('shows the name, description and both links in the %s theme', async (theme) => {
    localStorage.setItem(THEME_STORAGE_KEY, theme)
    const { container } = renderLanding()

    expect(document.documentElement.classList.contains('dark')).toBe(theme === 'dark')
    expect(screen.getByRole('heading', { level: 1, name: 'Cohort' })).toBeInTheDocument()
    expect(screen.getByText('Job application tracker with interview preparation.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Create account' })).toHaveAttribute('href', '/register')
    expect(await criticalViolations(container)).toEqual([])
  }, 20_000)

  it('has a footer with the privacy policy and help links', () => {
    renderLanding()

    const footer = screen.getByRole('navigation', { name: 'Footer' })
    expect(within(footer).getByRole('link', { name: 'Privacy policy' })).toHaveAttribute('href', '/privacy-policy')
    expect(within(footer).getByRole('link', { name: 'Help' })).toHaveAttribute('href', '/help')
  })
})
