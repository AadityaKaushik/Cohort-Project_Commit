import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { CopyIcon, MailIcon } from 'lucide-react'
import { createEmailDraft, EMAIL_DRAFT_LABELS, type EmailDraftKind } from '@/lib/ai-api'
import { formatLabel } from '@/lib/labels'
import { AiDisclosure } from '@/components/ai/AiDisclosure'
import { DraftEditor } from '@/components/ai/DraftEditor'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Skeleton } from '@/components/ui/skeleton'

interface EmailDraftPanelProps {
  applicationId: string
  kinds?: EmailDraftKind[]
  interviews?: { id: string; round_number: number; round_type: string }[]
}

const ALL_KINDS: EmailDraftKind[] = ['thank_you', 'follow_up', 'negotiation']

export function EmailDraftPanel({ applicationId, kinds = ALL_KINDS, interviews = [] }: EmailDraftPanelProps) {
  const [kind, setKind] = useState<EmailDraftKind>(kinds[0])
  const [interviewId, setInterviewId] = useState('')
  const draft = useMutation({
    mutationFn: () => createEmailDraft(applicationId, kind, kind === 'thank_you' && interviewId ? interviewId : undefined),
  })

  return (
    <div className="space-y-3">
      <AiDisclosure />
      {kinds.length > 1 && (
        <NativeSelect aria-label="Email type" value={kind} onChange={(event) => setKind(event.target.value as EmailDraftKind)}>
          {kinds.map((value) => (
            <option key={value} value={value}>
              {EMAIL_DRAFT_LABELS[value]}
            </option>
          ))}
        </NativeSelect>
      )}
      {kind === 'thank_you' && interviews.length > 0 && (
        <NativeSelect aria-label="Interview" value={interviewId} onChange={(event) => setInterviewId(event.target.value)}>
          <option value="">Latest interview</option>
          {interviews.map((interview) => (
            <option key={interview.id} value={interview.id}>
              Round {interview.round_number} · {formatLabel(interview.round_type)}
            </option>
          ))}
        </NativeSelect>
      )}
      <Button type="button" variant="outline" size="sm" disabled={draft.isPending} onClick={() => draft.mutate()}>
        <MailIcon aria-hidden />
        {draft.isPending ? 'Drafting…' : draft.data ? 'Draft again' : 'Create a draft'}
      </Button>
      {draft.isPending && <Skeleton role="status" aria-label="Drafting the email" className="h-40 w-full rounded-lg" />}
      {draft.isError && <p role="alert" className="text-sm text-destructive">Could not create a draft. Try again later.</p>}
      <div aria-live="polite">
        {draft.data && (
          <EmailDraftResult key={draft.submittedAt} subject={draft.data.subject} body={draft.data.body} source={draft.data.source} aiAvailable={draft.data.ai_available} />
        )}
      </div>
    </div>
  )
}

function EmailDraftResult({ subject, body, source, aiAvailable }: { subject: string; body: string; source: 'model' | 'template'; aiAvailable: boolean }) {
  const [subjectText, setSubjectText] = useState(subject)
  return (
    <div className="space-y-3">
      <Input aria-label="Email subject" value={subjectText} onChange={(event) => setSubjectText(event.target.value)} />
      <DraftEditor
        label="Email draft"
        draft={{ draft: body, source, ai_available: aiAvailable }}
        rows={10}
        actions={() => (
          <Button type="button" variant="outline" size="sm" onClick={() => navigator.clipboard.writeText(subjectText)}>
            <CopyIcon aria-hidden />
            Copy subject
          </Button>
        )}
      />
    </div>
  )
}
