import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { format } from 'date-fns'
import { AlertTriangleIcon, ExternalLinkIcon, StickyNoteIcon, Trash2Icon } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { StatusBadge } from '@/components/shared/StatusBadge'
import { addCompanyNote, companyKeys, deleteCompanyNote, getCompany } from '@/lib/companies-api'
import { formatLabel } from '@/lib/labels'
import { textProblem } from '@/lib/text-quality'
import { useSoftWarnings } from '@/hooks/use-soft-warnings'
import { SoftWarning } from '@/components/shared/SoftWarning'

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border bg-card px-3 py-2">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="text-xl font-semibold tabular-nums">{value}</dd>
    </div>
  )
}

/** US-96: the aggregated view of one company. */
export function CompanyDetailPage() {
  const { id = '' } = useParams()
  const queryClient = useQueryClient()
  const [draft, setDraft] = useState('')
  const [draftError, setDraftError] = useState<string | null>(null)
  const softWarnings = useSoftWarnings()
  const query = useQuery({ queryKey: companyKeys.detail(id), queryFn: () => getCompany(id), enabled: id !== '' })
  const refresh = () => queryClient.invalidateQueries({ queryKey: companyKeys.all })
  const addNote = useMutation({
    mutationFn: (body: string) => addCompanyNote(id, body),
    onSuccess: () => {
      setDraft('')
      void refresh()
    },
  })
  const removeNote = useMutation({ mutationFn: (noteId: string) => deleteCompanyNote(id, noteId), onSuccess: refresh })

  if (query.isLoading) {
    return <LoadingRows rows={5} label="Loading company" />
  }
  if (query.isError || !query.data) {
    return (
      <EmptyState
        compact
        icon={AlertTriangleIcon}
        title="Company not found"
        description="This company does not exist or could not be loaded."
        action={
          <Button variant="outline" asChild>
            <Link to="/companies">Back to companies</Link>
          </Button>
        }
      />
    )
  }
  const company = query.data

  function submitNote(event?: FormEvent) {
    event?.preventDefault()
    if (draft.trim() === '') {
      return
    }
    const problem = textProblem(draft)
    setDraftError(problem?.message ?? null)
    if (problem || softWarnings.check('note', draft, true).length > 0) {
      return
    }
    addNote.mutate(draft)
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div>
        <h2 className="text-lg font-semibold">{company.name}</h2>
        <p className="flex flex-wrap items-center gap-x-3 text-sm text-muted-foreground">
          <span>{company.industry ?? 'Industry not set'}</span>
          {company.website && (
            <a
              href={company.website}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 text-primary hover:underline"
            >
              Website <ExternalLinkIcon aria-hidden className="size-3" />
            </a>
          )}
        </p>
      </div>

      <dl className="grid grid-cols-2 gap-2 sm:grid-cols-4">
        <Stat label="Applications" value={company.stats.applications} />
        <Stat label="Active" value={company.stats.active} />
        <Stat label="Interviews" value={company.stats.interviews} />
        <Stat label="Offers" value={company.stats.offers} />
      </dl>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Applications</CardTitle>
        </CardHeader>
        <CardContent>
          {company.applications.length === 0 ? (
            <p className="text-sm text-muted-foreground">No applications for this company.</p>
          ) : (
            <ul className="divide-y">
              {company.applications.map((application) => (
                <li key={application.id} className="flex flex-wrap items-center justify-between gap-2 py-2">
                  <Link to={`/applications/${application.id}`} className="text-sm font-medium hover:underline">
                    {application.job_title}
                  </Link>
                  <span className="flex items-center gap-2 text-xs text-muted-foreground">
                    {format(new Date(application.date_applied), 'd MMM yyyy')}
                    <StatusBadge status={application.status} />
                  </span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Contacts</CardTitle>
        </CardHeader>
        <CardContent>
          {company.contacts.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No contacts here yet. Add one from the <Link to="/contacts" className="text-primary hover:underline">contacts page</Link>.
            </p>
          ) : (
            <ul className="divide-y">
              {company.contacts.map((contact) => (
                <li key={contact.id} className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm">
                  <span className="font-medium">
                    {contact.name}
                    {contact.role_title && <span className="font-normal text-muted-foreground"> · {contact.role_title}</span>}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {formatLabel(contact.relationship)}
                    {contact.referral_status !== 'none' && ` · Referral ${formatLabel(contact.referral_status).toLowerCase()}`}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">Notes</CardTitle>
        </CardHeader>
        <CardContent className="space-y-3">
          <form onSubmit={submitNote} className="space-y-2">
            <Textarea
              aria-label="New note"
              placeholder="Culture, interview tips, questions to ask"
              rows={3}
              value={draft}
              onChange={(event) => {
                setDraft(event.target.value)
                setDraftError(null)
              }}
              onBlur={() => softWarnings.check('note', draft, true)}
            />
            {draftError && (
              <p role="alert" className="text-xs font-medium text-destructive">
                {draftError}
              </p>
            )}
            <SoftWarning
              id="note"
              messages={softWarnings.shown.note}
              onEdit={() => softWarnings.dismiss('note')}
              onSaveAnyway={() => {
                softWarnings.accept('note', draft)
                submitNote()
              }}
            />
            <Button type="submit" size="sm" disabled={addNote.isPending || draft.trim() === ''}>
              {addNote.isPending ? 'Saving…' : 'Add note'}
            </Button>
          </form>
          {company.notes.length === 0 ? (
            <EmptyState
              compact
              icon={StickyNoteIcon}
              title="No notes yet"
              description="Keep what you learn about this company in one place."
            />
          ) : (
            <ul className="space-y-2">
              {company.notes.map((note) => (
                <li key={note.id} className="flex items-start gap-2 rounded-lg border bg-surface-2/50 p-3">
                  <div className="min-w-0 flex-1">
                    <p className="text-sm whitespace-pre-wrap">{note.body}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{format(new Date(note.created_at), 'd MMM yyyy')}</p>
                  </div>
                  <Button
                    variant="ghost"
                    size="icon-sm"
                    aria-label="Delete note"
                    onClick={() => removeNote.mutate(note.id)}
                    disabled={removeNote.isPending}
                  >
                    <Trash2Icon className="size-4" />
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
