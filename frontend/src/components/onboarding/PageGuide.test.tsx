import { beforeEach, describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { GUIDE_IDS, PAGE_GUIDES } from '@/lib/page-guide-content'
import { isGuideSeen, markGuideSeen, resetAllGuides } from '@/lib/page-guide-state'

function renderGuide(signedIn = true) {
  const client = new QueryClient()
  if (signedIn) {
    client.setQueryData(['me'], { id: 'user-1' })
  }
  return render(
    <QueryClientProvider client={client}>
      <div data-guide="outcome">Outcome chips</div>
      <PageGuide guideId="debrief" />
    </QueryClientProvider>,
  )
}

describe('PageGuide', () => {
  beforeEach(() => {
    window.localStorage.clear()
    window.localStorage.setItem('cohort:tour-done:user-1', '1')
  })

  it('walks through the steps and puts focus back on the button', async () => {
    const user = userEvent.setup()
    renderGuide()
    const steps = PAGE_GUIDES.debrief.steps

    await user.click(screen.getByRole('button', { name: 'How this page works' }))
    const dialog = screen.getByRole('dialog', { name: PAGE_GUIDES.debrief.title })
    expect(dialog).toHaveFocus()
    expect(dialog).toHaveTextContent(`Step 1 of ${steps.length}`)
    expect(screen.getByRole('button', { name: 'Back' })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: 'Next' }))
    expect(dialog).toHaveTextContent(steps[1].title)

    await user.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'How this page works' })).toHaveFocus()
  })

  it('shows a centred card when the element is not on the page', async () => {
    const user = userEvent.setup()
    renderGuide()
    await user.click(screen.getByRole('button', { name: 'How this page works' }))
    for (let index = 0; index < PAGE_GUIDES.debrief.steps.length - 1; index += 1) {
      await user.click(screen.getByRole('button', { name: 'Next' }))
    }

    expect(screen.getByRole('dialog').className).toContain('-translate-x-1/2')
  })

  it('is offered once on a first visit and can be dismissed for good', async () => {
    const user = userEvent.setup()
    renderGuide()

    expect(screen.getByRole('region', { name: 'Page guide offer' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: "Don't show again" }))

    expect(screen.queryByRole('region', { name: 'Page guide offer' })).not.toBeInTheDocument()
    expect(isGuideSeen('user-1', 'debrief')).toBe(true)
  })

  it('is not offered again once seen, and is offered again after a reset', () => {
    markGuideSeen('user-1', 'debrief')
    const { unmount } = renderGuide()
    expect(screen.queryByRole('region', { name: 'Page guide offer' })).not.toBeInTheDocument()
    unmount()

    resetAllGuides('user-1')
    renderGuide()
    expect(screen.getByRole('region', { name: 'Page guide offer' })).toBeInTheDocument()
  })

  it('does not offer itself before the product tour is done', () => {
    window.localStorage.clear()
    renderGuide()
    expect(screen.queryByRole('region', { name: 'Page guide offer' })).not.toBeInTheDocument()
  })
})

describe('guide content', () => {
  it('has steps with a title and text for every guide', () => {
    for (const id of GUIDE_IDS) {
      expect(PAGE_GUIDES[id].steps.length).toBeGreaterThan(0)
      for (const step of PAGE_GUIDES[id].steps) {
        expect(step.title).not.toBe('')
        expect(step.body).not.toBe('')
      }
    }
  })
})
