import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircleIcon } from 'lucide-react'
import {
  createApplication,
  getApplication,
  updateApplication,
  type ApplicationFormInput,
  type ApplicationStatusValue,
} from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { Textarea } from '@/components/ui/textarea'

const CREATE_STATUS_OPTIONS: ApplicationStatusValue[] = ['SAVED', 'APPLIED', 'OA', 'INTERVIEW', 'FINAL_ROUND', 'OFFER']

export function ApplicationFormPage() {
  const { id } = useParams()
  const isEditing = Boolean(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const detailQuery = useQuery({
    queryKey: ['application', id],
    queryFn: () => getApplication(id!),
    enabled: isEditing,
  })

  const [companyName, setCompanyName] = useState('')
  const [jobTitle, setJobTitle] = useState('')
  const [dateApplied, setDateApplied] = useState('')
  const [status, setStatus] = useState<ApplicationStatusValue>('SAVED')
  const [location, setLocation] = useState('')
  const [jobUrl, setJobUrl] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})

  useEffect(() => {
    if (detailQuery.data) {
      setCompanyName(detailQuery.data.company.name)
      setJobTitle(detailQuery.data.job_title)
      setDateApplied(detailQuery.data.date_applied)
      setLocation(detailQuery.data.location ?? '')
      setJobUrl(detailQuery.data.job_url ?? '')
      setNotes(detailQuery.data.notes ?? '')
    }
  }, [detailQuery.data])

  const mutation = useMutation({
    mutationFn: (input: ApplicationFormInput) => (isEditing ? updateApplication(id!, input) : createApplication(input)),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['application', result.id] })
      navigate(`/applications/${result.id}`)
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
      company_name: companyName,
      job_title: jobTitle,
      date_applied: dateApplied,
      status: isEditing ? undefined : status,
      location: location || null,
      job_url: jobUrl || null,
      notes: notes || null,
    })
  }

  if (isEditing && detailQuery.isLoading) {
    return (
      <div className="max-w-2xl space-y-4">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    )
  }

  if (isEditing && detailQuery.isError) {
    return (
      <Alert variant="destructive">
        <AlertCircleIcon className="h-4 w-4" />
        <AlertTitle>Couldn&apos;t load this application</AlertTitle>
        <AlertDescription>It may have been deleted, or something went wrong.</AlertDescription>
      </Alert>
    )
  }

  return (
    <Card className="max-w-2xl">
      <CardHeader>
        <CardTitle>{isEditing ? 'Edit application' : 'Create an application'}</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4" noValidate>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="companyName">Company</Label>
              <Input id="companyName" value={companyName} onChange={(event) => setCompanyName(event.target.value)} required />
              {fieldErrors.companyName && <p className="text-sm text-destructive">{fieldErrors.companyName}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="jobTitle">Job title</Label>
              <Input id="jobTitle" value={jobTitle} onChange={(event) => setJobTitle(event.target.value)} required />
              {fieldErrors.jobTitle && <p className="text-sm text-destructive">{fieldErrors.jobTitle}</p>}
            </div>
            <div className="space-y-2">
              <Label htmlFor="dateApplied">Date applied</Label>
              <Input
                id="dateApplied"
                type="date"
                value={dateApplied}
                onChange={(event) => setDateApplied(event.target.value)}
                required
              />
              {fieldErrors.dateApplied && <p className="text-sm text-destructive">{fieldErrors.dateApplied}</p>}
            </div>
            {!isEditing && (
              <div className="space-y-2">
                <Label htmlFor="status">Status</Label>
                <Select value={status} onValueChange={(value) => setStatus(value as ApplicationStatusValue)}>
                  <SelectTrigger id="status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CREATE_STATUS_OPTIONS.map((option) => (
                      <SelectItem key={option} value={option}>
                        {option}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="location">Location</Label>
              <Input id="location" value={location} onChange={(event) => setLocation(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="jobUrl">Job URL</Label>
              <Input id="jobUrl" type="url" value={jobUrl} onChange={(event) => setJobUrl(event.target.value)} />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="notes">Notes</Label>
            <Textarea id="notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={4} />
          </div>
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
              {mutation.isPending ? 'Saving…' : isEditing ? 'Save changes' : 'Create application'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
