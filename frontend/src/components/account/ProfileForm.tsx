import { useEffect, useState, type FormEvent } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '@/hooks/use-auth'
import { useToast } from '@/hooks/use-toast'
import { ApiError } from '@/lib/api-client'
import { updateMyProfile } from '@/lib/users-api'
import { email as emailRule, hasErrors, required, validate } from '@/lib/validation'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

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
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['me'] })
      toast({ title: 'Profile saved' })
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
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Profile</CardTitle>
        <CardDescription>This shapes your starter plan and how Cohort addresses you.</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="grid gap-4 sm:grid-cols-2" noValidate>
          <div className="space-y-1.5">
            <Label htmlFor="profile-display-name">Display name</Label>
            <Input id="profile-display-name" value={displayName} onChange={(e) => setDisplayName(e.target.value)} />
            {errors.displayName && <p className="text-sm text-destructive">{errors.displayName}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="profile-email">Email</Label>
            <Input id="profile-email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
            {errors.email && <p className="text-sm text-destructive">{errors.email}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="profile-role">Target role</Label>
            <Input id="profile-role" value={targetRole} onChange={(e) => setTargetRole(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="profile-target-date">Interview-ready by</Label>
            <Input id="profile-target-date" type="date" value={targetDate} onChange={(e) => setTargetDate(e.target.value)} />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="profile-grad-year">Graduation year</Label>
            <Input id="profile-grad-year" inputMode="numeric" value={graduationYear} onChange={(e) => setGraduationYear(e.target.value)} />
            {errors.graduationYear && <p className="text-sm text-destructive">{errors.graduationYear}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="profile-timezone">Time zone</Label>
            <Input id="profile-timezone" placeholder="Europe/Berlin" value={timezone} onChange={(e) => setTimezone(e.target.value)} />
          </div>
          {serverError && (
            <p role="alert" className="text-sm text-destructive sm:col-span-2">
              {serverError}
            </p>
          )}
          <div className="sm:col-span-2">
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : 'Save profile'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
