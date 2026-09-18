/**
 * Client-side token persistence (US-04: "a valid session survives a page
 * refresh"). The API issues tokens in the JSON response body rather than as
 * httpOnly cookies (PROJECT_PLAN.md §18.1), so localStorage is the only place
 * they can live; that's a known, documented trade-off against XSS exposure,
 * acceptable for a solo learner project with no third-party scripts.
 */

const STORAGE_KEY = 'cohort.auth.tokens'

interface StoredTokens {
  accessToken: string
  refreshToken: string
}

function read(): StoredTokens | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as StoredTokens) : null
  } catch {
    return null
  }
}

export function getAccessToken(): string | null {
  return read()?.accessToken ?? null
}

export function getRefreshToken(): string | null {
  return read()?.refreshToken ?? null
}

export function setTokens(accessToken: string, refreshToken: string): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ accessToken, refreshToken } satisfies StoredTokens))
  } catch {
    // Storage unavailable (private browsing, quota) — session simply won't
    // survive a refresh; not worth failing the login over.
  }
}

export function clearTokens(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // no-op
  }
}
