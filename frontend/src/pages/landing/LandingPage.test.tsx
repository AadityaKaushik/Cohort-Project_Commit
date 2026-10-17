import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
    expect(screen.getByRole('heading', { level: 1, name: 'From saved to signed.' })).toBeInTheDocument()
    expect(screen.getByText('Job application tracker with interview preparation')).toBeInTheDocument()
    const logins = screen.getAllByRole('link', { name: 'Log in' })
    expect(logins.length).toBeGreaterThan(0)
    for (const link of logins) expect(link).toHaveAttribute('href', '/login')
    expect(screen.getByRole('link', { name: 'Create account' })).toHaveAttribute('href', '/register')
    expect(await criticalViolations(container)).toEqual([])
  }, 20_000)

  it('has a footer with the privacy policy and help links', () => {
    renderLanding()

    const footer = screen.getByRole('navigation', { name: 'Footer' })
    expect(within(footer).getByRole('link', { name: 'Privacy policy' })).toHaveAttribute('href', '/privacy-policy')
    expect(within(footer).getByRole('link', { name: 'Help' })).toHaveAttribute('href', '/help')
  })

  it('names the three problems it solves and links each section from the header', () => {
    renderLanding()

    for (const problem of ['Applications go quiet and nobody notices', 'What you were asked is forgotten by Thursday', 'Practice follows a list, not your gaps']) {
      expect(screen.getByRole('heading', { level: 3, name: problem })).toBeInTheDocument()
    }
    const sections = screen.getByRole('navigation', { name: 'Sections' })
    expect(within(sections).getByRole('link', { name: 'FAQ' })).toHaveAttribute('href', '#faq')
    expect(document.getElementById('faq')).not.toBeNull()
  })

  it('shows what Cohort does at the stage you pick, with arrow keys moving between stages', async () => {
    const user = userEvent.setup()
    renderLanding()

    expect(screen.getByRole('tab', { name: 'Saved' })).toHaveAttribute('aria-selected', 'true')
    await user.click(screen.getByRole('tab', { name: 'Interview' }))
    expect(screen.getByRole('tabpanel')).toHaveTextContent('Walk in prepared, walk out with notes')

    await user.keyboard('{ArrowRight}')
    expect(screen.getByRole('tab', { name: 'Final' })).toHaveFocus()
    expect(screen.getByRole('tabpanel')).toHaveTextContent('See the whole picture')
  })

  it('opens a FAQ answer when its question is clicked', async () => {
    const user = userEvent.setup()
    renderLanding()

    const question = screen.getByText('Does Cohort apply to jobs or read my LinkedIn for me?')
    const details = question.closest('details') as HTMLDetailsElement
    expect(details.open).toBe(false)
    await user.click(question)
    expect(details.open).toBe(true)
  })
})
