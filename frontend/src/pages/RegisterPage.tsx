import { useEffect, useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { email as emailRule, hasErrors, minLength, required, validate } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { AuthFrame } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    document.title = 'Create account · Cohort'
  }, [])

  if (isAuthenticated) {
    return <Navigate to="/" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const problems = validate(
      { displayName, email, password },
      {
        displayName: [required('Display name')],
        email: [required('Email'), emailRule],
        password: [required('Password'), minLength('Password', 10)],
      },
    )
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    setIsSubmitting(true)
    try {
      await register(email, password, displayName)
      navigate('/', { replace: true })
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message)
        setFieldErrors(err.fieldErrors ?? {})
      } else {
        setError('Something went wrong. Please try again.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  function handleBlur(field: 'email' | 'password') {
    const rule = field === 'email' ? emailRule : minLength('Password', 10)
    const value = field === 'email' ? email : password
    if (value !== '') {
      const message = rule(value)
      setFieldErrors((current) => {
        const next = { ...current }
        if (message) {
          next[field] = message
        } else {
          delete next[field]
        }
        return next
      })
    }
  }

  return (
    <AuthFrame>
      <div className="mb-8">
        <h1 className="text-display text-3xl">Create your Cohort account</h1>
        <p className="mt-2 text-sm text-muted-foreground">Your data stays private to you, always.</p>
      </div>
      <OAuthButtons />
      <form onSubmit={handleSubmit} className="space-y-5" noValidate aria-busy={isSubmitting}>
        <div className="space-y-2">
          <Label htmlFor="displayName">Display name</Label>
          <Input
            id="displayName"
            autoComplete="name"
            className="h-10"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            aria-invalid={fieldErrors.displayName ? true : undefined}
            aria-describedby={fieldErrors.displayName ? 'displayName-error' : undefined}
            required
          />
          {fieldErrors.displayName && (
            <p id="displayName-error" className="text-sm text-destructive">
              {fieldErrors.displayName}
            </p>
          )}
        </div>
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
            onBlur={() => handleBlur('email')}
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
          <Label htmlFor="password">Password</Label>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            minLength={10}
            className="h-10"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            onBlur={() => handleBlur('password')}
            aria-invalid={fieldErrors.password ? true : undefined}
            aria-describedby={fieldErrors.password ? 'password-error password-hint' : 'password-hint'}
            required
          />
          <p id="password-hint" className="text-xs text-muted-foreground">
            At least 10 characters.
          </p>
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
        <Button type="submit" variant="cta" size="xl" className="w-full" disabled={isSubmitting}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
          Log in
        </Link>
      </p>
    </AuthFrame>
  )
}
