import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { RouteLine, type RouteStation } from '@/components/board/RouteLine'
import { FlapText } from '@/components/board/FlapText'

const STATIONS: RouteStation[] = [
  { key: 'SAVED', label: 'Saved', count: 4, line: 'saved', to: '/applications/board' },
  { key: 'APPLIED', label: 'Applied', count: 6, line: 'applied', to: '/applications/board' },
  { key: 'OFFER', label: 'Offer', count: 1, line: 'offer' },
]

describe('RouteLine', () => {
  it('shows every station with its label and count, in order', () => {
    render(
      <MemoryRouter>
        <RouteLine stations={STATIONS} />
      </MemoryRouter>,
    )

    const [wide] = screen.getAllByRole('list', { name: 'Pipeline' })
    const items = within(wide).getAllByRole('listitem')
    expect(items.map((item) => item.textContent)).toEqual(['4Saved', '6Applied', '1Offer'])
  })

  it('links a station when it has a target and names the link with the count', () => {
    render(
      <MemoryRouter>
        <RouteLine stations={STATIONS} />
      </MemoryRouter>,
    )

    expect(screen.getAllByRole('link', { name: 'Applied: 6' })[0]).toHaveAttribute('href', '/applications/board')
    expect(screen.queryByRole('link', { name: 'Offer: 1' })).not.toBeInTheDocument()
  })
})

describe('FlapText', () => {
  it('shows the final value straight away when reduced motion is on', () => {
    render(<FlapText value="17 OCT" />)

    expect(screen.getByText('17 OCT')).toBeInTheDocument()
    expect(screen.getByLabelText('17 OCT')).toBeInTheDocument()
  })
})
