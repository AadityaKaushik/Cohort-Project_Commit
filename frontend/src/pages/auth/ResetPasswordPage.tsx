import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ApiError } from '@/lib/api-client'
import { confirmPasswordReset } from '@/lib/auth-api'
import { Button } from '@/components/ui/button'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    if (password.length < 10) {
      setError('Use at least 10 characters.')
      return
    }
    setIsSubmitting(true)
    try {
      await confirmPasswordReset({ token, new_password: password })
      setDone(true)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Something went wrong. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthFrame>
      <AuthIntro title="Choose a new password" subtitle="Signing in again is required on every device." />
      <div className="space-y-4">
          {done ? (
            <p role="status" className="text-sm">
              Your password was updated.{' '}
              <Link to="/login" className="font-medium text-primary underline-offset-4 hover:underline">
                Log in
              </Link>
            </p>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4" noValidate>
              <div className="space-y-2">
                <Label htmlFor="new-password">New password</Label>
                <Input
                  id="new-password"
                  type="password"
                  autoComplete="new-password"
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
              <Button type="submit" size="xl" className="w-full" disabled={isSubmitting || token === ''}>
                Update password
              </Button>
            </form>
          )}
        </div>
    </AuthFrame>
  )
}
