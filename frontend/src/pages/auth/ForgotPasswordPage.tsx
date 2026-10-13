import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { requestPasswordReset } from '@/lib/auth-api'
import { Button } from '@/components/ui/button'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await requestPasswordReset(email)
      setSubmitted(true)
    } catch {
      setError('Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame>
      <AuthIntro title="Reset your password" subtitle="We will email you a link that expires in one hour." />
      <div className="space-y-4">
          {submitted ? (
            <p role="status" className="text-sm">
              If an account exists for that address, a reset link is on its way.
            </p>
          ) : (
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
              {error && (
                <p role="alert" className="text-sm text-destructive">
                  {error}
                </p>
              )}
              <Button type="submit" size="xl" className="w-full" disabled={isSubmitting || email.trim() === ''}>
                Send reset link
              </Button>
            </form>
          )}
          <p className="mt-4 text-center text-sm text-muted-foreground">
            <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
              Back to log in
            </Link>
          </p>
        </div>
    </AuthFrame>
  )
}
