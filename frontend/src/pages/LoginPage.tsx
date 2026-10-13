import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams, type Location } from 'react-router-dom'
import { AlertCircle, Loader2 } from 'lucide-react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { email as emailRule, hasErrors, required, validate, type FieldErrors } from '@/lib/validation'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { AuthFrame } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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

  useEffect(() => {
    document.title = 'Log in · Cohort'
  }, [])

  if (isAuthenticated) {
    const redirectTo = (location.state as { from?: Location } | null)?.from?.pathname ?? '/'
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
    try {
      await login(email, password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
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
      <div className="mb-8">
        <h1 className="text-display text-3xl">Log in to Cohort</h1>
        <p className="mt-2 text-sm text-muted-foreground">Welcome back. Pick up where you left off.</p>
      </div>
      {sessionMessage && (
        <Alert variant="destructive" className="mb-4">
          <AlertCircle className="h-4 w-4" />
          <AlertTitle>Session ended</AlertTitle>
          <AlertDescription>{sessionMessage}</AlertDescription>
        </Alert>
      )}
      {oauthError && (
        <Alert variant="destructive" className="mb-4">
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
        <div className="space-y-2">
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            className="h-10"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            onBlur={handleEmailBlur}
            aria-invalid={fieldErrors.email ? true : undefined}
            aria-describedby={fieldErrors.email ? 'email-error' : undefined}
            required
          />
          {fieldErrors.email && (
            <p id="email-error" className="text-sm text-destructive">
              {fieldErrors.email}
            </p>
          )}
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <Label htmlFor="password">Password</Label>
            <Link to="/forgot-password" className="text-xs font-medium text-primary underline-offset-4 hover:underline">
              Forgot your password?
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
          {fieldErrors.password && (
            <p id="password-error" className="text-sm text-destructive">
              {fieldErrors.password}
            </p>
          )}
        </div>
        {error && (
          <p role="alert" className="rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            {error}
          </p>
        )}
        <Button type="submit" size="xl" className="w-full" disabled={isSubmitting}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          {isSubmitting ? 'Logging in…' : 'Log in'}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        Don&apos;t have an account?{' '}
        <Link to="/register" className="font-medium text-primary underline-offset-4 hover:underline">
          Register
        </Link>
      </p>
    </AuthFrame>
  )
}
