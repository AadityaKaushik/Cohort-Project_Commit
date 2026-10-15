import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { getSampleData, removeSampleData, sampleDataKey } from '@/lib/sample-data-api'

export function SampleDataBanner() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const statusQuery = useQuery({ queryKey: sampleDataKey, queryFn: getSampleData })

  const removal = useMutation({
    mutationFn: removeSampleData,
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      toast({ title: 'Sample data removed' })
    },
    onError: () => toast({ title: 'Could not remove the sample data', variant: 'error' }),
  })

  if (!statusQuery.data?.loaded) {
    return null
  }

  return (
    <div
      role="status"
      className="flex flex-col gap-2 rounded-xl border border-dashed bg-surface-2/50 p-3 sm:flex-row sm:items-center sm:justify-between"
    >
      <p className="text-sm text-muted-foreground">
        You are looking at sample data. It is marked [Sample] and only you can see it.
      </p>
      <Button type="button" variant="outline" size="sm" disabled={removal.isPending} onClick={() => removal.mutate()}>
        Remove sample data
      </Button>
    </div>
  )
}
