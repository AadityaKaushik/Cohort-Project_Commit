import { afterEach, describe, expect, it } from 'vitest'
import { render, renderHook, screen } from '@testing-library/react'
import { useCountUp, useReducedMotion } from '@/hooks/use-motion'
import { RotatingQuote } from '@/components/shared/RotatingQuote'
import { TypingText } from '@/components/motion/TypingText'
import { mockReducedMotion } from '@/test/a11y'

const originalMatchMedia = window.matchMedia

describe('reduced motion', () => {
  afterEach(() => {
    window.matchMedia = originalMatchMedia
  })

  it('reports the preference and makes count-ups land on their final value immediately', () => {
    mockReducedMotion(true)

    expect(renderHook(() => useReducedMotion()).result.current).toBe(true)
    expect(renderHook(() => useCountUp(42)).result.current).toBe(42)
  })

  it('starts count-ups from zero when motion is allowed', () => {
    mockReducedMotion(false)

    expect(renderHook(() => useCountUp(42)).result.current).toBe(0)
  })

  it('shows typed text in full, with its whole content available to screen readers', () => {
    mockReducedMotion(true)
    render(<TypingText phrases={['First line.', 'Second line.']} />)

    expect(screen.getByTestId('typing-text')).toHaveTextContent('First line.')
    expect(screen.getByText('First line. Second line.')).toHaveClass('sr-only')
  })

  it('renders a quote that holds still', () => {
    mockReducedMotion(true)
    render(<RotatingQuote />)

    expect(screen.getByTestId('rotating-quote').textContent?.length).toBeGreaterThan(10)
  })
})
