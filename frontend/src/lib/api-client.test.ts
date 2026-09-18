import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError, SESSION_EXPIRED_EVENT } from '@/lib/api-client'
import { clearTokens, getAccessToken, setTokens } from '@/lib/auth-storage'

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiFetch (token refresh interceptor)', () => {
  beforeEach(() => {
    clearTokens()
  })

  afterEach(() => {
    clearTokens()
    vi.restoreAllMocks()
  })

  it('attaches the bearer token when one is stored', async () => {
    setTokens('access-1', 'refresh-1')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(jsonResponse({ ok: true }))

    await apiFetch('/applications')

    const [, init] = fetchMock.mock.calls[0]!
    const headers = init?.headers as Record<string, string> | undefined
    expect(headers?.Authorization).toBe('Bearer access-1')
  })

  it('refreshes the access token once and retries the original request on a 401', async () => {
    setTokens('expired-access', 'valid-refresh')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/refresh')) {
        return jsonResponse({ access_token: 'new-access', refresh_token: 'new-refresh' })
      }
      const authHeader = (init?.headers as Record<string, string> | undefined)?.Authorization
      if (authHeader === 'Bearer new-access') {
        return jsonResponse({ ok: true })
      }
      return jsonResponse({ error: { code: 'UNAUTHORIZED', message: 'expired' } }, 401)
    })

    const result = await apiFetch<{ ok: boolean }>('/applications')

    expect(result).toEqual({ ok: true })
    expect(getAccessToken()).toBe('new-access')
    const refreshCalls = fetchMock.mock.calls.filter(([url]) => String(url).endsWith('/auth/refresh'))
    expect(refreshCalls).toHaveLength(1)
  })

  it('dedupes concurrent refresh attempts into a single /auth/refresh call', async () => {
    setTokens('expired-access', 'valid-refresh')
    let refreshCallCount = 0
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
      const url = String(input)
      if (url.endsWith('/auth/refresh')) {
        refreshCallCount += 1
        return jsonResponse({ access_token: 'new-access', refresh_token: 'new-refresh' })
      }
      const authHeader = (init?.headers as Record<string, string> | undefined)?.Authorization
      if (authHeader === 'Bearer new-access') {
        return jsonResponse({ ok: true })
      }
      return jsonResponse({ error: { code: 'UNAUTHORIZED', message: 'expired' } }, 401)
    })

    await Promise.all([apiFetch('/applications'), apiFetch('/interviews')])

    expect(refreshCallCount).toBe(1)
  })

  it('clears tokens and emits a session-expired event when the refresh token itself is invalid', async () => {
    setTokens('expired-access', 'dead-refresh')
    vi.spyOn(globalThis, 'fetch').mockImplementation(async (input) => {
      const url = String(input)
      if (url.endsWith('/auth/refresh')) {
        return jsonResponse({ error: { code: 'INVALID_REFRESH_TOKEN', message: 'expired' } }, 401)
      }
      return jsonResponse({ error: { code: 'UNAUTHORIZED', message: 'expired' } }, 401)
    })

    const listener = vi.fn()
    window.addEventListener(SESSION_EXPIRED_EVENT, listener)

    await expect(apiFetch('/applications')).rejects.toBeInstanceOf(ApiError)

    expect(getAccessToken()).toBeNull()
    expect(listener).toHaveBeenCalledOnce()
    window.removeEventListener(SESSION_EXPIRED_EVENT, listener)
  })

  it('never attempts a refresh for calls marked skipAuth (login/register/refresh itself)', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValueOnce(
      jsonResponse({ error: { code: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' } }, 401),
    )

    await expect(
      apiFetch('/auth/login', { method: 'POST', body: { email: 'a@b.com', password: 'x' }, skipAuth: true }),
    ).rejects.toMatchObject({ message: 'Invalid email or password.' })

    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})
