import { useState, type FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { CODE_LENGTH, CodeInput } from '@/components/auth/CodeInput'
import { PasswordInput } from '@/components/auth/PasswordInput'
import { ResendCodeButton } from '@/components/auth/ResendCodeButton'
import { FormError, FormField } from '@/components/shared/Form'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { ApiError } from '@/lib/api-client'
import { confirmPasswordReset, requestPasswordReset } from '@/lib/auth-api'

/** The address travels in router state from the forgot-password page, never in the URL. */
export interface ResetPasswordState {
  email?: string
}

export function ResetPasswordPage() {
  const state = (useLocation().state as ResetPasswordState | null) ?? {}
  const [email, setEmail] = useState(state.email ?? '')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (code.length !== CODE_LENGTH) {
      setError(`Enter all ${CODE_LENGTH} digits of the code.`)
      return
    }
    if (password.length < 10) {
      setError('Use at least 10 characters.')
      return
    }
    setIsSubmitting(true)
    try {
      await confirmPasswordReset({ email: email.trim(), code, new_password: password })
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  if (done) {
    return (
      <AuthFrame>
        <AuthIntro title="Password updated" subtitle="You are signed out on every device. Log in with the new password." />
        <Button asChild size="xl" className="w-full">
          <Link to="/login">Log in</Link>
        </Button>
      </AuthFrame>
    )
  }

  return (
    <AuthFrame>
      <AuthIntro
        title="Choose a new password"
        subtitle={
          state.email
            ? `If an account exists for ${state.email}, we sent it a ${CODE_LENGTH}-digit code.`
            : `Enter your email, the ${CODE_LENGTH}-digit code we sent you and a new password.`
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
        <CodeInput value={code} onChange={setCode} disabled={isSubmitting} autoFocus={Boolean(state.email)} />
        <FormField id="new-password" label="New password" hint="At least 10 characters.">
          <PasswordInput
            id="new-password"
            autoComplete="new-password"
            className="h-10"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </FormField>
        {error && <FormError>{error}</FormError>}
        <Button type="submit" size="xl" className="w-full" disabled={isSubmitting || email.trim() === ''}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          Update password
        </Button>
      </form>
      <div className="mt-4 space-y-3">
        {email.trim() !== '' && (
          <ResendCodeButton onResend={() => requestPasswordReset(email.trim())} startCoolingDown={Boolean(state.email)} />
        )}
        <p className="text-sm text-muted-foreground">
          <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
            Back to log in
          </Link>
        </p>
      </div>
    </AuthFrame>
  )
}
