export type FieldErrors<T extends string = string> = Partial<Record<T, string>>

type Rule = (value: string) => string | null

export const required =
  (label: string): Rule =>
  (value) =>
    value.trim() === '' ? `${label} is required.` : null

export const email: Rule = (value) =>
  value.trim() === '' || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()) ? null : 'Enter a valid email address, like name@example.com.'

export const minLength =
  (label: string, length: number): Rule =>
  (value) =>
    value.length < length ? `${label} needs at least ${length} characters.` : null

export const url: Rule = (value) => {
  if (value.trim() === '') {
    return null
  }
  try {
    const parsed = new URL(value.trim())
    return parsed.protocol === 'http:' || parsed.protocol === 'https:' ? null : 'Use a link that starts with http:// or https://.'
  } catch {
    return 'Enter a full link, like https://example.com/jobs/123.'
  }
}

export const positiveInteger =
  (label: string): Rule =>
  (value) =>
    value.trim() === '' || (/^\d+$/.test(value.trim()) && Number(value) > 0) ? null : `${label} must be a whole number above zero.`

/** Runs each field's rules in order and keeps the first message per field. */
export function validate<T extends string>(
  values: Record<T, string>,
  rules: Partial<Record<T, Rule[]>>,
): FieldErrors<T> {
  const errors: FieldErrors<T> = {}
  for (const field of Object.keys(rules) as T[]) {
    for (const rule of rules[field] ?? []) {
      const message = rule(values[field] ?? '')
      if (message) {
        errors[field] = message
        break
      }
    }
  }
  return errors
}

export function hasErrors(errors: FieldErrors): boolean {
  return Object.keys(errors).length > 0
}
