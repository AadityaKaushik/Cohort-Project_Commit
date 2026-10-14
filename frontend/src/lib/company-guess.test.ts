import { describe, expect, it } from 'vitest'
import { guessCompanyFromUrl } from '@/lib/company-guess'

describe('guessCompanyFromUrl', () => {
  it('uses the domain name', () => {
    expect(guessCompanyFromUrl('https://www.acme.com/careers/123')).toBe('Acme')
  })

  it('drops careers and jobs subdomains', () => {
    expect(guessCompanyFromUrl('https://careers.stripe.com/listing/1')).toBe('Stripe')
  })

  it('handles two-part country domains', () => {
    expect(guessCompanyFromUrl('https://jobs.tesco.co.uk/role')).toBe('Tesco')
  })

  it('reads the company from a job board path', () => {
    expect(guessCompanyFromUrl('https://boards.greenhouse.io/open-ai/jobs/42')).toBe('Open Ai')
  })

  it('gives no guess for generic job sites or bad input', () => {
    expect(guessCompanyFromUrl('https://www.linkedin.com/jobs/view/1')).toBe('')
    expect(guessCompanyFromUrl('not a url')).toBe('')
  })
})
