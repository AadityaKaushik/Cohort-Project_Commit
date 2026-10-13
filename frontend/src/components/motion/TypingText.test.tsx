import { act, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { TypingText } from '@/components/motion/TypingText'
import { mockReducedMotion } from '@/test/a11y'

const originalMatchMedia = window.matchMedia

function advance(ms: number) {
  for (let elapsed = 0; elapsed < ms; elapsed += 10) {
    act(() => {
      vi.advanceTimersByTime(10)
    })
  }
}

function setHidden(hidden: boolean) {
  Object.defineProperty(document, 'hidden', { value: hidden, configurable: true })
  document.dispatchEvent(new Event('visibilitychange'))
}

describe('TypingText', () => {
  beforeEach(() => {
    sessionStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    window.matchMedia = originalMatchMedia
    setHidden(false)
  })

  it('shows the full text at once under reduced motion', () => {
    mockReducedMotion(true)
    render(<TypingText text="Track every application" />)

    expect(screen.getByText('Track every application')).toBeInTheDocument()
    expect(screen.queryByTestId('typing-text')).not.toBeInTheDocument()
  })

  it('types letter by letter while the full text stays available to screen readers', () => {
    mockReducedMotion(false)
    render(<TypingText text="Hello" />)

    expect(screen.getByText('Hello')).toBeInTheDocument()
    expect(screen.getByTestId('typing-text')).toHaveAttribute('aria-hidden', 'true')
    advance(60)
    expect(screen.getByTestId('typing-text').getAttribute('data-typed')).toBe('He')
    advance(500)
    expect(screen.queryByTestId('typing-text')).not.toBeInTheDocument()
    expect(screen.getByText('Hello')).not.toHaveClass('text-transparent')
  })

  it('pauses while the tab is hidden', () => {
    mockReducedMotion(false)
    render(<TypingText text="Paused line" />)

    act(() => setHidden(true))
    advance(1000)
    expect(screen.getByTestId('typing-text').getAttribute('data-typed')).toBe('')
    act(() => setHidden(false))
    advance(60)
    expect(screen.getByTestId('typing-text').getAttribute('data-typed')?.length).toBeGreaterThan(0)
  })

  it('types a keyed line only once per session', () => {
    mockReducedMotion(false)
    const first = render(<TypingText text="Once only" sessionKey="greeting" />)
    advance(1000)
    first.unmount()

    render(<TypingText text="Once only" sessionKey="greeting" />)
    expect(screen.queryByTestId('typing-text')).not.toBeInTheDocument()
    expect(screen.getByText('Once only')).toBeInTheDocument()
  })

  it('cycles through several phrases and exposes all of them to screen readers', () => {
    mockReducedMotion(false)
    render(<TypingText text={['One', 'Two']} />)

    expect(screen.getByText('One Two')).toHaveClass('sr-only')
    advance(100)
    expect(screen.getByTestId('typing-text').getAttribute('data-typed')).toBe('One')
  })
})
