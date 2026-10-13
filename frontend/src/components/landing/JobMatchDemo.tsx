import { useId, useMemo, useState } from 'react'
import { CheckIcon, MinusIcon } from 'lucide-react'
import { ProgressRing } from '@/components/dashboard/ProgressRing'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { matchJob, SAMPLE_JOB_DESCRIPTION, SAMPLE_RESUME_SKILLS } from '@/lib/job-match'

/** Keyword overlap between a pasted job description and a fixed sample resume, computed in the browser. */
export function JobMatchDemo() {
  const inputId = useId()
  const [text, setText] = useState('')
  const result = useMemo(() => matchJob(text), [text])
  const hasInput = text.trim() !== ''

  return (
    <div className="surface-raised grid overflow-hidden rounded-2xl border md:grid-cols-[1.1fr_1fr]">
      <div className="space-y-3 border-b p-5 sm:p-6 md:border-r md:border-b-0">
        <div className="flex items-center justify-between gap-2">
          <Label htmlFor={inputId}>Job description</Label>
          <span className="rounded-md bg-warning-soft px-2 py-0.5 text-[11px] font-medium text-warning-soft-foreground">
            Sample demo, not your data
          </span>
        </div>
        <Textarea
          id={inputId}
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder="Paste a job description here"
          className="min-h-44 resize-none text-sm"
        />
        <div className="flex flex-wrap gap-2">
          <Button type="button" variant="outline" size="sm" onClick={() => setText(SAMPLE_JOB_DESCRIPTION)}>
            Use an example job
          </Button>
          {hasInput && (
            <Button type="button" variant="ghost" size="sm" onClick={() => setText('')}>
              Clear
            </Button>
          )}
        </div>
        <p className="text-xs text-muted-foreground">
          Compared with a built-in sample resume ({SAMPLE_RESUME_SKILLS.join(', ')}). Nothing is sent anywhere.
        </p>
      </div>

      <div aria-live="polite" className="flex flex-col gap-5 p-5 sm:p-6">
        <div className="flex items-center gap-4">
          <ProgressRing value={result.score} max={100} label="Skill overlap with the sample resume" size={88} stroke={8} />
          <div>
            <p className="font-semibold tracking-tight">
              {!hasInput
                ? 'Waiting for a job description'
                : result.required.length === 0
                  ? 'No known skills found yet'
                  : `${result.matched.length} of ${result.required.length} skills matched`}
            </p>
            <p className="text-xs text-muted-foreground">Simple keyword overlap, not a hiring prediction.</p>
          </div>
        </div>
        <SkillList title="Matched" skills={result.matched} tone="match" empty={hasInput ? 'None yet' : '—'} />
        <SkillList title="Missing" skills={result.missing} tone="miss" empty={hasInput ? 'Nothing missing' : '—'} />
      </div>
    </div>
  )
}

function SkillList({ title, skills, tone, empty }: { title: string; skills: string[]; tone: 'match' | 'miss'; empty: string }) {
  return (
    <div>
      <h4 className="mb-2 text-xs font-medium tracking-wide text-muted-foreground uppercase">{title}</h4>
      {skills.length === 0 ? (
        <p className="text-sm text-muted-foreground">{empty}</p>
      ) : (
        <ul aria-label={`${title} skills`} className="flex flex-wrap gap-1.5">
          {skills.map((skill) => (
            <li
              key={skill}
              className={
                tone === 'match'
                  ? 'animate-enter inline-flex items-center gap-1 rounded-full bg-success/12 px-2.5 py-1 text-xs font-medium text-success'
                  : 'animate-enter inline-flex items-center gap-1 rounded-full bg-warning-soft px-2.5 py-1 text-xs font-medium text-warning-soft-foreground'
              }
            >
              {tone === 'match' ? <CheckIcon aria-hidden className="size-3" /> : <MinusIcon aria-hidden className="size-3" />}
              {skill}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
