import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { confirmEmailVerification } from '@/lib/auth-api'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'

type State = 'working' | 'verified' | 'failed'

export function VerifyEmailPage() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [state, setState] = useState<State>(token ? 'working' : 'failed')

  useEffect(() => {
    if (!token) {
      return
    }
    let cancelled = false
    confirmEmailVerification(token)
      .then(() => !cancelled && setState('verified'))
      .catch(() => !cancelled && setState('failed'))
    return () => {
      cancelled = true
    }
  }, [token])

  return (
    <AuthFrame>
      <AuthIntro title="Email verification" />
      <div className="space-y-4 space-y-4 text-sm">
          {state === 'working' && <p role="status">Verifying your email address.</p>}
          {state === 'verified' && <p role="status">Your email address is verified.</p>}
          {state === 'failed' && <p role="alert">This link is invalid or has expired.</p>}
          <Link to="/" className="font-medium text-primary underline-offset-4 hover:underline">
            Continue to Cohort
          </Link>
        </div>
    </AuthFrame>
  )
}
