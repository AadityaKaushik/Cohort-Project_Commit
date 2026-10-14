import { afterEach, describe, expect, it } from 'vitest'
import { renderHook } from '@testing-library/react'
import { useCountUp, useReducedMotion } from '@/hooks/use-motion'
import { mockReducedMotion } from '@/test/a11y'

const originalMatchMedia = window.matchMedia

describe('reduced motion', () => {
  afterEach(() => {
    window.matchMedia = originalMatchMedia
  })

  it('reports the preference and makes count-ups land on their final value immediately', () => {
    mockReducedMotion(true)

    expect(renderHook(() => useReducedMotion()).result.current).toBe(true)
    expect(renderHook(() => useCountUp(42)).result.current.text).toBe('42')
  })

  it('starts count-ups from zero when motion is allowed', () => {
    mockReducedMotion(false)

    expect(renderHook(() => useCountUp(42)).result.current.text).toBe('0')
  })

})
