import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import { createResume, getResume, updateResume, type ResumeFormInput } from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'

export function ResumeFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['resume', id],
    queryFn: () => getResume(id!),
    enabled: isEditing,
  })

  const [name, setName] = useState('')
  const [versionLabel, setVersionLabel] = useState('')
  const [targetRole, setTargetRole] = useState('')
  const [summary, setSummary] = useState('')
  const [isDefault, setIsDefault] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (detailQuery.data) {
      setName(detailQuery.data.name)
      setVersionLabel(detailQuery.data.version_label ?? '')
      setTargetRole(detailQuery.data.target_role ?? '')
      setSummary(detailQuery.data.summary ?? '')
      setIsDefault(detailQuery.data.is_default)
    }
  }, [detailQuery.data])

  const mutation = useMutation({
    mutationFn: (input: ResumeFormInput) => (isEditing ? updateResume(id!, input) : createResume(input)),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['resumes'] })
      queryClient.invalidateQueries({ queryKey: ['resume', result.id] })
      navigate(`/resumes/${result.id}`)
    },
    onError: (mutationError: unknown) => {
      if (mutationError instanceof ApiError) {
        setError(mutationError.message)
        setFieldErrors(mutationError.fieldErrors ?? {})
      } else {
        setError('Something went wrong. Please try again.')
      }
    },
  })

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    mutation.mutate({
      name,
      version_label: versionLabel || null,
      target_role: targetRole || null,
      summary: summary || null,
      is_default: isDefault,
    })
  }

  if (isEditing && detailQuery.isLoading) {
    return (
      <div className="max-w-2xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-72 w-full" />
      </div>
    )
  }

  if (isEditing && detailQuery.isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load this resume</AlertTitle>
        <AlertDescription>It may have been deleted, or something went wrong.</AlertDescription>
      </Alert>
    )
  }

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>{isEditing ? 'Edit resume' : 'Create a resume'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="name">Name</Label>
              <Input id="name" value={name} onChange={(event) => setName(event.target.value)} required />
              {fieldErrors.name && <p className="text-sm text-destructive">{fieldErrors.name}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="versionLabel">Version label</Label>
              <Input
                id="versionLabel"
                placeholder="v1"
                value={versionLabel}
                onChange={(event) => setVersionLabel(event.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="targetRole">Target role</Label>
              <Input id="targetRole" value={targetRole} onChange={(event) => setTargetRole(event.target.value)} />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="summary">Summary</Label>
            <Textarea id="summary" value={summary} onChange={(event) => setSummary(event.target.value)} rows={4} />
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={isDefault}
              onChange={(event) => setIsDefault(event.target.checked)}
              className="h-4 w-4 rounded border-input"
            />
            Set as my default resume
          </label>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create resume'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
