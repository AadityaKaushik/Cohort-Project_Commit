import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '@/lib/auth-storage'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api/v1'

/** Mirrors the error envelope every endpoint uses (PROJECT_PLAN.md §18):
 * {"error": {"code", "message", "field_errors"}}. */
export class ApiError extends Error {
  readonly status: number
  readonly code?: string
  readonly fieldErrors?: Record<string, string>

  constructor(status: number, message: string, code?: string, fieldErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.code = code
    this.fieldErrors = fieldErrors
  }
}

/**
 * Dispatched when the refresh token itself turns out to be invalid, expired,
 * or revoked — i.e. refreshing cannot save this session. apiFetch has no
 * React context of its own, so it hands off via a DOM event; AuthProvider is
 * the one listener, and it clears state and routes to /login with an
 * explanation (US-04 acceptance criterion).
 */
export const SESSION_EXPIRED_EVENT = 'cohort:session-expired'

/**
 * Single-flight refresh: every 401 that arrives while a refresh is already
 * in progress awaits the same promise instead of firing its own /auth/refresh
 * call (US-04: "concurrent requests during a refresh do not trigger multiple
 * refresh calls").
 */
let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    return null
  }

  if (!refreshPromise) {
    refreshPromise = fetch(`${API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refresh_token: refreshToken }),
    })
      .then(async (response) => {
        if (!response.ok) {
          return null
        }
        const data = (await response.json()) as { access_token: string; refresh_token: string }
        setTokens(data.access_token, data.refresh_token)
        return data.access_token
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

interface ApiFetchOptions extends Omit<RequestInit, 'body' | 'headers'> {
  body?: unknown
  headers?: Record<string, string>
  /** Set on calls the interceptor must never try to refresh/retry for —
   * register, login, and refresh itself. */
  skipAuth?: boolean
  /** Internal: prevents an infinite retry loop after one refresh attempt. */
  _retried?: boolean
}

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const { body, skipAuth, _retried, headers, ...rest } = options
  const accessToken = getAccessToken()

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...rest,
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && !skipAuth ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...headers,
    },
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (response.status === 401 && !skipAuth && !_retried) {
    const newAccessToken = await refreshAccessToken()
    if (newAccessToken) {
      return apiFetch<T>(path, { ...options, _retried: true })
    }
    clearTokens()
    window.dispatchEvent(new Event(SESSION_EXPIRED_EVENT))
    throw new ApiError(401, 'Your session has expired. Please log in again.', 'SESSION_EXPIRED')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const data = await response.json().catch(() => null)

  if (!response.ok) {
    const errorBody = (data as { error?: { code?: string; message?: string; field_errors?: Record<string, string> } })
      ?.error
    throw new ApiError(
      response.status,
      errorBody?.message ?? 'Something went wrong. Please try again.',
      errorBody?.code,
      errorBody?.field_errors,
    )
  }

  return data as T
}
