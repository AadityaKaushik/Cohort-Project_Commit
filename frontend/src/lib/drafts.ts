const PREFIX = 'cohort:draft:'

export function readDraft<T>(key: string): T | null {
  try {
    const raw = window.localStorage.getItem(PREFIX + key)
    return raw === null ? null : (JSON.parse(raw) as T)
  } catch {
    return null
  }
}

export function writeDraft(key: string, value: unknown) {
  try {
    window.localStorage.setItem(PREFIX + key, JSON.stringify(value))
  } catch {
    // storage can be full or blocked; losing a draft is not worth an error
  }
}

export function clearDraft(key: string) {
  try {
    window.localStorage.removeItem(PREFIX + key)
  } catch {
    // see writeDraft
  }
}
