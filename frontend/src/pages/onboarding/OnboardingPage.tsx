import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/use-auth'
import { useToast } from '@/hooks/use-toast'
import { ApiError } from '@/lib/api-client'
import { completeOnboarding } from '@/lib/users-api'
import { hasErrors, required, validate } from '@/lib/validation'
import { AuthFrame } from '@/components/layout/AuthFrame'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

function todayIso() {
  return new Date().toISOString().slice(0, 10)
}

export function OnboardingPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [targetRole, setTargetRole] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [serverError, setServerError] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: (input: Parameters<typeof completeOnboarding>[0]) => completeOnboarding(input),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['me'] })
      queryClient.invalidateQueries({ queryKey: ['preparation-tasks'] })
      toast({
        title: result.tasks_created > 0 ? 'Your starter plan is ready' : 'You are all set',
        description: result.tasks_created > 0 ? `${result.tasks_created} preparation tasks were added.` : undefined,
      })
      navigate('/', { replace: true })
    },
    onError: (error: unknown) => {
      setServerError(error instanceof ApiError ? error.message : 'Something went wrong. Please try again.')
    },
  })

  if (user?.onboarded) {
    return <Navigate to="/" replace />
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setServerError(null)
    const problems = validate({ targetRole, targetDate }, { targetRole: [required('Target role')], targetDate: [required('Target date')] })
    if (targetDate && targetDate <= todayIso()) {
      problems.targetDate = 'Choose a date in the future.'
    }
    setErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate({ target_role: targetRole.trim(), target_date: targetDate })
  }

  return (
    <AuthFrame>
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle>Welcome{user?.display_name ? `, ${user.display_name}` : ''}</CardTitle>
          <CardDescription>
            Tell us what you are aiming for and we will set up a starter preparation plan, spread across the time you have.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-2">
              <Label htmlFor="targetRole">Target role</Label>
              <Input id="targetRole" placeholder="Backend engineer" value={targetRole} onChange={(e) => setTargetRole(e.target.value)} />
              {errors.targetRole && <p className="text-sm text-destructive">{errors.targetRole}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="targetDate">Be interview-ready by</Label>
              <Input id="targetDate" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
              {errors.targetDate && <p className="text-sm text-destructive">{errors.targetDate}</p>}
            </div>
            {serverError && (
              <p role="alert" className="text-sm text-destructive">
                {serverError}
              </p>
            )}
            <div className="flex flex-col gap-2 sm:flex-row-reverse">
              <Button type="submit" disabled={mutation.isPending} className="sm:flex-1">
                Create my plan
              </Button>
              <Button
                type="button"
                variant="ghost"
                disabled={mutation.isPending}
                onClick={() => mutation.mutate({ skip: true })}
              >
                Skip for now
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </AuthFrame>
  )
}
