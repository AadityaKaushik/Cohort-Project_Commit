import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { XIcon } from 'lucide-react'
import { useAuth } from '@/hooks/use-auth'
import { Button } from '@/components/ui/button'
import { TOUR_REPLAY_EVENT, TOUR_STEPS, isTourDone, markTourDone } from '@/lib/tour'

export function ProductTour() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const userId = user?.id
  const [step, setStep] = useState<number | null>(null)

  useEffect(() => {
    if (userId && user?.onboarded && !isTourDone(userId)) {
      setStep(0)
    }
  }, [userId, user?.onboarded])

  useEffect(() => {
    function replay() {
      setStep(0)
      navigate(TOUR_STEPS[0].path)
    }
    window.addEventListener(TOUR_REPLAY_EVENT, replay)
    return () => window.removeEventListener(TOUR_REPLAY_EVENT, replay)
  }, [navigate])

  useEffect(() => {
    if (step === null) return
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape' && !event.defaultPrevented) {
        if (userId) markTourDone(userId)
        setStep(null)
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [step, userId])

  if (step === null || !userId) {
    return null
  }

  const current = TOUR_STEPS[step]
  const isLast = step === TOUR_STEPS.length - 1

  function finish() {
    markTourDone(userId as string)
    setStep(null)
  }

  function go(next: number) {
    setStep(next)
    if (!TOUR_STEPS[next].outsideShell) {
      navigate(TOUR_STEPS[next].path)
    }
  }

  return (
    <aside
      role="complementary"
      aria-label="Product tour"
      className="surface-raised fixed right-4 bottom-20 z-40 w-[min(22rem,calc(100vw-2rem))] space-y-3 rounded-xl border p-4 shadow-lg md:bottom-6 md:right-6"
    >
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-xs text-muted-foreground">
            Step {step + 1} of {TOUR_STEPS.length}
          </p>
          <h2 className="text-base font-semibold tracking-tight">{current.title}</h2>
        </div>
        <button
          type="button"
          onClick={finish}
          aria-label="Close tour"
          className="rounded-md p-1 text-muted-foreground transition-colors hover:text-foreground"
        >
          <XIcon className="size-4" />
        </button>
      </div>
      <div aria-live="polite" className="space-y-2">
        <p className="text-sm text-muted-foreground">{current.body}</p>
        <Link to={current.path} className="inline-block text-sm font-medium text-primary underline-offset-4 hover:underline">
          Open {current.title}
        </Link>
      </div>
      <div className="flex items-center justify-between gap-2">
        <Button type="button" variant="ghost" size="sm" onClick={finish}>
          Skip tour
        </Button>
        <div className="flex gap-2">
          <Button type="button" variant="outline" size="sm" disabled={step === 0} onClick={() => go(step - 1)}>
            Back
          </Button>
          <Button type="button" size="sm" onClick={() => (isLast ? finish() : go(step + 1))}>
            {isLast ? 'Done' : 'Next'}
          </Button>
        </div>
      </div>
    </aside>
  )
}
