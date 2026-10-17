import { describe, expect, it } from 'vitest'
import css from '../index.css?raw'

function block(selector: string): Record<string, string> {
  const start = css.indexOf(`\n${selector} {`)
  const body = css.slice(start, css.indexOf('\n}', start))
  const tokens: Record<string, string> = {}
  for (const m of body.matchAll(/--([\w-]+):\s*([^;]+);/g)) tokens[m[1]] = m[2].trim()
  return tokens
}

// linear-light sRGB, which is what relative luminance needs
function toRgb(value: string): Rgb {
  const m = value.match(/^oklch\(\s*([\d.]+)\s+([\d.]+)\s+([\d.]+)\s*\)$/)
  if (!m) throw new Error(`unsupported colour: ${value}`)
  const [l, c, h] = [Number(m[1]), Number(m[2]), (Number(m[3]) * Math.PI) / 180]
  const a = c * Math.cos(h)
  const b = c * Math.sin(h)
  const l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
  const m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
  const s_ = (l - 0.0894841775 * a - 1.291485548 * b) ** 3
  const rgb = [
    4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_,
    -1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_,
    -0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_,
  ]
  return rgb.map((v) => Math.min(1, Math.max(0, v))) as Rgb
}

type Rgb = [number, number, number]

function luminance([r, g, b]: Rgb): number {
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contrast(a: Rgb, b: Rgb): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

function ratio(a: string, b: string): number {
  return contrast(toRgb(a), toRgb(b))
}

const encode = (c: number) => (c <= 0.0031308 ? 12.92 * c : 1.055 * c ** (1 / 2.4) - 0.055)
const decode = (c: number) => (c <= 0.04045 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)

// browsers blend alpha in encoded space, so a tint is mixed there too
function tint(fg: string, bg: string, alpha: number): Rgb {
  const [f, b] = [toRgb(fg), toRgb(bg)]
  return f.map((v, i) => decode(encode(v) * alpha + encode(b[i]) * (1 - alpha))) as Rgb
}

const textPairs: [string, string][] = [
  ['foreground', 'background'],
  ['card-foreground', 'card'],
  ['popover-foreground', 'popover'],
  ['secondary-foreground', 'secondary'],
  ['accent-foreground', 'accent'],
  ['primary-foreground', 'primary'],
  ['sidebar-foreground', 'sidebar'],
  ['sidebar-primary-foreground', 'sidebar-primary'],
  ['sidebar-accent-foreground', 'sidebar-accent'],
  ['foreground', 'muted'],
  ['muted-foreground', 'background'],
  ['muted-foreground', 'card'],
  ['muted-foreground', 'popover'],
  ['muted-foreground', 'muted'],
  ['muted-foreground', 'secondary'],
  ['muted-foreground', 'accent'],
  ['sidebar-muted-foreground', 'sidebar'],
  ['signal-foreground', 'signal'],
  ['brand-foreground', 'brand'],
  ['muted-foreground', 'surface-2'],
  ['primary', 'background'],
  ['primary', 'card'],
  ['destructive', 'background'],
  ['destructive', 'card'],
  ['success', 'background'],
  ['success', 'card'],
  ['success-foreground', 'success'],
  ['warning', 'background'],
  ['warning', 'card'],
  ['warning-soft-foreground', 'warning-soft'],
  ['info', 'background'],
  ['info', 'card'],
]

const uiPairs: [string, string][] = [
  ['input', 'background'],
  ['input', 'card'],
  ['ring', 'background'],
  ['ring', 'card'],
  ['sidebar-ring', 'sidebar'],
  ['chart-1', 'card'],
  ['chart-2', 'card'],
  ['chart-3', 'card'],
  ['chart-4', 'card'],
  ['chart-5', 'card'],
  ['series-1', 'card'],
  ['series-2', 'card'],
  ['rule', 'background'],
  ['signal', 'brand'],
  ['sidebar-primary', 'sidebar'],
  ['line-saved', 'card'],
  ['line-saved', 'background'],
  ['line-applied', 'card'],
  ['line-applied', 'background'],
  ['line-oa', 'card'],
  ['line-oa', 'background'],
  ['line-interview', 'card'],
  ['line-interview', 'background'],
  ['line-final', 'card'],
  ['line-final', 'background'],
  ['line-offer', 'card'],
  ['line-offer', 'background'],
  ['line-closed', 'card'],
  ['line-closed', 'background'],
]

const toneTokens = ['primary', 'info', 'success', 'destructive']

// strongest tint used by the tone badges and destructive badge in each theme
describe.each([
  ['light', ':root', 0.12],
  ['dark', '.dark', 0.2],
] as const)('%s theme contrast', (_name, selector, tintAlpha) => {
  const tokens = block(selector)

  it.each(textPairs)('%s on %s is at least 4.5:1', (fg, bg) => {
    expect(ratio(tokens[fg], tokens[bg])).toBeGreaterThanOrEqual(4.5)
  })

  it.each(uiPairs)('%s on %s is at least 3:1', (fg, bg) => {
    expect(ratio(tokens[fg], tokens[bg])).toBeGreaterThanOrEqual(3)
  })

  it.each(toneTokens)('%s badge text on a tinted card is at least 4.5:1', (tone) => {
    expect(contrast(toRgb(tokens[tone]), tint(tokens[tone], tokens.card, tintAlpha))).toBeGreaterThanOrEqual(4.5)
  })
})
