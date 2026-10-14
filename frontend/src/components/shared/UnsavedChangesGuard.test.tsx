import { describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Link, RouterProvider, createMemoryRouter } from 'react-router-dom'
import { useUnsavedChanges } from '@/components/shared/UnsavedChangesGuard'

function Form() {
  const { guard } = useUnsavedChanges(true)
  return (
    <>
      {guard}
      <Link to="/elsewhere">go</Link>
    </>
  )
}

function renderForm() {
  const router = createMemoryRouter([
    { path: '/', element: <Form /> },
    { path: '/elsewhere', element: <p>other page</p> },
  ])
  render(<RouterProvider router={router} />)
}

describe('useUnsavedChanges', () => {
  it('asks the browser to confirm when the tab closes', () => {
    renderForm()
    const event = new Event('beforeunload', { cancelable: true })
    fireEvent(window, event)
    expect(event.defaultPrevented).toBe(true)
  })

  it('blocks in-app navigation until the user confirms', async () => {
    const user = userEvent.setup()
    renderForm()

    await user.click(screen.getByRole('link', { name: 'go' }))
    expect(await screen.findByRole('dialog', { name: 'Leave without saving?' })).toBeInTheDocument()
    expect(screen.queryByText('other page')).not.toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Stay on page' }))
    expect(screen.queryByText('other page')).not.toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: 'go' }))
    await user.click(await screen.findByRole('button', { name: 'Leave' }))
    expect(await screen.findByText('other page')).toBeInTheDocument()
  })
})
