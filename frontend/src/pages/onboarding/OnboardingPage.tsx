import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/use-auth'
import { useToast } from '@/hooks/use-toast'
import { ApiError } from '@/lib/api-client'
import { completeOnboarding } from '@/lib/users-api'
import { hasErrors, required, validate } from '@/lib/validation'
import { AuthFrame, AuthIntro } from '@/components/layout/AuthFrame'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormError, FormField } from '@/components/shared/Form'
import { CsvImport } from '@/components/applications/CsvImport'
import { LoadSampleDataButton } from '@/components/onboarding/LoadSampleDataButton'

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
  const [showImport, setShowImport] = useState(false)

  useEffect(() => {
    document.title = 'Get started · Cohort'
  }, [])

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
      <AuthIntro
        eyebrow="Getting started"
        title={`Welcome${user?.display_name ? `, ${user.display_name}` : ''}`}
        subtitle="Tell us what you are aiming for and we will set up a starter preparation plan, spread across the time you have."
      />
      <form onSubmit={handleSubmit} className="stagger space-y-5" noValidate>
        <FormField id="targetRole" label="Target role" error={errors.targetRole}>
          <Input id="targetRole" className="h-10" placeholder="Backend engineer" value={targetRole} onChange={(e) => setTargetRole(e.target.value)} />
        </FormField>
        <FormField id="targetDate" label="Be interview-ready by" error={errors.targetDate}>
          <Input id="targetDate" className="h-10" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
        </FormField>
        {serverError && <FormError>{serverError}</FormError>}
        <div className="flex flex-col gap-2 pt-1">
          <Button type="submit" size="xl" disabled={mutation.isPending} className="w-full">
            {mutation.isPending ? 'Creating your plan…' : 'Create my plan'}
          </Button>
          <Button type="button" variant="ghost" disabled={mutation.isPending} onClick={() => mutation.mutate({ skip: true })}>
            Skip for now
          </Button>
        </div>
      </form>
      <div className="mt-6 space-y-3 border-t pt-5">
        <LoadSampleDataButton className="w-full" onLoaded={() => mutation.mutate({ skip: true })} />
        {showImport ? (
          <CsvImport />
        ) : (
          <Button type="button" variant="outline" className="w-full" onClick={() => setShowImport(true)}>
            Import applications from a spreadsheet
          </Button>
        )}
      </div>
    </AuthFrame>
  )
}
