
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
  }
}

export function clearTokens(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
  }
}
