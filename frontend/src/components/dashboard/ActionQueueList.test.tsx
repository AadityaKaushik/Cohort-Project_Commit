import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { ActionQueueList } from '@/components/dashboard/ActionQueueList'
import type { ActionItem } from '@/lib/action-queue'

describe('ActionQueueList', () => {
  it('shows an empty state when nothing needs attention', () => {
    render(
      <MemoryRouter>
        <ActionQueueList items={[]} />
      </MemoryRouter>,
    )

    expect(screen.getByText(/nothing needs attention/i)).toBeInTheDocument()
  })

  it('renders each item as a link straight to its action', () => {
    const items: ActionItem[] = [
      { id: 'a', type: 'stalled', title: 'Follow up: Acme', subtitle: 'Stalled at Applied — 20 days', href: '/applications/1' },
      { id: 'b', type: 'due_task', title: 'Practice arrays', subtitle: 'Due Jan 1', href: '/prep' },
    ]

    render(
      <MemoryRouter>
        <ActionQueueList items={items} />
      </MemoryRouter>,
    )

    const followUpLink = screen.getByRole('link', { name: /follow up: acme/i })
    expect(followUpLink).toHaveAttribute('href', '/applications/1')

    const taskLink = screen.getByRole('link', { name: /practice arrays/i })
    expect(taskLink).toHaveAttribute('href', '/prep')
  })
})
