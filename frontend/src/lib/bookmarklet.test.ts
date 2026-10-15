import { describe, expect, it } from 'vitest'
import { buildBookmarklet, buildSaveUrl, readBookmarkParams } from '@/lib/bookmarklet'

describe('bookmarklet', () => {
  it('builds a save url that reads back the same values', () => {
    const link = buildSaveUrl('https://cohort.example', {
      company: 'Acme, Inc',
      title: 'Backend Engineer',
      url: 'https://acme.com/jobs/1?ref=a&b=2',
      text: 'Line one\nLine two & more',
    })
    const search = link.split('?')[1]

    expect(link.startsWith('https://cohort.example/applications/new?src=bookmarklet')).toBe(true)
    expect(readBookmarkParams(search)).toEqual({
      company: 'Acme, Inc',
      title: 'Backend Engineer',
      location: '',
      url: 'https://acme.com/jobs/1?ref=a&b=2',
      text: 'Line one\nLine two & more',
    })
  })

  it('ignores links that did not come from the bookmark', () => {
    expect(readBookmarkParams('company=Acme')).toBeNull()
  })

  it('strips html, rejects bad urls and caps lengths', () => {
    const search = new URLSearchParams({
      src: 'bookmarklet',
      company: '<img src=x onerror=alert(1)>Acme',
      title: 'x'.repeat(500),
      url: 'javascript:alert(1)',
      text: 'a'.repeat(6000),
    }).toString()

    const params = readBookmarkParams(search)!
    expect(params.company).toBe('Acme')
    expect(params.title).toHaveLength(300)
    expect(params.url).toBe('')
    expect(params.text).toHaveLength(5000)
  })

  it('produces a javascript url that carries the origin', () => {
    const link = buildBookmarklet('https://cohort.example')

    expect(link.startsWith('javascript:')).toBe(true)
    expect(decodeURI(link)).toContain('"https://cohort.example"')
    expect(decodeURI(link)).toContain('/applications/new?')
  })
})
