import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { ProductTour } from '@/components/onboarding/ProductTour'
import { useAuth } from '@/hooks/use-auth'
import { isTourDone, replayTour, TOUR_STEPS } from '@/lib/tour'

vi.mock('@/hooks/use-auth')

function Where() {
  return <p data-testid="where">{useLocation().pathname}</p>
}

function renderTour() {
  return render(
    <MemoryRouter initialEntries={['/']}>
      <ProductTour />
      <Where />
    </MemoryRouter>,
  )
}

describe('ProductTour', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'user-1', email: 'a@example.com', display_name: 'A', onboarded: true },
      isLoading: false,
      isAuthenticated: true,
      sessionMessage: null,
      login: vi.fn(),
      register: vi.fn(),
      logout: vi.fn(),
    } as unknown as ReturnType<typeof useAuth>)
  })

  it('starts on first run and walks through the areas', async () => {
    const user = userEvent.setup()
    renderTour()

    expect(await screen.findByRole('complementary', { name: 'Product tour' })).toHaveTextContent(`Step 1 of ${TOUR_STEPS.length}`)
    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(screen.getByRole('complementary')).toHaveTextContent('Applications')
    expect(screen.getByTestId('where')).toHaveTextContent('/applications')
  })

  it('covers every area in order, with Back disabled on the first step', async () => {
    const user = userEvent.setup()
    renderTour()
    await screen.findByRole('complementary', { name: 'Product tour' })

    expect(TOUR_STEPS.map((step) => step.path)).toEqual([
      '/', '/applications', '/interviews', '/prep', '/resumes', '/directory', '/offers', '/goals', '/recommendations', '/privacy', '/help',
    ])
    expect(screen.getByRole('button', { name: 'Back' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(screen.getByRole('button', { name: 'Back' })).toBeEnabled()
    expect(screen.getByRole('link', { name: /Open Applications and board/ })).toHaveAttribute('href', '/applications')
  })

  it('stays in the app on the help step and links to it', async () => {
    const user = userEvent.setup()
    renderTour()
    await screen.findByRole('complementary', { name: 'Product tour' })

    for (let index = 0; index < TOUR_STEPS.length - 1; index += 1) {
      await user.click(screen.getByRole('button', { name: 'Next' }))
    }

    expect(screen.getByRole('complementary')).toHaveTextContent('Help')
    expect(screen.getByTestId('where')).toHaveTextContent('/privacy')
    expect(screen.getByRole('link', { name: 'Open Help' })).toHaveAttribute('href', '/help')
    await user.click(screen.getByRole('button', { name: 'Done' }))
    expect(isTourDone('user-1')).toBe(true)
  })

  it('closes on Escape', async () => {
    const user = userEvent.setup()
    renderTour()
    await screen.findByRole('complementary', { name: 'Product tour' })

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()
    expect(isTourDone('user-1')).toBe(true)
  })

  it('can be skipped and stays dismissed for that user', async () => {
    const user = userEvent.setup()
    const { unmount } = renderTour()

    await user.click(await screen.findByRole('button', { name: 'Skip tour' }))
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()
    expect(isTourDone('user-1')).toBe(true)

    unmount()
    renderTour()
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()
  })

  it('is not shown to a user who finished it before, and replays on request', async () => {
    window.localStorage.setItem('cohort:tour-done:user-1', '1')
    renderTour()
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()

    replayTour()

    await waitFor(() => expect(screen.getByRole('complementary')).toHaveTextContent(TOUR_STEPS[0].title))
  })

  it('does not show when onboarding is not finished', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 'user-2', onboarded: false },
    } as unknown as ReturnType<typeof useAuth>)
    renderTour()
    expect(screen.queryByRole('complementary')).not.toBeInTheDocument()
  })
})
