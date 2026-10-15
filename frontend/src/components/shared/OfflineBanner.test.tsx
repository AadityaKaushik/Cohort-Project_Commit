import { afterEach, describe, expect, it, vi } from 'vitest'
import { act, render, screen } from '@testing-library/react'
import { OfflineBanner } from '@/components/shared/OfflineBanner'

function setOnline(value: boolean) {
  vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(value)
  act(() => {
    window.dispatchEvent(new Event(value ? 'online' : 'offline'))
  })
}

describe('OfflineBanner', () => {
  afterEach(() => vi.restoreAllMocks())

  it('appears when the browser goes offline and hides on reconnect', () => {
    render(<OfflineBanner />)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()

    setOnline(false)
    expect(screen.getByRole('status')).toHaveTextContent("You're offline")

    setOnline(true)
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
