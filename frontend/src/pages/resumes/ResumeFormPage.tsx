import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createResume, getResume, updateResume, type ResumeFormInput } from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { hasErrors, required, validate } from '@/lib/validation'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { CheckboxField, FormActions, FormError, FormField, FormSection } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { ErrorState, PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'

export function ResumeFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { toast } = useToast()

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
      toast({ title: isEditing ? 'Resume updated' : 'Resume created' })
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
    const problems = validate({ name }, { name: [required('Name')] })
    setFieldErrors(problems)
    if (hasErrors(problems)) {
      return
    }
    mutation.mutate({
      name,
      version_label: versionLabel || null,
      target_role: targetRole || null,
      summary: summary || null,
      is_default: isDefault,
    })
  }

  if (isEditing && detailQuery.isLoading) {
    return <PageSkeleton label="Loading resume" variant="form" />
  }

  if (isEditing && detailQuery.isError) {
    return (
      <ErrorState title="Couldn't load this resume" description="It may have been deleted, or something went wrong." onRetry={() => detailQuery.refetch()} />
    )
  }

  return (
    <div className="max-w-4xl space-y-6">
      <PageHeader
        back={isEditing ? { to: `/resumes/${id}`, label: 'Resume' } : { to: '/resumes', label: 'Resumes' }}
        title={isEditing ? 'Edit resume' : 'Create a resume'}
        description="Name each version for the kind of role it targets, then upload its file."
      />
      <SectionCard>
        <form onSubmit={handleSubmit} className="space-y-6" noValidate>
          <FormSection title="Version" description="Name for this version.">
            <FormField id="name" label="Name" error={fieldErrors.name} className="sm:col-span-2">
              <Input id="name" value={name} onChange={(event) => setName(event.target.value)} required />
            </FormField>
            <FormField id="versionLabel" label="Version label" error={fieldErrors.versionLabel}>
              <Input id="versionLabel" placeholder="v1" value={versionLabel} onChange={(event) => setVersionLabel(event.target.value)} />
            </FormField>
            <FormField id="targetRole" label="Target role" error={fieldErrors.targetRole}>
              <Input id="targetRole" value={targetRole} onChange={(event) => setTargetRole(event.target.value)} />
            </FormField>
          </FormSection>
          <FormSection title="Content" description="A short summary of what this version emphasises." columns={1}>
            <FormField id="summary" label="Summary">
              <Textarea id="summary" value={summary} onChange={(event) => setSummary(event.target.value)} rows={4} />
            </FormField>
            <CheckboxField checked={isDefault} onChange={setIsDefault} label="Set as my default resume" />
          </FormSection>
          {error && <FormError>{error}</FormError>}
          <FormActions>
            <Button type="button" variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
            <Button type="submit" disabled={mutation.isPending}>
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create resume'}
            </Button>
          </FormActions>
        </form>
      </SectionCard>
    </div>
  )
}
