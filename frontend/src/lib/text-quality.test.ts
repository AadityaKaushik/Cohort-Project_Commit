import { describe, expect, it } from 'vitest'
import { textHints, textProblem } from '@/lib/text-quality'
import { cleanName, meaningfulText, plausibleDate } from '@/lib/validation'

const LEGITIMATE = [
  'Rejected after round 2',
  'No feedback yet',
  'Offer accepted',
  'Two coding rounds and a system design round. They asked about caching and database indexes.',
  'The tea there was not good',
  'O(n log n) with a heap',
  'arr[i] += 1; // O(n)',
  'Entretien technique en deux parties, questions sur Java et les bases de donnees.',
  'इंटरव्यू में दो राउंड थे और सवाल कोडिंग पर थे',
  '面试一共三轮，问了很多算法题',
  'Собеседование прошло хорошо, спрашивали про базы данных',
  'مقابلة العمل كانت جيدة وسألوا عن البرمجة',
  'Hmmmm not sure how it went',
  'Wooooow what a long day -------- but good',
  'Typewriter pretty puppy poetry',
  'Round 1: DSA\nRound 2: HLD\nRound 3: HR',
  'Explained my approach clearly',
]

const JUNK: [string, string][] = [
  ['   ', 'EMPTY'],
  ['😂😂😂😂😂', 'NO_LETTERS'],
  ['12345 6789', 'NO_LETTERS'],
  ['!!!!!!!!!!!!!!!!! a', 'TOO_FEW_LETTERS'],
  ['aaaaaaaaaa', 'REPEATED_CHARACTER'],
  ['test test test test', 'REPEATED_WORD'],
  ['asdfasdfasdf', 'REPEATED_WORD'],
  ['asdfghjkl', 'KEYBOARD_MASH'],
  ['qwertyuiop', 'KEYBOARD_MASH'],
  ['asdf jkl; asdf', 'KEYBOARD_MASH'],
]

describe('text quality', () => {
  it.each(LEGITIMATE)('accepts %s', (text) => {
    expect(textProblem(text)).toBeNull()
  })

  it.each(JUNK)('rejects %j as %s', (text, code) => {
    expect(textProblem(text)?.code).toBe(code)
  })

  it('does not reject a real sentence with one odd token', () => {
    expect(textProblem('They asked about asdfgh in the second round of the interview')).toBeNull()
  })

  it('warns about off-topic experience text but not short or on-topic text', () => {
    expect(textHints('the tea there was not good', true).map((hint) => hint.code)).toEqual(['OFF_TOPIC'])
    expect(textHints('the tea there was not good', false)).toEqual([])
    expect(textHints('Rejected after round 2', true)).toEqual([])
    expect(textHints('Explained my approach clearly', true)).toEqual([])
  })

  it('never calls non-Latin text off-topic', () => {
    expect(textHints('मुझे वहां की चाय अच्छी नहीं लगी बिल्कुल भी', true)).toEqual([])
  })

  it('flags emails and phone numbers', () => {
    expect(textHints('Round 1 with priya@acme.com went fine', true).map((hint) => hint.code)).toContain('CONTAINS_CONTACT_DETAILS')
    expect(textHints('Recruiter number +91 98765 43210, round 2', true).map((hint) => hint.code)).toContain('CONTAINS_CONTACT_DETAILS')
    expect(textHints('Round 2 on 2024-05-12', true)).toEqual([])
  })
})

describe('field rules', () => {
  it('meaningfulText passes empty and short real notes', () => {
    expect(meaningfulText('')).toBeNull()
    expect(meaningfulText('Rejected after round 2')).toBeNull()
    expect(meaningfulText('qwertyuiop')).not.toBeNull()
  })

  it('cleanName accepts real names and rejects symbols only or emoji', () => {
    const rule = cleanName('Company')
    expect(rule('3M')).toBeNull()
    expect(rule("L'Oréal")).toBeNull()
    expect(rule('Zoho (India)')).toBeNull()
    expect(rule('!!!')).toMatch(/at least one letter/)
    expect(rule('Acme 🚀')).toMatch(/common punctuation/)
  })

  it('plausibleDate rejects year 1990 and dates years ahead', () => {
    const rule = plausibleDate('Date applied')
    expect(rule('')).toBeNull()
    expect(rule('2024-05-01')).toBeNull()
    expect(rule('1990-05-01')).not.toBeNull()
    expect(rule('2099-05-01')).not.toBeNull()
  })
})
