import { useMutation, useQueryClient } from '@tanstack/react-query'
import { changeApplicationStatus, type ApplicationStatusValue, type ApplicationSummary } from '@/lib/applications-api'
import { ApiError } from '@/lib/api-client'
import { formatLabel } from '@/lib/labels'
import { useToast } from '@/hooks/use-toast'

export function QuickStatusMenu({ application }: { application: ApplicationSummary }) {
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const mutation = useMutation({
    mutationFn: (status: ApplicationStatusValue) => changeApplicationStatus(application.id, { to_status: status }),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
      queryClient.invalidateQueries({ queryKey: ['application', application.id] })
      toast({ title: `Moved to ${formatLabel(result.status)}` })
    },
    onError: (error: unknown) => {
      toast({
        title: 'Move refused',
        description: error instanceof ApiError ? error.message : 'Something went wrong. The status was not changed.',
        variant: 'error',
      })
    },
  })

  if (application.allowed_next_statuses.length === 0) {
    return <span className="text-muted-foreground">—</span>
  }

  return (
    <span onClick={(event) => event.stopPropagation()} onKeyDown={(event) => event.stopPropagation()}>
      <select
        aria-label={`Change status of ${application.company.name}, ${application.job_title}`}
        value=""
        disabled={mutation.isPending}
        onChange={(event) => event.target.value && mutation.mutate(event.target.value as ApplicationStatusValue)}
        className="h-7 max-w-[8rem] rounded-md border bg-card px-1.5 text-xs text-foreground transition-colors hover:border-primary/40 focus-visible:outline-2 focus-visible:outline-ring"
      >
        <option value="">Move to</option>
        {application.allowed_next_statuses.map((status) => (
          <option key={status} value={status}>
            {formatLabel(status)}
          </option>
        ))}
      </select>
    </span>
  )
}
