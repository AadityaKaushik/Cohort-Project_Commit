import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { loadSampleData } from '@/lib/sample-data-api'

interface LoadSampleDataButtonProps {
  variant?: 'outline' | 'ghost'
  className?: string
  onLoaded?: () => void
}

export function LoadSampleDataButton({ variant = 'outline', className, onLoaded }: LoadSampleDataButtonProps) {
  const queryClient = useQueryClient()
  const { toast } = useToast()

  const mutation = useMutation({
    mutationFn: loadSampleData,
    onSuccess: async () => {
      await queryClient.invalidateQueries()
      toast({ title: 'Sample data loaded', description: 'Everything is marked [Sample] and can be removed from the dashboard.' })
      onLoaded?.()
    },
    onError: () => toast({ title: 'Could not load the sample data', variant: 'error' }),
  })

  return (
    <Button type="button" variant={variant} className={className} disabled={mutation.isPending} onClick={() => mutation.mutate()}>
      {mutation.isPending ? 'Loading sample data…' : 'Load sample data'}
    </Button>
  )
}
