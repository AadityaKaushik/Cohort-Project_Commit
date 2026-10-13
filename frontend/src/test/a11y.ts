import axe from 'axe-core'

/** Runs axe against a rendered tree and returns only critical violations. Colour contrast is covered by theme-contrast.test.ts, since jsdom cannot compute styles. */
export async function criticalViolations(container: Element) {
  const results = await axe.run(container, { rules: { 'color-contrast': { enabled: false } } })
  return results.violations.filter((violation) => violation.impact === 'critical').map((violation) => violation.id)
}

export function mockReducedMotion(reduce: boolean) {
  window.matchMedia = (query: string) =>
    ({
      matches: reduce && query.includes('prefers-reduced-motion'),
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList
}
