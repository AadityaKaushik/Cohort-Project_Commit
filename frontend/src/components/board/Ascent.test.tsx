import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { Ascent, type AscentStage } from '@/components/board/Ascent'
import { FlapText } from '@/components/board/FlapText'

const STAGES: AscentStage[] = [
  { key: 'SAVED', label: 'Saved', count: 4, line: 'saved', to: '/applications/board' },
  { key: 'APPLIED', label: 'Applied', count: 6, line: 'applied', to: '/applications/board' },
  { key: 'OFFER', label: 'Offer', count: 1, line: 'offer' },
]

describe('Ascent', () => {
  it('shows every stage with its count and label, in order, one step higher each time', () => {
    render(
      <MemoryRouter>
        <Ascent stages={STAGES} />
      </MemoryRouter>,
    )

    const [wide] = screen.getAllByRole('list', { name: 'Pipeline' })
    const items = within(wide).getAllByRole('listitem')
    expect(items.map((item) => item.textContent)).toEqual(['4Saved', '6Applied', '1Offer'])
    const heights = items.map((item) => parseFloat((item.querySelector('[data-step]') as HTMLElement).style.height))
    expect(heights).toEqual([...heights].sort((a, b) => a - b))
    expect(new Set(heights).size).toBe(heights.length)
  })

  it('links a stage when it has a target and names the link with the count', () => {
    render(
      <MemoryRouter>
        <Ascent stages={STAGES} />
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

describe('Ascent counts', () => {
  it('can hide the counts for the sign-in panel', () => {
    render(
      <MemoryRouter>
        <Ascent stages={STAGES} inverse showCounts={false} />
      </MemoryRouter>,
    )

    const [wide] = screen.getAllByRole('list', { name: 'Pipeline' })
    expect(within(wide).getAllByRole('listitem').map((item) => item.textContent)).toEqual(['Saved', 'Applied', 'Offer'])
  })
})
