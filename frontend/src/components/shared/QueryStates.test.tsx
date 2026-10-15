import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ApiError } from '@/lib/api-client'
import { ErrorState } from '@/components/shared/QueryStates'

describe('ErrorState', () => {
  afterEach(() => vi.restoreAllMocks())

  it('explains a network failure and retries', async () => {
    const onRetry = vi.fn()
    render(<ErrorState title="Couldn't load" error={new TypeError('Failed to fetch')} onRetry={onRetry} />)

    expect(screen.getByText("Can't reach the server")).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Try again' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })

  it('says the server had a problem on a 500', () => {
    render(<ErrorState title="Couldn't load" error={new ApiError(500, 'x')} />)
    expect(screen.getByText('The server had a problem')).toBeInTheDocument()
  })

  it('keeps the caller title for other errors', () => {
    render(<ErrorState title="Couldn't load" error={new ApiError(404, 'x')} />)
    expect(screen.getByText("Couldn't load")).toBeInTheDocument()
  })

  it('says offline when the browser is offline', () => {
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false)
    render(<ErrorState title="Couldn't load" />)
    expect(screen.getByText("You're offline")).toBeInTheDocument()
  })
})
