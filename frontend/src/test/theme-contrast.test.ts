import { describe, expect, it } from 'vitest'
import css from '../index.css?raw'

function tokenBlock(selector: string): Record<string, string> {
  const start = css.indexOf(`${selector} {`)
  const end = css.indexOf('}', start)
  const tokens: Record<string, string> = {}
  for (const match of css.slice(start, end).matchAll(/--([\w-]+):\s*(oklch\([^)]+\))/g)) {
    tokens[match[1]] = match[2]
  }
  return tokens
}

function relativeLuminance(oklch: string): number {
  const [l, c, h] = oklch
    .replace(/oklch\(|\)/g, '')
    .split(/\s+/)
    .map(Number)
  const hue = (h * Math.PI) / 180
  const a = c * Math.cos(hue)
  const b = c * Math.sin(hue)
  const l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
  const m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
  const s_ = (l - 0.0894841775 * a - 1.291485548 * b) ** 3
  const clamp = (value: number) => Math.min(1, Math.max(0, value))
  const r = clamp(4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_)
  const g = clamp(-1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_)
  const bl = clamp(-0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_)
  return 0.2126 * r + 0.7152 * g + 0.0722 * bl
}

function contrast(foreground: string, background: string): number {
  const [light, dark] = [relativeLuminance(foreground), relativeLuminance(background)].sort((x, y) => y - x)
  return (light + 0.05) / (dark + 0.05)
}

const TEXT_PAIRS: [string, string][] = [
  ['foreground', 'background'],
  ['card-foreground', 'card'],
  ['popover-foreground', 'popover'],
  ['primary-foreground', 'primary'],
  ['secondary-foreground', 'secondary'],
  ['muted-foreground', 'background'],
  ['muted-foreground', 'muted'],
  ['accent-foreground', 'accent'],
  ['destructive', 'background'],
  ['destructive', 'card'],
  ['success', 'background'],
  ['warning', 'background'],
  ['success-foreground', 'success'],
  ['warning-soft-foreground', 'warning-soft'],
  ['info', 'background'],
  ['primary', 'background'],
  ['sidebar-foreground', 'sidebar'],
  ['sidebar-accent-foreground', 'sidebar-accent'],
]

describe.each([
  ['light', ':root'],
  ['dark', '.dark'],
])('%s theme contrast', (_name, selector) => {
  const tokens = tokenBlock(selector)

  it.each(TEXT_PAIRS)('%s on %s meets 4.5:1', (foreground, background) => {
    expect(contrast(tokens[foreground], tokens[background])).toBeGreaterThanOrEqual(4.5)
  })

  it('focus ring is at least 3:1 against the page background', () => {
    expect(contrast(tokens.ring, tokens.background)).toBeGreaterThanOrEqual(3)
  })
})
