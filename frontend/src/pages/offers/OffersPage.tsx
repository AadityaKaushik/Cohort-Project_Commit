import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangleIcon, HandCoinsIcon, PlusIcon, Trash2Icon } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { OfferDecisionHelper } from '@/components/offers/OfferDecisionHelper'
import { EmailDraftPanel } from '@/components/ai/EmailDraftPanel'
import { NativeSelect } from '@/components/ui/native-select'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import { listApplications } from '@/lib/applications-api'
import { formatLabel } from '@/lib/labels'
import {
  createOffer,
  deleteOffer,
  countdownLabel,
  listOffers,
  offerKeys,
  OFFER_STATUSES,
  updateOffer,
  type Offer,
  type OfferStatus,
} from '@/lib/offers-api'

function money(value: number | null, currency: string) {
  if (value === null) {
    return '—'
  }
  return new Intl.NumberFormat('en', { style: 'currency', currency, maximumFractionDigits: 0 }).format(value)
}

function OfferDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const queryClient = useQueryClient()
  const [applicationId, setApplicationId] = useState('')
  const [baseSalary, setBaseSalary] = useState('')
  const [bonus, setBonus] = useState('')
  const [equity, setEquity] = useState('')
  const [signing, setSigning] = useState('')
  const [currency, setCurrency] = useState('USD')
  const [deadline, setDeadline] = useState('')
  const applications = useQuery({
    queryKey: ['applications', 'offer-picker'],
    queryFn: () => listApplications({ size: 100 }),
    enabled: open,
  })
  const create = useMutation({
    mutationFn: createOffer,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: offerKeys.all })
      setApplicationId('')
      setBaseSalary('')
      setBonus('')
      setEquity('')
      setSigning('')
      setDeadline('')
      onOpenChange(false)
    },
  })

  function number(value: string) {
    return value === '' ? undefined : Number(value)
  }

  function submit(event: FormEvent) {
    event.preventDefault()
    create.mutate({
      application_id: applicationId,
      base_salary: Number(baseSalary),
      bonus: number(bonus),
      equity_value: number(equity),
      signing_bonus: number(signing),
      currency,
      response_deadline: deadline || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <form onSubmit={submit} className="space-y-4">
          <DialogHeader>
            <DialogTitle>Add offer</DialogTitle>
            <DialogDescription>Amounts are yearly. Equity is your own estimate per year.</DialogDescription>
          </DialogHeader>
          <div className="space-y-1.5">
            <Label htmlFor="offer-application">Application</Label>
            <NativeSelect id="offer-application" required value={applicationId} onChange={(event) => setApplicationId(event.target.value)}>
              <option value="">Select an application</option>
              {applications.data?.items.map((application) => (
                <option key={application.id} value={application.id}>
                  {application.company.name} · {application.job_title}
                </option>
              ))}
            </NativeSelect>
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="offer-base">Base salary</Label>
              <Input id="offer-base" type="number" min="0" required value={baseSalary} onChange={(event) => setBaseSalary(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="offer-currency">Currency</Label>
              <Input
                id="offer-currency"
                maxLength={3}
                value={currency}
                onChange={(event) => setCurrency(event.target.value.toUpperCase())}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="offer-bonus">Bonus</Label>
              <Input id="offer-bonus" type="number" min="0" value={bonus} onChange={(event) => setBonus(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="offer-equity">Equity per year</Label>
              <Input id="offer-equity" type="number" min="0" value={equity} onChange={(event) => setEquity(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="offer-signing">Signing bonus</Label>
              <Input id="offer-signing" type="number" min="0" value={signing} onChange={(event) => setSigning(event.target.value)} />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="offer-deadline">Reply by</Label>
              <Input id="offer-deadline" type="date" value={deadline} onChange={(event) => setDeadline(event.target.value)} />
            </div>
          </div>
          {create.isError && <p role="alert" className="text-sm text-destructive">Could not save this offer. An application can have one offer.</p>}
          <DialogFooter>
            <Button type="submit" disabled={create.isPending || applicationId === '' || baseSalary === ''}>
              {create.isPending ? 'Saving…' : 'Save offer'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function OfferCard({ offer, selected, onToggle }: { offer: Offer; selected: boolean; onToggle: () => void }) {
  const queryClient = useQueryClient()
  const refresh = () => queryClient.invalidateQueries({ queryKey: offerKeys.all })
  const changeStatus = useMutation({
    mutationFn: (status: OfferStatus) => updateOffer(offer.id, { status }),
    onSuccess: refresh,
  })
  const remove = useMutation({ mutationFn: () => deleteOffer(offer.id), onSuccess: refresh })
  const urgent = offer.days_remaining !== null && offer.days_remaining <= 3
  const [draftOpen, setDraftOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)

  return (
    <Card className="py-0">
      <CardContent className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
        <input
          type="checkbox"
          className="size-4"
          aria-label={`Compare ${offer.company_name}`}
          checked={selected}
          onChange={onToggle}
        />
        <div className="min-w-0 flex-1 space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="truncate text-sm font-semibold">{offer.company_name}</h3>
            <span className="text-sm text-muted-foreground">{offer.job_title}</span>
            <Badge variant={urgent ? 'destructive' : 'secondary'}>{countdownLabel(offer)}</Badge>
          </div>
          <p className="text-xs text-muted-foreground">
            {money(offer.first_year_total, offer.currency)} in the first year
            {offer.location ? ` · ${offer.location}` : ''}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <NativeSelect
            aria-label={`Status for ${offer.company_name}`}
            className="w-36"
            value={offer.status}
            onChange={(event) => changeStatus.mutate(event.target.value as OfferStatus)}
          >
            {OFFER_STATUSES.map((value) => (
              <option key={value} value={value}>
                {formatLabel(value)}
              </option>
            ))}
          </NativeSelect>
          <Button variant="outline" size="sm" onClick={() => setDraftOpen(true)}>
            Draft negotiation email
          </Button>
          <Button
            variant="ghost"
            size="icon-sm"
            aria-label={`Delete offer from ${offer.company_name}`}
            onClick={() => setConfirmOpen(true)}
          >
            <Trash2Icon aria-hidden className="size-4" />
          </Button>
        </div>
      </CardContent>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Delete this offer?"
        description={<>This deletes the offer from <strong>{offer.company_name}</strong>. This cannot be undone.</>}
        pending={remove.isPending}
        onConfirm={() => remove.mutate()}
      />
      <Dialog open={draftOpen} onOpenChange={setDraftOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Negotiation email for {offer.company_name}</DialogTitle>
            <DialogDescription>A draft built from this offer. Edit it and copy it. Nothing is sent.</DialogDescription>
          </DialogHeader>
          {draftOpen && <EmailDraftPanel applicationId={offer.application_id} kinds={['negotiation']} />}
        </DialogContent>
      </Dialog>
    </Card>
  )
}

function CompareTable({ offers }: { offers: Offer[] }) {
  const rows: { label: string; value: (offer: Offer) => string }[] = [
    { label: 'Role', value: (offer) => offer.job_title },
    { label: 'Base salary', value: (offer) => money(offer.base_salary, offer.currency) },
    { label: 'Bonus', value: (offer) => money(offer.bonus, offer.currency) },
    { label: 'Equity per year', value: (offer) => money(offer.equity_value, offer.currency) },
    { label: 'Signing bonus', value: (offer) => money(offer.signing_bonus, offer.currency) },
    { label: 'First year total', value: (offer) => money(offer.first_year_total, offer.currency) },
    { label: 'Location', value: (offer) => offer.location ?? '—' },
    { label: 'Work mode', value: (offer) => (offer.work_mode ? formatLabel(offer.work_mode) : '—') },
    { label: 'Start date', value: (offer) => offer.start_date ?? '—' },
    { label: 'Reply', value: (offer) => countdownLabel(offer) },
  ]
  return (
    <div className="overflow-x-auto rounded-lg border">
      <table className="w-full text-sm">
        <caption className="sr-only">Offer comparison</caption>
        <thead>
          <tr className="border-b bg-muted/40 text-left">
            <th scope="col" className="p-3 font-medium">Offer</th>
            {offers.map((offer) => (
              <th key={offer.id} scope="col" className="p-3 font-medium">
                {offer.company_name}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.label} className="border-b last:border-0">
              <th scope="row" className="p-3 text-left font-normal text-muted-foreground">
                {row.label}
              </th>
              {offers.map((offer) => (
                <td key={offer.id} className="p-3">
                  {row.value(offer)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function OffersPage() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [selected, setSelected] = useState<string[]>([])
  const query = useQuery({ queryKey: offerKeys.all, queryFn: listOffers })
  const offers = query.data ?? []
  const compared = offers.filter((offer) => selected.includes(offer.id))

  function toggle(id: string) {
    setSelected((current) => (current.includes(id) ? current.filter((value) => value !== id) : [...current, id]))
  }

  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold">Offers</h2>
          <p className="text-sm text-muted-foreground">Compare offers side by side and see how long you have to reply.</p>
        </div>
        <Button onClick={() => setDialogOpen(true)}>
          <PlusIcon className="size-4" />
          Add offer
        </Button>
      </div>

      {query.isLoading ? (
        <LoadingRows rows={3} label="Loading offers" />
      ) : query.isError ? (
        <EmptyState
          compact
          icon={AlertTriangleIcon}
          title="Couldn't load offers"
          description="Something went wrong while fetching your offers."
          action={<Button variant="outline" onClick={() => query.refetch()}>Try again</Button>}
        />
      ) : offers.length === 0 ? (
        <EmptyState
          compact
          icon={HandCoinsIcon}
          title="No offers yet"
          description="Add an offer from one of your applications to compare it with others."
          action={<Button onClick={() => setDialogOpen(true)}>Add an offer</Button>}
        />
      ) : (
        <>
          <div className="space-y-2">
            {offers.map((offer) => (
              <OfferCard key={offer.id} offer={offer} selected={selected.includes(offer.id)} onToggle={() => toggle(offer.id)} />
            ))}
          </div>
          {compared.length >= 2 ? (
            <>
              <CompareTable offers={compared} />
              <OfferDecisionHelper offerIds={compared.map((offer) => offer.id)} />
            </>
          ) : (
            <p className="text-sm text-muted-foreground">Tick two or more offers to compare them.</p>
          )}
        </>
      )}

      <OfferDialog open={dialogOpen} onOpenChange={setDialogOpen} />
    </div>
  )
}
