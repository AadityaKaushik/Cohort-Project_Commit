import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ThemeProvider, THEME_STORAGE_KEY } from '@/hooks/use-theme'
import { ThemeToggle } from '@/components/layout/ThemeToggle'

function mockSystemTheme(dark: boolean) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn().mockImplementation(() => ({
      matches: dark,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })),
  )
}

function renderToggle() {
  return render(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  )
}

describe('theme toggle', () => {
  beforeEach(() => {
    window.localStorage.clear()
    document.documentElement.classList.remove('dark')
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('follows a dark system preference by default', () => {
    mockSystemTheme(true)
    renderToggle()

    expect(document.documentElement).toHaveClass('dark')
    expect(screen.getByRole('button', { name: 'System theme' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('follows a light system preference by default', () => {
    mockSystemTheme(false)
    renderToggle()

    expect(document.documentElement).not.toHaveClass('dark')
  })

  it('applies and persists an explicit choice over the system preference', async () => {
    mockSystemTheme(false)
    const user = userEvent.setup()
    renderToggle()

    await user.click(screen.getByRole('button', { name: 'Dark theme' }))

    expect(document.documentElement).toHaveClass('dark')
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark')
  })

  it('restores a persisted choice on load', () => {
    mockSystemTheme(false)
    window.localStorage.setItem(THEME_STORAGE_KEY, 'dark')
    renderToggle()

    expect(document.documentElement).toHaveClass('dark')
    expect(screen.getByRole('button', { name: 'Dark theme' })).toHaveAttribute('aria-pressed', 'true')
  })

  it('returns to the system preference when system is chosen', async () => {
    mockSystemTheme(false)
    window.localStorage.setItem(THEME_STORAGE_KEY, 'dark')
    const user = userEvent.setup()
    renderToggle()

    await user.click(screen.getByRole('button', { name: 'System theme' }))

    expect(document.documentElement).not.toHaveClass('dark')
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBeNull()
  })
})
