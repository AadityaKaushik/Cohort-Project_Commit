import { Link, useParams } from 'react-router-dom'
import { useMutation, useQuery } from '@tanstack/react-query'
import { format, parseISO } from 'date-fns'
import { PrinterIcon, SearchXIcon, FileTextIcon } from 'lucide-react'
import { aiKeys, createPrepSummary, getPrepPack } from '@/lib/ai-api'
import { formatLabel } from '@/lib/labels'
import { AiDisclosure } from '@/components/ai/AiDisclosure'
import { DraftEditor } from '@/components/ai/DraftEditor'
import { EmptyState } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { Button } from '@/components/ui/button'

export function InterviewPrepPage() {
  const { id } = useParams()
  const { data, isLoading, isError } = useQuery({
    queryKey: aiKeys.prepPack(id ?? ''),
    queryFn: () => getPrepPack(id!),
    enabled: Boolean(id),
  })
  const summary = useMutation({ mutationFn: () => createPrepSummary(id!) })

  if (isLoading) {
    return <PageSkeleton label="Loading prep pack" />
  }

  if (isError || !data) {
    return (
      <EmptyState
        icon={SearchXIcon}
        title="Interview not found"
        description="It may have been deleted, or the link may be wrong."
        action={
          <Button asChild>
            <Link to="/interviews">Back to interviews</Link>
          </Button>
        }
      />
    )
  }

  const when = data.scheduled_at ? format(parseISO(data.scheduled_at), 'EEE d MMM yyyy, HH:mm') : 'Not scheduled'
  const interviewer = [data.interviewer_name, data.interviewer_role].filter(Boolean).join(', ')

  return (
    <div className="space-y-6">
      <PageHeader
        back={{ to: `/interviews/${data.interview_id}`, label: 'Interview' }}
        title={`Prep pack: ${data.company_name}`}
        description={`Round ${data.round_number} · ${formatLabel(data.round_type)} · ${data.job_title}`}
        actions={
          <Button variant="outline" className="print:hidden" onClick={() => window.print()}>
            <PrinterIcon aria-hidden />
            Print
          </Button>
        }
      />
      <AiDisclosure />

      <SectionCard title="The round">
        <p className="text-sm">{when}</p>
        {interviewer && <p className="text-sm text-muted-foreground">Interviewer: {interviewer}</p>}
        <p className="mt-2 text-sm text-muted-foreground">
          Readiness for this role: {data.readiness_score !== null ? data.readiness_formatted : 'not enough practice yet'}
        </p>
      </SectionCard>

      <SectionCard title="Summary" description="A short paragraph written from the facts on this page. You can edit it.">
        <div className="space-y-3">
          <Button type="button" variant="outline" size="sm" className="print:hidden" disabled={summary.isPending} onClick={() => summary.mutate()}>
            <FileTextIcon aria-hidden />
            {summary.isPending ? 'Writing…' : summary.data ? 'Write again' : 'Write a summary'}
          </Button>
          {summary.isError && <p role="alert" className="text-sm text-destructive">Could not create a summary. Try again later.</p>}
          {summary.data && <DraftEditor key={summary.submittedAt} label="Prep summary" draft={summary.data} rows={5} />}
        </div>
      </SectionCard>

      {data.weak_topics.length > 0 && (
        <SectionCard title="Topics to revise" description="From the job posting, weakest first.">
          <ul className="space-y-1.5 text-sm">
            {data.weak_topics.map((topic) => (
              <li key={topic.name} className="flex items-center justify-between gap-3">
                <span>{topic.name}</span>
                <span className="tabular-nums text-muted-foreground">{topic.has_evidence ? `${topic.mastery_percent}%` : 'no practice'}</span>
              </li>
            ))}
          </ul>
        </SectionCard>
      )}

      {data.resume_highlights.length > 0 && (
        <SectionCard title="Resume highlights to mention">
          <ul className="list-disc space-y-1 pl-5 text-sm">
            {data.resume_highlights.map((line) => (
              <li key={line}>{line}</li>
            ))}
          </ul>
        </SectionCard>
      )}

      <SectionCard title="Earlier rounds with this company">
        {data.past_rounds.length === 0 ? (
          <p className="text-sm text-muted-foreground">No earlier rounds recorded.</p>
        ) : (
          <ul className="space-y-3 text-sm">
            {data.past_rounds.map((round) => (
              <li key={`${round.round_number}-${round.job_title}`} className="space-y-1">
                <p className="font-medium">
                  Round {round.round_number} · {formatLabel(round.round_type)} · {formatLabel(round.outcome)}
                </p>
                {round.went_well && <p>Went well: {round.went_well}</p>}
                {round.went_poorly && <p>Went poorly: {round.went_poorly}</p>}
                {round.improvement_areas && <p>To improve: {round.improvement_areas}</p>}
              </li>
            ))}
          </ul>
        )}
      </SectionCard>

      <SectionCard title="Questions asked so far">
        {data.questions_asked.length === 0 ? (
          <p className="text-sm text-muted-foreground">No questions logged yet.</p>
        ) : (
          <ul className="list-disc space-y-1 pl-5 text-sm">
            {data.questions_asked.map((question, index) => (
              <li key={`${question.round_number}-${index}`}>
                {question.text} <span className="text-muted-foreground">(round {question.round_number})</span>
              </li>
            ))}
          </ul>
        )}
      </SectionCard>

      <SectionCard title="Company notes">
        {data.company_notes.length === 0 ? (
          <p className="text-sm text-muted-foreground">No notes saved for this company.</p>
        ) : (
          <ul className="space-y-2 text-sm">
            {data.company_notes.map((note) => (
              <li key={note} className="whitespace-pre-wrap">
                {note}
              </li>
            ))}
          </ul>
        )}
      </SectionCard>
    </div>
  )
}
