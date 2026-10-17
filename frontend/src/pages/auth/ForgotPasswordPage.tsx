import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Loader2 } from 'lucide-react'
import { requestPasswordReset } from '@/lib/auth-api'
import { email as emailRule } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { FormError, FormField } from '@/components/shared/Form'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import type { ResetPasswordState } from '@/pages/auth/ResetPasswordPage'

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    const problem = emailRule(email.trim())
    if (problem) {
      setError(problem)
      return
    }
    setIsSubmitting(true)
    try {
      await requestPasswordReset(email.trim())
      navigate('/reset-password', { state: { email: email.trim() } satisfies ResetPasswordState })
    } catch {
      setError('Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame>
      <AuthIntro title="Reset your password" subtitle="We will email you a 6-digit code that expires in 10 minutes." />
      <form onSubmit={handleSubmit} className="space-y-4" noValidate aria-busy={isSubmitting}>
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
        {error && <FormError>{error}</FormError>}
        <Button type="submit" size="xl" className="w-full" disabled={isSubmitting || email.trim() === ''}>
          {isSubmitting && <Loader2 aria-hidden className="animate-spin" />}
          Send code
        </Button>
      </form>
      <p className="mt-4 text-center text-sm text-muted-foreground">
        <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
          Back to log in
        </Link>
      </p>
    </AuthFrame>
  )
}
