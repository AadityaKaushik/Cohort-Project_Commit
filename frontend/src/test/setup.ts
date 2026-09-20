import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'
import '@testing-library/jest-dom/vitest'

// Vitest doesn't auto-detect RTL's cleanup hook unless `test.globals` is on
// (we deliberately keep explicit imports instead of globals), so it's wired
// up here: otherwise each test file's DOM nodes pile up across tests.
afterEach(() => {
  cleanup()
})

// jsdom doesn't implement the Pointer Events capture API or scrollIntoView,
// which Radix's Select (shadcn's <Select>, first exercised interactively by
// PrepPage's topic picker, P31) calls internally when it opens/positions its
// listbox. Without these no-op polyfills, opening a Select in a test throws
// "target.hasPointerCapture is not a function" rather than testing anything
// about the component under test.
if (!Element.prototype.hasPointerCapture) {
  Element.prototype.hasPointerCapture = () => false
}
if (!Element.prototype.setPointerCapture) {
  Element.prototype.setPointerCapture = () => {}
}
if (!Element.prototype.releasePointerCapture) {
  Element.prototype.releasePointerCapture = () => {}
}
if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = () => {}
}
