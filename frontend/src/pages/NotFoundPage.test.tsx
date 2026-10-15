import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { NotFoundPage } from '@/pages/NotFoundPage'

describe('NotFoundPage', () => {
  it('offers a way home and to the help page', () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('link', { name: /Return home/ })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: 'Open help' })).toHaveAttribute('href', '/help')
  })
})
