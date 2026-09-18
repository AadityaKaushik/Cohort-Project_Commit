import { apiFetch } from '@/lib/api-client'

export interface UserProfile {
  id: string
  email: string
  display_name: string
  target_role: string | null
  graduation_year: number | null
  timezone: string | null
  fl_opt_in: boolean
  fl_privacy_posture: string
  created_at: string
}

export function getMyProfile() {
  return apiFetch<UserProfile>('/users/me')
}

export function updateMyProfile(input: Partial<Pick<UserProfile, 'email' | 'display_name' | 'target_role' | 'timezone'>> & { graduation_year?: number | null }) {
  return apiFetch<UserProfile>('/users/me', { method: 'PATCH', body: input })
}
