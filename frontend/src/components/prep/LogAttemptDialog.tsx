import { useState, type FormEvent } from 'react'
import type { PracticeAttemptStatus, QuestionBankItem } from '@/lib/prep-api'
import { formatLabel } from '@/lib/labels'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'

const STATUSES: PracticeAttemptStatus[] = ['in_progress', 'confident', 'needs_review', 'not_started']
const CONFIDENCE_LEVELS = [
  { value: 1, label: '1 - Lost' },
  { value: 2, label: '2 - Shaky' },
  { value: 3, label: '3 - Okay' },
  { value: 4, label: '4 - Good' },
  { value: 5, label: '5 - Solid' },
]

export interface AttemptDraft {
  status: PracticeAttemptStatus
  confidence: number
  time_taken_minutes: number | null
  notes: string | null
}

interface LogAttemptDialogProps {
  question: QuestionBankItem | null
  isSaving: boolean
  onSubmit: (question: QuestionBankItem, draft: AttemptDraft) => void
  onClose: () => void
}

export function LogAttemptDialog({ question, isSaving, onSubmit, onClose }: LogAttemptDialogProps) {
  const [status, setStatus] = useState<PracticeAttemptStatus>('in_progress')
  const [confidence, setConfidence] = useState('3')
  const [minutes, setMinutes] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    if (!question) {
      return
    }
    if (minutes !== '' && !/^\d+$/.test(minutes)) {
      setError('Time taken must be a whole number of minutes.')
      return
    }
    setError(null)
    onSubmit(question, {
      status,
      confidence: Number(confidence),
      time_taken_minutes: minutes === '' ? null : Number(minutes),
      notes: notes.trim() === '' ? null : notes.trim(),
    })
  }

  return (
    <Dialog open={question !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent>
        <form onSubmit={handleSubmit} className="grid gap-4" noValidate>
          <DialogHeader>
            <DialogTitle>Log an attempt</DialogTitle>
            <DialogDescription>{question?.question_text}</DialogDescription>
          </DialogHeader>
          <div className="space-y-2">
            <Label htmlFor="attempt-status">How did it go?</Label>
            <NativeSelect id="attempt-status" value={status} onChange={(e) => setStatus(e.target.value as PracticeAttemptStatus)}>
              {STATUSES.map((option) => (
                <option key={option} value={option}>
                  {formatLabel(option)}
                </option>
              ))}
            </NativeSelect>
          </div>
          <div className="space-y-2">
            <Label htmlFor="attempt-confidence">Confidence</Label>
            <NativeSelect id="attempt-confidence" value={confidence} onChange={(e) => setConfidence(e.target.value)}>
              {CONFIDENCE_LEVELS.map((level) => (
                <option key={level.value} value={level.value}>
                  {level.label}
                </option>
              ))}
            </NativeSelect>
          </div>
          <div className="space-y-2">
            <Label htmlFor="attempt-minutes">Time taken (minutes)</Label>
            <Input id="attempt-minutes" inputMode="numeric" value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="attempt-notes">Notes</Label>
            <Textarea id="attempt-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
          </div>
          {error && (
            <p role="alert" className="text-sm text-destructive">
              {error}
            </p>
          )}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSaving}>
              {isSaving ? 'Saving…' : 'Log attempt'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
