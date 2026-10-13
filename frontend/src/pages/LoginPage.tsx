import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, useSearchParams, type Location } from 'react-router-dom'
import { AlertCircle } from 'lucide-react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { email as emailRule, hasErrors, required, validate } from '@/lib/validation'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
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
  const [isSubmitting, setIsSubmitting] = useState(false)

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
    if (hasErrors(problems)) {
      setError(problems.email ?? problems.password ?? null)
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

  return (
    <AuthFrame>
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Log in to Cohort</CardTitle>
          <CardDescription>Track your campaign, prepare smarter.</CardDescription>
        </CardHeader>
        <CardContent>
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
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                required
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </div>
            {error && (
              <p role="alert" className="text-sm text-destructive">
                {error}
              </p>
            )}
            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? 'Logging in…' : 'Log in'}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            <Link to="/forgot-password" className="font-medium text-primary underline-offset-4 hover:underline">
              Forgot your password?
            </Link>
          </p>
          <p className="mt-2 text-center text-sm text-muted-foreground">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="font-medium text-primary underline-offset-4 hover:underline">
              Register
            </Link>
          </p>
        </CardContent>
      </Card>
    </AuthFrame>
  )
}
