import { describe, expect, it } from 'vitest'
import { parsePosting } from '@/lib/posting-parser'

const linkedin = `Senior Backend Engineer
Acme Payments · Bengaluru, Karnataka, India

Location: Bengaluru, Karnataka, India
Employment type: Full-time
Hybrid, 3 days in office

About Acme Payments
We build payment rails for small businesses.

Salary: ₹25,00,000 - ₹35,00,000 per annum`

const campus = `Campus Drive 2026
Role: Graduate Engineer Trainee
Company: Tata Consultancy Services
Location: Pune
Job type: Full-time
CTC: 3.5 - 4.5 LPA
On-site at the client office`

const internship = `Data Science Intern at Globex
Remote internship, 3 months. Stipend $500 - $800 per month.
Apply at https://globex.com/careers/ds-intern`

const startup = `We are hiring a Full Stack Developer to join Lumen Labs.
Fully remote, work from anywhere. Contract role.
$80k-120k USD`

const messy = `   

   >>> URGENT!!! <<<   
Position:    Junior QA Tester    
 company : Pixel Forge
 location - Jaipur   
salary 40k to 60k INR   
`

const noSalary = `Product Designer
Company: Northwind
Location: Remote
Part-time position, flexible hours.`

const nonEnglish = `Ingénieur logiciel – Paris
Nous recherchons un ingénieur pour rejoindre notre équipe.`

describe('parsePosting', () => {
  it('reads a LinkedIn-style posting', () => {
    const r = parsePosting(linkedin)
    expect(r).toMatchObject({
      jobTitle: 'Senior Backend Engineer',
      companyName: 'Acme Payments',
      location: 'Bengaluru, Karnataka, India',
      workMode: 'hybrid',
      jobType: 'full_time',
      salaryMin: '2500000',
      salaryMax: '3500000',
      salaryCurrency: 'INR',
    })
    expect(r.jobDescription).toContain('payment rails')
  })

  it('reads an Indian campus drive with LPA', () => {
    expect(parsePosting(campus)).toMatchObject({
      jobTitle: 'Graduate Engineer Trainee',
      companyName: 'Tata Consultancy Services',
      location: 'Pune',
      workMode: 'onsite',
      jobType: 'full_time',
      salaryMin: '350000',
      salaryMax: '450000',
      salaryCurrency: 'INR',
    })
  })

  it('reads an internship with the company from the title and the url', () => {
    const r = parsePosting(internship, 'https://globex.com/careers/ds-intern')
    expect(r).toMatchObject({
      jobTitle: 'Data Science Intern',
      companyName: 'Globex',
      workMode: 'remote',
      jobType: 'internship',
      jobUrl: 'https://globex.com/careers/ds-intern',
      salaryCurrency: 'USD',
    })
  })

  it('reads a remote startup posting with k amounts', () => {
    expect(parsePosting(startup)).toMatchObject({
      jobTitle: 'Full Stack Developer',
      workMode: 'remote',
      jobType: 'contract',
      salaryMin: '80000',
      salaryMax: '120000',
      salaryCurrency: 'USD',
    })
  })

  it('copes with a messy paste', () => {
    expect(parsePosting(messy)).toMatchObject({
      jobTitle: 'Junior QA Tester',
      companyName: 'Pixel Forge',
      location: 'Jaipur',
      salaryMin: '40000',
      salaryMax: '60000',
      salaryCurrency: 'INR',
    })
  })

  it('leaves salary empty when there is none', () => {
    const r = parsePosting(noSalary)
    expect(r.jobType).toBe('part_time')
    expect(r.salaryMin).toBeUndefined()
    expect(r.salaryCurrency).toBeUndefined()
  })

  it('does not read years of experience as a salary', () => {
    expect(parsePosting('Analyst\n2-5 years of experience required').salaryMin).toBeUndefined()
  })

  it('handles a non-English headline without inventing fields', () => {
    const r = parsePosting(nonEnglish)
    expect(r.jobTitle).toBe('Ingénieur logiciel – Paris')
    expect(r.companyName).toBeUndefined()
    expect(r.workMode).toBeUndefined()
    expect(r.jobType).toBeUndefined()
  })

  it('returns nothing for empty input', () => {
    expect(parsePosting('')).toEqual({})
    expect(parsePosting('   \n  ')).toEqual({})
  })

  it('uses the url domain when the text names no company', () => {
    expect(parsePosting('Backend Developer', 'https://www.initech.com/jobs/1').companyName).toBe('Initech')
  })

  it('drops an invalid url and never throws on odd input', () => {
    expect(parsePosting('Engineer', 'not a url').jobUrl).toBeUndefined()
    for (const input of ['\u0000\u0001', '😀😀😀', '₹ - ₹', '9'.repeat(5000), null, undefined, 42]) {
      expect(() => parsePosting(input as unknown as string, input as unknown as string)).not.toThrow()
    }
  })
})
