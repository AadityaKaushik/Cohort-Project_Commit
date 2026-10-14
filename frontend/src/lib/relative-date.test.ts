import { describe, expect, it } from 'vitest'
import { exactDate, relativeDate } from '@/lib/relative-date'

const now = new Date(2026, 4, 20)

describe('relativeDate', () => {
  it.each([
    ['2026-05-20', 'today'],
    ['2026-05-19', 'yesterday'],
    ['2026-05-17', '3 days ago'],
    ['2026-05-01', '2 weeks ago'],
    ['2026-02-10', '3 months ago'],
    ['2025-01-01', '1 year ago'],
    ['2026-05-23', 'in 3 days'],
  ])('%s is %s', (iso, expected) => {
    expect(relativeDate(iso, now)).toBe(expected)
  })

  it('formats the exact date', () => {
    expect(exactDate('2026-01-10')).toBe('Jan 10, 2026')
  })
})
