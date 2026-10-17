import { apiFetch } from '@/lib/api-client'

export interface AuthUser {
  id: string
  email: string
  display_name: string
}

/** Tokens are null while the account still has to confirm its email with a code. */
export interface AuthResponse {
  user: AuthUser
  access_token: string | null
  refresh_token: string | null
  verification_required: boolean
}

export const EMAIL_NOT_VERIFIED = 'EMAIL_NOT_VERIFIED'

export function registerRequest(input: { email: string; password: string; display_name: string }) {
  return apiFetch<AuthResponse>('/auth/register', { method: 'POST', body: input, skipAuth: true })
}

export function loginRequest(input: { email: string; password: string }) {
  return apiFetch<AuthResponse>('/auth/login', { method: 'POST', body: input, skipAuth: true })
}

export function logoutRequest(refreshToken: string) {
  return apiFetch<void>('/auth/logout', {
    method: 'POST',
    body: { refresh_token: refreshToken },
    skipAuth: true,
  })
}

export function requestPasswordReset(email: string) {
  return apiFetch<void>('/auth/password-reset/request', { method: 'POST', body: { email }, skipAuth: true })
}

export function confirmPasswordReset(input: { email: string; code: string; new_password: string }) {
  return apiFetch<void>('/auth/password-reset/confirm', { method: 'POST', body: input, skipAuth: true })
}

export function requestEmailVerification(email: string) {
  return apiFetch<void>('/auth/email-verification/request', { method: 'POST', body: { email }, skipAuth: true })
}

export function confirmEmailVerification(input: { email: string; code: string }) {
  return apiFetch<AuthResponse>('/auth/email-verification/confirm', { method: 'POST', body: input, skipAuth: true })
}

export type OAuthProvider = 'google' | 'github' | 'linkedin'

export async function listOAuthProviders(): Promise<OAuthProvider[]> {
  const response = await apiFetch<{ providers: OAuthProvider[] }>('/auth/oauth/providers', { skipAuth: true })
  return response.providers
}
