import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { CODE_LENGTH, CodeInput } from '@/components/auth/CodeInput'
import { ResendCodeButton } from '@/components/auth/ResendCodeButton'
import { FormError, FormField } from '@/components/shared/Form'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { requestEmailVerification } from '@/lib/auth-api'

/** Where to land after sign-up or a blocked log-in. The address travels in router state, never in the URL. */
export interface VerifyEmailState {
  email?: string
  sendCode?: boolean
}

export function VerifyEmailPage() {
  const { confirmEmail } = useAuth()
  const navigate = useNavigate()
  const state = (useLocation().state as VerifyEmailState | null) ?? {}
  const [email, setEmail] = useState(state.email ?? '')
  const [code, setCode] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const askedForCode = useRef(false)

  useEffect(() => {
    document.title = 'Confirm your email · Cohort'
  }, [])

  useEffect(() => {
    if (state.sendCode && state.email && !askedForCode.current) {
      askedForCode.current = true
      requestEmailVerification(state.email).catch(() => undefined)
    }
  }, [state.sendCode, state.email])

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (code.length !== CODE_LENGTH) {
      setError(`Enter all ${CODE_LENGTH} digits.`)
      return
    }
    setIsSubmitting(true)
    try {
      await confirmEmail(email.trim(), code)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
      setCode('')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame>
      <AuthIntro
        title="Check your email"
        subtitle={
          state.email
            ? `We sent a ${CODE_LENGTH}-digit code to ${state.email}. It expires in 10 minutes.`
            : `Enter your email and the ${CODE_LENGTH}-digit code we sent you.`
        }
      />
      <form onSubmit={handleSubmit} className="space-y-5" noValidate aria-busy={isSubmitting}>
        {!state.email && (
          <FormField id="email" label="Email">
            <Input
              id="email"
              type="email"
              autoComplete="email"
              className="h-10"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </FormField>
        )}
        <CodeInput value={code} onChange={setCode} invalid={Boolean(error)} disabled={isSubmitting} autoFocus={Boolean(state.email)} />
        {error && <FormError>{error}</FormError>}
        <Button type="submit" size="xl" className="w-full" disabled={isSubmitting || email.trim() === ''}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          {isSubmitting ? 'Checking…' : 'Confirm email'}
        </Button>
      </form>
      <div className="mt-4 space-y-3">
        {email.trim() !== '' && (
          <ResendCodeButton onResend={() => requestEmailVerification(email.trim())} startCoolingDown={Boolean(state.email)} />
        )}
        <p className="text-sm text-muted-foreground">
          Not your address?{' '}
          <Link to="/register" className="font-medium text-primary underline-offset-4 hover:underline">
            Sign up again
          </Link>{' '}
          or{' '}
          <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
            log in
          </Link>
          .
        </p>
      </div>
    </AuthFrame>
  )
}
