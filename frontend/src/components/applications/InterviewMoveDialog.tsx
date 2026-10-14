import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { addDays, format } from 'date-fns'
import { changeApplicationStatus } from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { createInterview, listInterviews, type InterviewRoundType } from '@/lib/interviews-api'
import { formatLabel } from '@/lib/labels'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { FormError, FormField } from '@/components/shared/Form'

const ROUND_TYPES: InterviewRoundType[] = ['OA', 'PHONE_SCREEN', 'TECHNICAL', 'SYSTEM_DESIGN', 'BEHAVIOURAL', 'HR', 'FINAL']

interface InterviewMoveDialogProps {
  applicationId: string
  companyName: string
  note?: string
  onClose: () => void
  onMoved?: () => void
}

export function InterviewMoveDialog({ applicationId, companyName, note, onClose, onMoved }: InterviewMoveDialogProps) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [roundNumber, setRoundNumber] = useState('1')
  const [roundType, setRoundType] = useState<InterviewRoundType>('PHONE_SCREEN')
  const [date, setDate] = useState(format(addDays(new Date(), 7), 'yyyy-MM-dd'))
  const [error, setError] = useState<string | null>(null)

  const existing = useQuery({
    queryKey: ['interviews', 'for-application', applicationId],
    queryFn: () => listInterviews({ application_id: applicationId, size: 100 }),
  })

  useEffect(() => {
    if (!existing.data) return
    const next = Math.max(0, ...existing.data.items.map((interview) => interview.round_number)) + 1
    setRoundNumber(String(next))
    setRoundType(next === 1 ? 'PHONE_SCREEN' : 'TECHNICAL')
  }, [existing.data])

  const mutation = useMutation({
    mutationFn: async (withInterview: boolean) => {
      await changeApplicationStatus(applicationId, { to_status: 'INTERVIEW', note })
      if (withInterview) {
        await createInterview({
          application_id: applicationId,
          round_number: Number(roundNumber),
          round_type: roundType,
          scheduled_at: date ? new Date(`${date}T10:00`).toISOString() : null,
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['application', applicationId] })
      queryClient.invalidateQueries({ queryKey: ['interviews'] })
      toast({ title: 'Moved to Interview' })
      onMoved?.()
      onClose()
    },
    onError: (failure: unknown) => {
      setError(failure instanceof ApiError ? failure.message : 'Could not move this application.')
    },
  })

  const roundValid = Number.isInteger(Number(roundNumber)) && Number(roundNumber) >= 1

  return (
    <Dialog open onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add interview</DialogTitle>
          <DialogDescription>Moving {companyName} to Interview. Add the round now, or move it without one.</DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 sm:grid-cols-3">
          <FormField id="interviewRound" label="Round">
            <Input id="interviewRound" type="number" min={1} value={roundNumber} onChange={(event) => setRoundNumber(event.target.value)} />
          </FormField>
          <FormField id="interviewType" label="Type">
            <NativeSelect id="interviewType" value={roundType} onChange={(event) => setRoundType(event.target.value as InterviewRoundType)}>
              {ROUND_TYPES.map((type) => (
                <option key={type} value={type}>
                  {formatLabel(type)}
                </option>
              ))}
            </NativeSelect>
          </FormField>
          <FormField id="interviewDate" label="Date">
            <Input id="interviewDate" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
          </FormField>
        </div>
        {error && <FormError>{error}</FormError>}
        <DialogFooter>
          <Button variant="outline" disabled={mutation.isPending} onClick={() => mutation.mutate(false)}>
            Move without interview
          </Button>
          <Button disabled={mutation.isPending || !roundValid} onClick={() => mutation.mutate(true)}>
            {mutation.isPending ? 'Saving…' : 'Add interview'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
