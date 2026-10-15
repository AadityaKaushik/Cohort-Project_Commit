import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { SaveIcon, SparklesIcon } from 'lucide-react'
import { ApiError } from '@/lib/api-client'
import { updateApplication } from '@/lib/applications-api'
import { aiKeys, createCoverLetterDraft, getResumeMatch } from '@/lib/ai-api'
import { useToast } from '@/hooks/use-toast'
import { AiDisclosure } from '@/components/ai/AiDisclosure'
import { DraftEditor } from '@/components/ai/DraftEditor'
import { SectionCard } from '@/components/shared/SectionCard'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Skeleton } from '@/components/ui/skeleton'

export function ResumeMatchCard({ applicationId }: { applicationId: string }) {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const { data, isLoading, isError } = useQuery({
    queryKey: aiKeys.match(applicationId),
    queryFn: () => getResumeMatch(applicationId),
  })

  const letter = useMutation({ mutationFn: () => createCoverLetterDraft(applicationId) })

  const save = useMutation({
    mutationFn: (coverLetter: string) => updateApplication(applicationId, { cover_letter: coverLetter }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['application', applicationId] })
      toast({ title: 'Saved as the cover letter' })
    },
    onError: () => toast({ title: 'Could not save the cover letter', variant: 'error' }),
  })

  if (isError) {
    return null
  }

  const failure =
    letter.error instanceof ApiError && letter.error.status === 429
      ? 'You have hit the AI request limit. Try again in a few minutes.'
      : letter.error
        ? 'Could not create a draft. Try again later.'
        : null

  return (
    <SectionCard title="Resume match" description="Skills in the job description compared with the attached resume.">
      <div className="space-y-4">
        <AiDisclosure />
        {isLoading && <Skeleton className="h-20 w-full rounded-lg" data-testid="match-loading" />}
        {data && (
          <>
            {data.score !== null && <p className="text-3xl font-semibold tracking-tight">{data.score}%</p>}
            <p className="text-sm text-muted-foreground">{data.explanation}</p>
            {data.matched.length > 0 && (
              <div className="space-y-1.5">
                <p className="text-sm font-medium">Matched</p>
                <div className="flex flex-wrap gap-1.5">
                  {data.matched.map((skill) => (
                    <Badge key={skill} variant="secondary">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
            {data.missing.length > 0 && (
              <div className="space-y-1.5">
                <p className="text-sm font-medium">Missing</p>
                <div className="flex flex-wrap gap-1.5">
                  {data.missing.map((skill) => (
                    <Badge key={skill} variant="outline">
                      {skill}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
            {data.bullets.length > 0 && (
              <div className="space-y-1.5">
                <p className="text-sm font-medium">Lines from your resume worth leading with</p>
                <ul className="list-disc space-y-1 pl-5 text-sm">
                  {data.bullets.map((bullet) => (
                    <li key={bullet.text}>
                      {bullet.text}
                      <span className="text-muted-foreground"> ({bullet.source})</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </>
        )}

        <div className="space-y-3 border-t pt-4">
          <Button type="button" variant="outline" size="sm" disabled={letter.isPending} onClick={() => letter.mutate()}>
            <SparklesIcon aria-hidden />
            {letter.isPending ? 'Drafting…' : letter.data ? 'Draft again' : 'Draft a cover letter'}
          </Button>
          {failure && <p role="alert" className="text-sm text-destructive">{failure}</p>}
          {letter.data && (
            <DraftEditor
              key={letter.submittedAt}
              label="Cover letter draft"
              draft={letter.data}
              actions={(text) => (
                <Button type="button" size="sm" disabled={save.isPending || text.trim() === ''} onClick={() => save.mutate(text)}>
                  <SaveIcon aria-hidden />
                  Save to application
                </Button>
              )}
            />
          )}
        </div>
      </div>
    </SectionCard>
  )
}
