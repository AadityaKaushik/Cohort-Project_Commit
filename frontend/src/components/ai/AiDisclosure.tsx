import { useQuery } from '@tanstack/react-query'
import { Cloud } from 'lucide-react'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { aiKeys, getAiStatus } from '@/lib/ai-api'

export function AiDisclosure() {
  const { data } = useQuery({ queryKey: aiKeys.status, queryFn: getAiStatus, staleTime: 5 * 60_000 })

  if (!data?.hosted) {
    return null
  }

  return (
    <Alert>
      <Cloud aria-hidden />
      <AlertTitle>Gemini is active</AlertTitle>
      <AlertDescription>
        Text from this page is sent to Google's Gemini service to produce the answer. Ask the operator to switch back to the local model if that is not acceptable.
      </AlertDescription>
    </Alert>
  )
}
