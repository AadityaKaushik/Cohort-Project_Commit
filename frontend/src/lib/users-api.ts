import { apiFetch, apiFetchBlob } from '@/lib/api-client'

export interface UserProfile {
  id: string
  email: string
  display_name: string
  target_role: string | null
  graduation_year: number | null
  target_date: string | null
  onboarded: boolean
  timezone: string | null
  fl_opt_in: boolean
  fl_privacy_posture: string
  created_at: string
}

export function getMyProfile() {
  return apiFetch<UserProfile>('/users/me')
}

export function updateMyProfile(input: Partial<Pick<UserProfile, 'email' | 'display_name' | 'target_role' | 'timezone'>> & {
    graduation_year?: number | null
    target_date?: string | null
  }) {
  return apiFetch<UserProfile>('/users/me', { method: 'PATCH', body: input })
}

export function changeMyPassword(input: { current_password: string; new_password: string }) {
  return apiFetch<void>('/users/me/password', { method: 'POST', body: input })
}

export function exportMyData() {
  return apiFetchBlob('/users/me/export')
}

export function deleteMyAccount(confirmation: string) {
  return apiFetch<void>('/users/me', { method: 'DELETE', body: { confirmation } })
}

export interface OnboardingResult {
  profile: UserProfile
  tasks_created: number
}

export function completeOnboarding(input: { target_role?: string; target_date?: string; skip?: boolean }) {
  return apiFetch<OnboardingResult>('/users/me/onboarding', { method: 'POST', body: input })
}
