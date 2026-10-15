import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { LandingPage } from '@/pages/landing/LandingPage'
import { ThemeProvider, THEME_STORAGE_KEY } from '@/hooks/use-theme'
import { moveVerdict } from '@/lib/demo-board'
import { criticalViolations, mockReducedMotion } from '@/test/a11y'

const originalMatchMedia = window.matchMedia

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

  afterEach(() => {
    window.matchMedia = originalMatchMedia
  })

  it.each(['light', 'dark'] as const)('renders the hero, both calls to action and the brand in the %s theme', async (theme) => {
    localStorage.setItem(THEME_STORAGE_KEY, theme)
    const { container } = renderLanding()

    expect(document.documentElement.classList.contains('dark')).toBe(theme === 'dark')
    expect(screen.getByRole('heading', { level: 1, name: /run your job search/i })).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: /create your free account/i })[0]).toHaveAttribute('href', '/register')
    expect(screen.getAllByRole('link', { name: /^log in$/i })[0]).toHaveAttribute('href', '/login')
    expect(screen.getAllByText('Cohort').length).toBeGreaterThan(0)
    expect(await criticalViolations(container)).toEqual([])
  }, 20_000)

  it('has a footer with only real links', () => {
    renderLanding()

    const footer = screen.getByRole('navigation', { name: 'Footer' })
    expect(within(footer).getByRole('link', { name: 'Privacy' })).toHaveAttribute('href', '#privacy')
    expect(within(footer).getByRole('link', { name: 'Privacy policy' })).toHaveAttribute('href', '/privacy-policy')
    expect(within(footer).getByRole('link', { name: 'Help' })).toHaveAttribute('href', '/help')
    expect(within(footer).getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login')
    expect(within(footer).getByRole('link', { name: 'Create account' })).toHaveAttribute('href', '/register')
  })

  it('switches FAQ categories as tabs', async () => {
    const user = userEvent.setup()
    renderLanding()

    await user.click(screen.getByRole('tab', { name: 'Privacy and AI' }))

    expect(screen.getByRole('tab', { name: 'Privacy and AI' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel')).toHaveTextContent(/federated learning, in plain terms/i)
  })

  it('lets a visitor move a demo card one stage forward on the interactive board', async () => {
    const user = userEvent.setup()
    renderLanding()

    await user.click(screen.getByRole('button', { name: 'Move Northwind Labs to Assessment' }))

    expect(screen.getByText(/northwind labs: moved to assessment/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Move Northwind Labs to Interview' })).toBeInTheDocument()
  })

  it('scores an example job against the sample resume in the browser', async () => {
    const user = userEvent.setup()
    renderLanding()

    expect(screen.getByText('Sample demo, not your data')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Use an example job' }))

    expect(within(screen.getByRole('list', { name: 'Matched skills' })).getByText('Java')).toBeInTheDocument()
    expect(within(screen.getByRole('list', { name: 'Missing skills' })).getByText('Kubernetes')).toBeInTheDocument()
  })

  it('shows the full typed phrase and keeps everything still under reduced motion', () => {
    mockReducedMotion(true)
    renderLanding()

    expect(screen.getByTestId('typing-text')).toHaveAttribute('data-typed', 'Track every application.')
    expect(screen.getByRole('progressbar', { name: 'Prep tasks completed' })).toHaveAttribute('aria-valuenow', '75')
  })
})

describe('moveVerdict', () => {
  it('allows one stage forward and refuses skips and backward moves', () => {
    expect(moveVerdict(0, 1)?.tone).toBe('ok')
    expect(moveVerdict(0, 3)?.tone).toBe('blocked')
    expect(moveVerdict(2, 1)?.tone).toBe('blocked')
    expect(moveVerdict(1, 1)).toBeNull()
  })
})
