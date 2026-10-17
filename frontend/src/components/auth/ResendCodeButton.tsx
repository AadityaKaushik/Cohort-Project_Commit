import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/button'

export const RESEND_COOLDOWN_SECONDS = 60

/** Asks for a new code, then waits out the same cooldown the server applies before allowing another. */
export function ResendCodeButton({ onResend, startCoolingDown = true }: { onResend: () => Promise<void>; startCoolingDown?: boolean }) {
  const [secondsLeft, setSecondsLeft] = useState(startCoolingDown ? RESEND_COOLDOWN_SECONDS : 0)
  const [message, setMessage] = useState<string | null>(null)
  const [sending, setSending] = useState(false)

  useEffect(() => {
    if (secondsLeft <= 0) return
    const timer = window.setTimeout(() => setSecondsLeft((current) => current - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [secondsLeft])

  async function resend() {
    setSending(true)
    setMessage(null)
    try {
      await onResend()
      setMessage('A new code is on its way.')
      setSecondsLeft(RESEND_COOLDOWN_SECONDS)
    } catch {
      setMessage('Could not send a new code. Try again in a minute.')
    } finally {
      setSending(false)
    }
  }

  return (
    <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
      <Button type="button" variant="link" className="h-auto px-0" disabled={sending || secondsLeft > 0} onClick={resend}>
        {secondsLeft > 0 ? `Send a new code in ${secondsLeft}s` : 'Send a new code'}
      </Button>
      {message && (
        <span role="status" className="text-muted-foreground">
          {message}
        </span>
      )}
    </div>
  )
}
