import { describe, expect, it } from 'vitest'
import { isNavItemActive } from '@/lib/navigation'

describe('isNavItemActive', () => {
  it('matches the item and the pages under it', () => {
    expect(isNavItemActive('/applications', '/applications')).toBe(true)
    expect(isNavItemActive('/applications/123/edit', '/applications')).toBe(true)
    expect(isNavItemActive('/applicationsx', '/applications')).toBe(false)
  })

  it('only marks the dashboard on the root path', () => {
    expect(isNavItemActive('/', '/')).toBe(true)
    expect(isNavItemActive('/applications', '/')).toBe(false)
  })

  it('lets a nested item win over its parent', () => {
    expect(isNavItemActive('/prep/mock-interview', '/prep/mock-interview')).toBe(true)
    expect(isNavItemActive('/prep/mock-interview', '/prep')).toBe(false)
    expect(isNavItemActive('/prep/questions', '/prep')).toBe(true)
  })
})
