import { describe, expect, it } from 'vitest'
import { matchJob, SAMPLE_JOB_DESCRIPTION } from '@/lib/job-match'

describe('matchJob', () => {
  it('splits the skills a job mentions into matched and missing against the sample resume', () => {
    const result = matchJob(SAMPLE_JOB_DESCRIPTION)

    expect(result.matched).toEqual(expect.arrayContaining(['Java', 'Spring Boot', 'PostgreSQL', 'Docker', 'REST APIs', 'Testing']))
    expect(result.missing).toEqual(expect.arrayContaining(['Kubernetes', 'AWS', 'CI/CD', 'System design']))
    expect(result.score).toBe(Math.round((result.matched.length / result.required.length) * 100))
  })

  it('does not mistake JavaScript for Java and is deterministic', () => {
    const text = 'We use JavaScript and React.'

    expect(matchJob(text).required).toEqual(['JavaScript', 'React'])
    expect(matchJob(text)).toEqual(matchJob(text))
  })

  it('scores zero when no known skill is mentioned', () => {
    expect(matchJob('Friendly team, flexible hours.')).toEqual({ required: [], matched: [], missing: [], score: 0 })
  })
})
