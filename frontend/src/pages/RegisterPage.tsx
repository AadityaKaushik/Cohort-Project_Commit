import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { CheckIcon, Loader2 } from 'lucide-react'
import { OAuthButtons } from '@/components/auth/OAuthButtons'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { useAuth } from '@/hooks/use-auth'
import { ApiError } from '@/lib/api-client'
import { email as emailRule, hasErrors, minLength, required, validate } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { FormError, FormField } from '@/components/shared/Form'
import { TransitionLink } from '@/components/motion/TransitionLink'
import { finishSignIn } from '@/lib/route-transition'
import { prefetchRoute } from '@/lib/route-chunks'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'

export function RegisterPage() {
  const { register, isAuthenticated } = useAuth()
  const navigate = useNavigate()

  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [signedIn, setSignedIn] = useState(false)
  const [errorCount, setErrorCount] = useState(0)
  const [signingIn, setSigningIn] = useState(false)

  useEffect(() => {
    document.title = 'Create account · Cohort'
    prefetchRoute('/')
  }, [])

  if (isAuthenticated && !signingIn) {
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
    setSigningIn(true)
    try {
      await register(email, password, displayName)
      setSignedIn(true)
      finishSignIn(() => navigate('/', { replace: true }))
    } catch (err) {
      setSigningIn(false)
      setErrorCount((count) => count + 1)
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
      <AuthIntro title="Create your Cohort account" subtitle="Free to use. Your data stays private to you, always." />
      <OAuthButtons />
      <form onSubmit={handleSubmit} className="space-y-5" noValidate aria-busy={isSubmitting}>
        <FormField id="displayName" label="Display name" error={fieldErrors.displayName}>
          <Input
            id="displayName"
            autoComplete="name"
            className="h-10"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            required
          />
        </FormField>
        <FormField id="email" label="Email" error={fieldErrors.email}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@example.com"
            className="h-10"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            onBlur={() => handleBlur('email')}
            required
          />
        </FormField>
        <FormField id="password" label="Password" hint="At least 10 characters." error={fieldErrors.password}>
          <PasswordInput
            id="password"
            autoComplete="new-password"
            minLength={10}
            className="h-10"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            onBlur={() => handleBlur('password')}
            required
          />
        </FormField>
        {error && (
          <div key={errorCount} className="animate-shake">
            <FormError>{error}</FormError>
          </div>
        )}
        <Button type="submit" size="xl" variant={signedIn ? 'cta' : 'default'} className="w-full" disabled={isSubmitting || signedIn}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          {signedIn && <CheckIcon aria-hidden className="animate-enter" />}
          {signedIn ? 'Account created' : isSubmitting ? 'Creating account…' : 'Create account'}
        </Button>
      </form>
      <p className="mt-8 text-center text-sm text-muted-foreground">
        Already have an account?{' '}
        <TransitionLink to="/login" className="rounded font-medium text-primary underline-offset-4 hover:underline">
          Log in
        </TransitionLink>
      </p>
    </AuthFrame>
  )
}
