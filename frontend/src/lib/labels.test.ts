import { describe, expect, it } from 'vitest'
import { formatLabel } from '@/lib/labels'

describe('formatLabel', () => {
  it.each([
    ['FINAL_ROUND', 'Final Round'],
    ['OA', 'OA'],
    ['PHONE_SCREEN', 'Phone Screen'],
    ['system_design', 'System Design'],
    ['dsa', 'DSA'],
    ['hr', 'HR'],
    ['needs_review', 'Needs review'],
    ['failed', 'Not selected'],
    ['full_time', 'Full Time'],
    ['os_networking', 'OS and networking'],
  ])('formats %s as %s', (raw, expected) => {
    expect(formatLabel(raw)).toBe(expected)
  })

  it('shows a dash for missing values', () => {
    expect(formatLabel(null)).toBe('—')
    expect(formatLabel(undefined)).toBe('—')
  })
})
