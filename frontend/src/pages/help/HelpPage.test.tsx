import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { HelpPage } from '@/pages/help/HelpPage'
import { SHORTCUTS } from '@/hooks/use-shortcuts'

vi.mock('@/hooks/use-auth', () => ({ useAuth: () => ({ isAuthenticated: false }) }))
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
})
