import { textProblem } from '@/lib/text-quality'

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

export const meaningfulText: Rule = (value) => (value === '' ? null : (textProblem(value)?.message ?? null))

export const cleanName =
  (label: string): Rule =>
  (value) => {
    const text = value.replace(/\s+/g, ' ').trim()
    if (text === '') {
      return null
    }
    if (/\p{Cc}/u.test(text) || /[\p{So}\p{Sk}\p{Co}\p{Cs}]/u.test(text)) {
      return `${label} can only use letters, numbers and common punctuation.`
    }
    if (!/[\p{L}\p{N}]/u.test(text)) {
      return `${label} needs at least one letter or number.`
    }
    return null
  }

export const plausibleDate =
  (label: string): Rule =>
  (value) => {
    if (value.trim() === '') {
      return null
    }
    const year = Number(value.slice(0, 4))
    const limit = new Date()
    limit.setFullYear(limit.getFullYear() + 1)
    if (year < 2000 || new Date(value) > limit) {
      return `${label} must be from the year 2000 onwards and no more than a year ahead.`
    }
    return null
  }

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
