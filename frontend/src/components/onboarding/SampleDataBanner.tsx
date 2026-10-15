import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { getSampleData, removeSampleData, sampleDataKey } from '@/lib/sample-data-api'

export function SampleDataBanner() {
  const queryClient = useQueryClient()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const { toast } = useToast()
  const statusQuery = useQuery({ queryKey: sampleDataKey, queryFn: getSampleData })

  const removal = useMutation({
    mutationFn: removeSampleData,
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      setConfirmOpen(false)
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
      <Button type="button" variant="outline" size="sm" onClick={() => setConfirmOpen(true)}>
        Remove sample data
      </Button>
      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        title="Remove the sample data?"
        description="Everything marked [Sample] is deleted. Your own entries are not touched."
        confirmLabel="Remove sample data"
        pending={removal.isPending}
        onConfirm={() => removal.mutate()}
      />
    </div>
  )
}
