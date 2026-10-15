import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { HelpPage } from '@/pages/help/HelpPage'
import { SHORTCUTS } from '@/hooks/use-shortcuts'

const auth = vi.hoisted(() => ({ user: undefined as { id: string } | undefined }))
vi.mock('@/hooks/use-auth', () => ({ useAuth: () => ({ isAuthenticated: Boolean(auth.user), user: auth.user }) }))
vi.mock('@/components/layout/ThemeToggle', () => ({ ThemeToggle: () => null }))

function renderHelp(path = '/help') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <HelpPage />
    </MemoryRouter>,
  )
}

describe('HelpPage', () => {
  it('lists every real keyboard shortcut', () => {
    renderHelp()

    for (const shortcut of SHORTCUTS) {
      expect(screen.getByText(shortcut.label)).toBeInTheDocument()
    }
  })

  it('covers import, bookmarking and the tour anchor', () => {
    renderHelp('/help#tour')

    expect(screen.getByRole('heading', { name: 'Import and bookmark jobs' })).toBeInTheDocument()
    expect(document.getElementById('tour')).not.toBeNull()
  })

  it('links to the privacy policy', () => {
    renderHelp()
    expect(screen.getAllByRole('link', { name: /privacy policy/i })[0]).toHaveAttribute('href', '/privacy-policy')
  })

  it('starts the tour again for a signed-in user', async () => {
    auth.user = { id: 'user-1' }
    window.localStorage.setItem('cohort:tour-done:user-1', '1')
    const user = userEvent.setup()
    renderHelp('/help#tour')

    await user.click(screen.getByRole('button', { name: 'Start the tour' }))
    expect(window.localStorage.getItem('cohort:tour-done:user-1')).toBeNull()
    auth.user = undefined
  })

  it('asks signed-out visitors to log in for the tour', () => {
    renderHelp()
    expect(screen.queryByRole('button', { name: 'Start the tour' })).not.toBeInTheDocument()
  })
})
