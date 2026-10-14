import { apiFetch } from '@/lib/api-client'

export interface AiStatus {
  provider: string
  hosted: boolean
}

export const aiKeys = {
  status: ['ai', 'status'] as const,
}

export function getAiStatus() {
  return apiFetch<AiStatus>('/ai/status')
}
