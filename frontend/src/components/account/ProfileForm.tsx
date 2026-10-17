import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/use-auth'
import { useToast } from '@/hooks/use-toast'
import { ApiError } from '@/lib/api-client'
import { updateMyProfile } from '@/lib/users-api'
import { email as emailRule, hasErrors, required, validate } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { FormActions, FormError, FormField } from '@/components/shared/Form'
import { SectionCard } from '@/components/shared/SectionCard'

export function ProfileForm() {
  const { user } = useAuth()
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [targetRole, setTargetRole] = useState('')
  const [targetDate, setTargetDate] = useState('')
  const [graduationYear, setGraduationYear] = useState('')
  const [timezone, setTimezone] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [serverError, setServerError] = useState<string | null>(null)

  useEffect(() => {
    if (user) {
      setDisplayName(user.display_name ?? '')
      setEmail(user.email ?? '')
      setTargetRole(user.target_role ?? '')
      setTargetDate(user.target_date ?? '')
      setGraduationYear(user.graduation_year ? String(user.graduation_year) : '')
      setTimezone(user.timezone ?? '')
    }
  }, [user])

  const mutation = useMutation({
    mutationFn: (input: Parameters<typeof updateMyProfile>[0]) => updateMyProfile(input),
    onSuccess: (_saved, input) => {
      const emailChanged = Boolean(user) && input.email?.toLowerCase() !== user?.email.toLowerCase()
      queryClient.invalidateQueries({ queryKey: ['me'] })
      toast(
        emailChanged
          ? { title: 'Profile saved', description: `We sent a code to ${input.email}. Next time you log in with your password, enter it to confirm the new address.` }
          : { title: 'Profile saved' },
      )
    },
    onError: (error: unknown) => {
      setServerError(error instanceof ApiError ? error.message : 'Could not save your profile.')
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setServerError(null)
    const problems = validate(
      { displayName, email, graduationYear },
      {
        displayName: [required('Display name')],
        email: [required('Email'), emailRule],
        graduationYear: [(value) => (value === '' || /^\d{4}$/.test(value) ? null : 'Use a four-digit year, like 2027.')],
      },
    )
    setErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate({
      display_name: displayName.trim(),
      email: email.trim(),
      target_role: targetRole.trim() || null,
      target_date: targetDate || null,
      graduation_year: graduationYear ? Number(graduationYear) : null,
      timezone: timezone.trim() || null,
    })
  }

  return (
    <SectionCard title="Profile" description="Used for your starter plan and display name.">
      <form onSubmit={handleSubmit} className="space-y-4" noValidate>
        <div className="grid gap-4 sm:grid-cols-2">
          <FormField id="profile-display-name" label="Display name" error={errors.displayName}>
            <Input id="profile-display-name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
          </FormField>
          <FormField id="profile-email" label="Email" error={errors.email}>
            <Input id="profile-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </FormField>
          <FormField id="profile-role" label="Target role">
            <Input id="profile-role" value={targetRole} onChange={(e) => setTargetRole(e.target.value)} />
          </FormField>
          <FormField id="profile-target-date" label="Interview-ready by">
            <Input id="profile-target-date" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          </FormField>
          <FormField id="profile-grad-year" label="Graduation year" error={errors.graduationYear}>
            <Input id="profile-grad-year" inputMode="numeric" value={graduationYear} onChange={(e) => setGraduationYear(e.target.value)} />
          </FormField>
          <FormField id="profile-timezone" label="Time zone">
            <Input id="profile-timezone" placeholder="Europe/Berlin" value={timezone} onChange={(e) => setTimezone(e.target.value)} />
          </FormField>
        </div>
        {serverError && <FormError>{serverError}</FormError>}
        <FormActions>
          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? 'Saving…' : 'Save profile'}
          </Button>
        </FormActions>
      </form>
    </SectionCard>
  )
}
