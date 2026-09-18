import { apiFetch } from '@/lib/api-client'

export interface AuthUser {
  id: string
  email: string
  display_name: string
}

interface AuthResponse {
  user: AuthUser
  access_token: string
  refresh_token: string
}

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
