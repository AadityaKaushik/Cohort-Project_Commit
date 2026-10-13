import { afterEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { TransitionLink } from '@/components/motion/TransitionLink'
import { runRouteTransition } from '@/lib/route-transition'
import { mockReducedMotion } from '@/test/a11y'

const prefetch = vi.hoisted(() => vi.fn())
vi.mock('@/lib/route-chunks', () => ({ prefetchRoute: prefetch }))

function fakeViewTransitions() {
  const start = vi.fn((update: () => void) => {
    update()
    return { finished: Promise.resolve(), ready: Promise.resolve(), updateCallbackDone: Promise.resolve(), skipTransition: () => {} }
  })
  Object.defineProperty(document, 'startViewTransition', { value: start, configurable: true, writable: true })
  return start
}

describe('route transitions', () => {
  afterEach(() => {
    Reflect.deleteProperty(document, 'startViewTransition')
    delete document.documentElement.dataset.routeTransition
    prefetch.mockReset()
  })

  it('swaps instantly without a view transition under reduced motion', () => {
    mockReducedMotion(true)
    const start = fakeViewTransitions()
    const update = vi.fn()

    runRouteTransition(update)

    expect(update).toHaveBeenCalledOnce()
    expect(start).not.toHaveBeenCalled()
    expect(document.documentElement.dataset.routeTransition).toBeUndefined()
  })

  it('wraps the swap in a view transition when motion is allowed', () => {
    mockReducedMotion(false)
    const start = fakeViewTransitions()
    const update = vi.fn()

    runRouteTransition(update)

    expect(start).toHaveBeenCalledOnce()
    expect(update).toHaveBeenCalledOnce()
    expect(document.documentElement.dataset.routeTransition).toBe('true')
  })

  it('falls back to an instant swap where the browser has no view transitions', () => {
    mockReducedMotion(false)
    const update = vi.fn()

    runRouteTransition(update)

    expect(update).toHaveBeenCalledOnce()
  })

  it('prefetches the target on intent and navigates on click', async () => {
    mockReducedMotion(true)
    const user = userEvent.setup()
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<TransitionLink to="/login">Log in</TransitionLink>} />
          <Route path="/login" element={<h1>Sign-in form</h1>} />
        </Routes>
      </MemoryRouter>,
    )

    await user.hover(screen.getByRole('link', { name: 'Log in' }))
    expect(prefetch).toHaveBeenCalledWith('/login')

    await user.click(screen.getByRole('link', { name: 'Log in' }))
    expect(screen.getByRole('heading', { name: 'Sign-in form' })).toBeInTheDocument()
  })
})
