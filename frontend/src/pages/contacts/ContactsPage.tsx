import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { formatDistanceToNow } from 'date-fns'
import { AlertTriangleIcon, PlusIcon, SnowflakeIcon, Trash2Icon, UsersIcon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { EmailDraftPanel } from '@/components/ai/EmailDraftPanel'
import type { ContactDraftKind } from '@/lib/ai-api'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { listApplications } from '@/lib/applications-api'
import {
  contactKeys,
  createContact,
  deleteContact,
  listContacts,
  markContactContacted,
  REFERRAL_STATUSES,
  RELATIONSHIPS,
  updateContact,
  type Contact,
  type ContactRelationship,
  type ReferralStatus,
} from '@/lib/contacts-api'
import { formatLabel } from '@/lib/labels'
import { cn } from '@/lib/utils'

type Filter = ReferralStatus | 'all'

function ContactDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient()
  const [name, setName] = useState('')
  const [roleTitle, setRoleTitle] = useState('')
  const [email, setEmail] = useState('')
  const [relationship, setRelationship] = useState<ContactRelationship>('other')
  const [applicationId, setApplicationId] = useState('')
  const [notes, setNotes] = useState('')
  const applications = useQuery({
    queryKey: ['applications', 'contact-picker'],
    queryFn: () => listApplications({ size: 100 }),
    enabled: open,
  })
  const create = useMutation({
    mutationFn: createContact,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: contactKeys.all })
      setName('')
      setRoleTitle('')
      setEmail('')
      setRelationship('other')
      setApplicationId('')
      setNotes('')
      onOpenChange(false)
    },
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    create.mutate({
      name,
      role_title: roleTitle || undefined,
      email: email || undefined,
      relationship,
      application_id: applicationId || undefined,
      notes: notes || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Add contact</DialogTitle>
            <DialogDescription>Someone who can open a door, give advice, or refer you.</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="contact-name">Name</Label>
            <Input id="contact-name" required value={name} onChange={(event) => setName(event.target.value)} />
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="contact-role">Role</Label>
              <Input id="contact-role" value={roleTitle} onChange={(event) => setRoleTitle(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="contact-email">Email</Label>
              <Input id="contact-email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="contact-relationship">Relationship</Label>
              <NativeSelect
                id="contact-relationship"
                value={relationship}
                onChange={(event) => setRelationship(event.target.value as ContactRelationship)}
              >
                {RELATIONSHIPS.map((value) => (
                  <option key={value} value={value}>
                    {formatLabel(value)}
                  </option>
                ))}
              </NativeSelect>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="contact-application">Application</Label>
              <NativeSelect id="contact-application" value={applicationId} onChange={(event) => setApplicationId(event.target.value)}>
                <option value="">None</option>
                {applications.data?.items.map((application) => (
                  <option key={application.id} value={application.id}>
                    {application.company.name} · {application.job_title}
                  </option>
                ))}
              </NativeSelect>
            </div>
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="contact-notes">Notes</Label>
            <Textarea id="contact-notes" rows={3} value={notes} onChange={(event) => setNotes(event.target.value)} />
          </div>
          {create.isError && <p role="alert" className="text-sm text-destructive">Could not save this contact.</p>}
          <DialogFooter>
            <Button type="submit" disabled={create.isPending || name.trim() === ''}>
              {create.isPending ? 'Saving…' : 'Save contact'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function ContactCard({ contact }: { contact: Contact }) {
  const queryClient = useQueryClient()
  const refresh = () => queryClient.invalidateQueries({ queryKey: contactKeys.all })
  const changeStatus = useMutation({
    mutationFn: (status: ReferralStatus) => updateContact(contact.id, { referral_status: status }),
    onSuccess: refresh,
  })
  const contacted = useMutation({ mutationFn: () => markContactContacted(contact.id), onSuccess: refresh })
  const remove = useMutation({ mutationFn: () => deleteContact(contact.id), onSuccess: refresh })
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [draftKind, setDraftKind] = useState<ContactDraftKind | null>(null)
  const draftKinds: ContactDraftKind[] = ['referral_request', 'reconnect', 'referral_thanks']

  return (
    <Card className="py-0">
      <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
        <div className="min-w-0 flex-1 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="truncate text-sm font-semibold">{contact.name}</h3>
            <Badge variant="secondary">{formatLabel(contact.relationship)}</Badge>
            {contact.cold && (
              <Badge className="bg-warning-soft text-warning-soft-foreground">
                <SnowflakeIcon aria-hidden className="size-3" />
                Gone cold
              </Badge>
            )}
          </div>
          <p className="truncate text-xs text-muted-foreground">
            {[contact.role_title, contact.company_name].filter(Boolean).join(' · ') || 'No company linked'}
            {' · '}
            {contact.last_contacted_at
              ? `Last contacted ${formatDistanceToNow(new Date(contact.last_contacted_at), { addSuffix: true })}`
              : 'Not contacted yet'}
          </p>
          {contact.notes && <p className="line-clamp-2 text-xs text-muted-foreground">{contact.notes}</p>}
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <NativeSelect
            aria-label={`Referral status for ${contact.name}`}
            className="w-36"
            value={contact.referral_status}
            onChange={(event) => changeStatus.mutate(event.target.value as ReferralStatus)}
          >
            {REFERRAL_STATUSES.map((value) => (
              <option key={value} value={value}>
                {value === 'none' ? 'No referral' : `Referral ${formatLabel(value).toLowerCase()}`}
              </option>
            ))}
          </NativeSelect>
          <Button variant="outline" size="sm" onClick={() => setDraftKind('referral_request')}>
            Draft a referral request
          </Button>
          <Button variant="outline" size="sm" onClick={() => setDraftKind('reconnect')}>
            Reconnect
          </Button>
          <Button variant="outline" size="sm" onClick={() => contacted.mutate()} disabled={contacted.isPending}>
            Mark contacted
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={`Delete ${contact.name}`}
            onClick={() => setConfirmOpen(true)}
          >
            <Trash2Icon aria-hidden className="size-4" />
          </Button>
        </div>
      </CardContent>
      <Dialog open={draftKind !== null} onOpenChange={(open) => !open && setDraftKind(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Message for {contact.name}</DialogTitle>
            <DialogDescription>A draft built from what you saved about this contact. Edit it and copy it. Nothing is sent.</DialogDescription>
          </DialogHeader>
          {draftKind && (
            <EmailDraftPanel
              contactId={contact.id}
              kinds={[draftKind, ...draftKinds.filter((kind) => kind !== draftKind)]}
            />
          )}
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Delete this contact?"
        description={<>This deletes <strong>{contact.name}</strong> from your contacts. This cannot be undone.</>}
        pending={remove.isPending}
        onConfirm={() => remove.mutate()}
      />
    </Card>
  )
}

/** US-95: contacts and referral tracking. */
export function ContactsPage() {
  const [filter, setFilter] = useState<Filter>('all')
  const [dialogOpen, setDialogOpen] = useState(false)
  const query = useQuery({
    queryKey: contactKeys.list(filter),
    queryFn: () => listContacts(filter === 'all' ? undefined : filter),
  })
  const contacts = query.data ?? []

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Contacts</h2>
          <p className="text-sm text-muted-foreground">Recruiters, referrers and peers, and where each referral stands.</p>
        </div>
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon className="size-4" />
          Add contact
        </Button>
      </div>

      <div className="flex flex-wrap gap-1" role="group" aria-label="Filter by referral status">
        {(['all', ...REFERRAL_STATUSES] as Filter[]).map((value) => (
          <Button
            key={value}
            variant="ghost"
            size="sm"
            aria-pressed={filter === value}
            className={cn(filter === value && 'bg-accent text-accent-foreground')}
            onClick={() => setFilter(value)}
          >
            {value === 'all' ? 'All' : value === 'none' ? 'No referral' : formatLabel(value)}
          </Button>
        ))}
      </div>

      {query.isLoading ? (
        <LoadingRows rows={4} label="Loading contacts" />
      ) : query.isError ? (
        <EmptyState
          compact
          icon={AlertTriangleIcon}
          title="Couldn't load contacts"
          description="Something went wrong while fetching your contacts."
          action={<Button variant="outline" onClick={() => query.refetch()}>Try again</Button>}
        />
      ) : contacts.length === 0 ? (
        <EmptyState
          compact
          icon={UsersIcon}
          title={filter === 'all' ? 'No contacts yet' : 'No contacts with this referral status'}
          description="Add the people who can help, and we will nudge you when one goes quiet for a month."
          action={filter === 'all' ? <Button onClick={() => setDialogOpen(true)}>Add your first contact</Button> : undefined}
        />
      ) : (
        <div className="space-y-2">
          {contacts.map((contact) => (
            <ContactCard key={contact.id} contact={contact} />
          ))}
        </div>
      )}

      <ContactDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </div>
  )
}
