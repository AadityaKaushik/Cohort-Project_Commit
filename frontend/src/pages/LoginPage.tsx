import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams, type Location } from 'react-router-dom'
import { AlertCircle, CheckIcon, Loader2 } from 'lucide-react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { email as emailRule, hasErrors, required, validate, type FieldErrors } from '@/lib/validation'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { FieldMessage, FormError, FormField } from '@/components/shared/Form'
import { TransitionLink } from '@/components/motion/TransitionLink'
import { finishSignIn } from '@/lib/route-transition'
import { prefetchRoute } from '@/lib/route-chunks'

export function LoginPage() {
  const { login, isAuthenticated, sessionMessage } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const oauthError = searchParams.get('oauth_error')

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<FieldErrors<'email' | 'password'>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [signedIn, setSignedIn] = useState(false)
  const [errorCount, setErrorCount] = useState(0)
  const [signingIn, setSigningIn] = useState(false)

  useEffect(() => {
    document.title = 'Log in · Cohort'
    prefetchRoute('/')
  }, [])

  const from = (location.state as { from?: Location } | null)?.from
  const redirectTo = from ? `${from.pathname}${from.search}${from.hash}` : '/'

  if (isAuthenticated && !signingIn) {
    return <Navigate to={redirectTo} replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const problems = validate(
      { email, password },
      { email: [required('Email'), emailRule], password: [required('Password')] },
    )
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    setIsSubmitting(true)
    setSigningIn(true)
    try {
      await login(email, password)
      setSignedIn(true)
      finishSignIn(() => navigate(redirectTo, { replace: true }))
    } catch (err) {
      setSigningIn(false)
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
      setErrorCount((count) => count + 1)
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleEmailBlur() {
    if (email !== '') {
      setFieldErrors((current) => ({ ...current, email: emailRule(email) ?? undefined }))
    }
  }

  return (
    <AuthFrame>
      <AuthIntro title="Log in to Cohort" subtitle="Welcome back. Pick up where you left off." />
      {sessionMessage && (
        <Alert variant="destructive" className="mb-5">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Session ended</AlertTitle>
          <AlertDescription>{sessionMessage}</AlertDescription>
        </Alert>
      )}
      {oauthError && (
        <Alert variant="destructive" className="mb-5">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Sign-in was not completed</AlertTitle>
          <AlertDescription>
            {oauthError === 'unverified_email'
              ? 'That provider has not verified your email address, so it cannot be linked to an existing account. Log in with your password instead.'
              : oauthError === 'missing_email'
                ? 'That provider did not share an email address.'
                : 'The provider sign-in did not complete. Please try again.'}
          </AlertDescription>
        </Alert>
      )}
      <OAuthButtons />
      <form onSubmit={handleSubmit} className="space-y-5" noValidate aria-busy={isSubmitting}>
        <FormField id="email" label="Email" error={fieldErrors.email}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            className="h-10"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            onBlur={handleEmailBlur}
            required
          />
        </FormField>
        <div className="space-y-1.5">
          <div className="flex items-center justify-between gap-3">
            <Label htmlFor="password">Password</Label>
            <Link to="/forgot-password" className="rounded text-xs font-medium text-primary underline-offset-4 hover:underline">
              Forgot password?
            </Link>
          </div>
          <PasswordInput
            id="password"
            autoComplete="current-password"
            className="h-10"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            aria-invalid={fieldErrors.password ? true : undefined}
            aria-describedby={fieldErrors.password ? 'password-error' : undefined}
            required
          />
          {fieldErrors.password && <FieldMessage id="password-error">{fieldErrors.password}</FieldMessage>}
        </div>
        {error && (
          <div key={errorCount} className="animate-shake">
            <FormError>{error}</FormError>
          </div>
        )}
        <Button type="submit" size="xl" variant={signedIn ? 'cta' : 'default'} className="w-full" disabled={isSubmitting || signedIn}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          {signedIn && <CheckIcon aria-hidden className="animate-enter" />}
          {signedIn ? 'Signed in' : isSubmitting ? 'Logging in…' : 'Log in'}
        </Button>
      </form>
      <p className="mt-8 text-center text-sm text-muted-foreground">
        New to Cohort?{' '}
        <TransitionLink to="/register" className="rounded font-medium text-primary underline-offset-4 hover:underline">
          Create account
        </TransitionLink>
      </p>
    </AuthFrame>
  )
}
