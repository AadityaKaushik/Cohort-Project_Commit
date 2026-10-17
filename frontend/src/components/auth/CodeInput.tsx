import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react'
import { cn } from '@/lib/utils'

export const CODE_LENGTH = 6

/** Six boxes for an emailed code. Typing moves forward, Backspace moves back, and pasting fills every box. */
export function CodeInput({
  value,
  onChange,
  disabled,
  invalid,
  autoFocus,
}: {
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  invalid?: boolean
  autoFocus?: boolean
}) {
  const boxes = useRef<(HTMLInputElement | null)[]>([])
  const digits = Array.from({ length: CODE_LENGTH }, (_, index) => value[index] ?? '')

  function focus(index: number) {
    boxes.current[Math.max(0, Math.min(CODE_LENGTH - 1, index))]?.focus()
  }

  function setDigit(index: number, digit: string) {
    const next = digits.slice()
    next[index] = digit
    onChange(next.join('').slice(0, CODE_LENGTH))
  }

  function handleInput(index: number, raw: string) {
    const typed = raw.replace(/\D/g, '')
    if (typed.length > 1) {
      fillFrom(index, typed)
      return
    }
    setDigit(index, typed)
    if (typed) focus(index + 1)
  }

  function fillFrom(index: number, text: string) {
    const next = digits.slice()
    for (const [offset, digit] of [...text].entries()) {
      if (index + offset < CODE_LENGTH) next[index + offset] = digit
    }
    onChange(next.join(''))
    focus(index + text.length)
  }

  function handlePaste(index: number, event: ClipboardEvent<HTMLInputElement>) {
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '')
    if (!pasted) return
    event.preventDefault()
    fillFrom(index, pasted)
  }

  function handleKeyDown(index: number, event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Backspace' && !digits[index]) {
      event.preventDefault()
      setDigit(index - 1 < 0 ? 0 : index - 1, '')
      focus(index - 1)
    } else if (event.key === 'ArrowLeft') {
      focus(index - 1)
    } else if (event.key === 'ArrowRight') {
      focus(index + 1)
    }
  }

  return (
    <div role="group" aria-label="Code" className="flex gap-2">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(element) => {
            boxes.current[index] = element
          }}
          aria-label={`Digit ${index + 1} of ${CODE_LENGTH}`}
          aria-invalid={invalid || undefined}
          inputMode="numeric"
          autoComplete={index === 0 ? 'one-time-code' : 'off'}
          maxLength={CODE_LENGTH}
          autoFocus={autoFocus && index === 0}
          disabled={disabled}
          value={digit}
          onChange={(event) => handleInput(index, event.target.value)}
          onPaste={(event) => handlePaste(index, event)}
          onKeyDown={(event) => handleKeyDown(index, event)}
          onFocus={(event) => event.target.select()}
          className={cn(
            'tabular h-12 w-full min-w-0 rounded-md border border-input bg-card text-center text-xl font-semibold outline-none transition-colors focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/40 disabled:opacity-50 aria-invalid:border-destructive',
          )}
        />
      ))}
    </div>
  )
}
