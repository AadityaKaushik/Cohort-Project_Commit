import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useQueryClient } from '@tanstack/react-query'
import { CircleHelpIcon, XIcon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { PAGE_GUIDES, type GuideId } from '@/lib/page-guide-content'
import { isGuideSeen, markGuideSeen } from '@/lib/page-guide-state'
import { isTourDone } from '@/lib/tour'
import type { UserProfile } from '@/lib/users-api'

const POPOVER_WIDTH = 320
const GAP = 12

interface Placement {
  top: number
  left: number
  ring: { top: number; left: number; width: number; height: number } | null
}

function findTarget(target: string | undefined): HTMLElement | null {
  if (!target) return null
  return document.querySelector<HTMLElement>(`[data-guide="${target}"]`)
}

function place(element: HTMLElement | null, height: number): Placement | null {
  if (!element) return null
  const rect = element.getBoundingClientRect()
  const width = Math.min(POPOVER_WIDTH, window.innerWidth - 32)
  const below = rect.bottom + GAP + height <= window.innerHeight
  const top = below ? rect.bottom + GAP : Math.max(16, rect.top - GAP - height)
  const left = Math.min(Math.max(16, rect.left), window.innerWidth - width - 16)
  return { top, left, ring: { top: rect.top - 4, left: rect.left - 4, width: rect.width + 8, height: rect.height + 8 } }
}

export function PageGuide({ guideId }: { guideId: GuideId }) {
  const userId = useQueryClient().getQueryData<UserProfile>(['me'])?.id
  const guide = PAGE_GUIDES[guideId]
  const [step, setStep] = useState<number | null>(null)
  const [offered, setOffered] = useState(false)
  const [placement, setPlacement] = useState<Placement | null>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const cardRef = useRef<HTMLDivElement>(null)
  const open = step !== null
  const current = step === null ? null : guide.steps[step]

  useEffect(() => {
    if (userId && !isGuideSeen(userId, guideId) && isTourDone(userId)) {
      setOffered(true)
    }
  }, [userId, guideId])

  const close = useCallback(() => {
    if (userId) markGuideSeen(userId, guideId)
    setStep(null)
    setOffered(false)
    triggerRef.current?.focus()
  }, [userId, guideId])

  function dismissOffer() {
    if (userId) markGuideSeen(userId, guideId)
    setOffered(false)
  }

  const target = current?.target
  const reposition = useCallback(() => {
    setPlacement(place(findTarget(target), cardRef.current?.offsetHeight ?? 180))
  }, [target])

  useLayoutEffect(() => {
    if (!open) return
    findTarget(target)?.scrollIntoView({ block: 'center', behavior: 'auto' })
    reposition()
    cardRef.current?.focus()
  }, [open, step, target, reposition])

  useEffect(() => {
    if (!open) return
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') close()
    }
    window.addEventListener('keydown', onKey)
    window.addEventListener('resize', reposition)
    window.addEventListener('scroll', reposition, true)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('resize', reposition)
      window.removeEventListener('scroll', reposition, true)
    }
  }, [open, close, reposition])

  const last = step === guide.steps.length - 1
  const cardStyle = placement
    ? { top: placement.top, left: placement.left, width: Math.min(POPOVER_WIDTH, window.innerWidth - 32) }
    : undefined

  return (
    <>
      <Button ref={triggerRef} type="button" variant="outline" size="sm" onClick={() => setStep(0)}>
        <CircleHelpIcon aria-hidden />
        How this page works
      </Button>
      {offered && !open && createPortal(
        <section
          aria-label="Page guide offer"
          className="surface-raised fixed right-4 bottom-20 z-40 w-[min(20rem,calc(100vw-2rem))] space-y-3 rounded-xl border p-4 shadow-lg md:bottom-6 md:right-6"
        >
          <p className="text-sm font-medium">This page has a few features worth a quick look.</p>
          <div className="flex flex-wrap gap-2">
            <Button type="button" size="sm" onClick={() => setStep(0)}>
              Show me
            </Button>
            <Button type="button" size="sm" variant="outline" onClick={() => setOffered(false)}>
              Not now
            </Button>
            <Button type="button" size="sm" variant="ghost" onClick={dismissOffer}>
              Don't show again
            </Button>
          </div>
        </section>,
        document.body,
      )}
      {open && current && createPortal(
        <>
          {placement?.ring && (
            <div
              aria-hidden
              className="pointer-events-none fixed z-50 rounded-lg ring-2 ring-primary"
              style={placement.ring}
            />
          )}
          <div
            ref={cardRef}
            role="dialog"
            aria-modal="false"
            aria-label={guide.title}
            tabIndex={-1}
            style={cardStyle}
            className={
              'surface-raised fixed z-50 space-y-3 rounded-xl border p-4 shadow-lg outline-none ' +
              (placement ? '' : 'top-1/2 left-1/2 w-[min(20rem,calc(100vw-2rem))] -translate-x-1/2 -translate-y-1/2')
            }
          >
            <div className="flex items-start justify-between gap-2">
              <div>
                <p className="text-xs text-muted-foreground">
                  Step {step + 1} of {guide.steps.length}
                </p>
                <h2 className="text-base font-semibold tracking-tight">{current.title}</h2>
              </div>
              <button
                type="button"
                onClick={close}
                aria-label="Close guide"
                className="rounded-md p-1 text-muted-foreground transition-colors hover:text-foreground"
              >
                <XIcon className="size-4" />
              </button>
            </div>
            <p aria-live="polite" className="text-sm text-muted-foreground">
              {current.body}
            </p>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <Button type="button" variant="ghost" size="sm" onClick={close}>
                Don't show again
              </Button>
              <div className="flex gap-2">
                <Button type="button" variant="outline" size="sm" disabled={step === 0} onClick={() => setStep(step - 1)}>
                  Back
                </Button>
                <Button type="button" size="sm" onClick={() => (last ? close() : setStep(step + 1))}>
                  {last ? 'Done' : 'Next'}
                </Button>
              </div>
            </div>
          </div>
        </>,
        document.body,
      )}
    </>
  )
}
