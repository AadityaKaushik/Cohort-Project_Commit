import { useCallback, useRef, useState } from 'react'
import { textHints } from '@/lib/text-quality'

export function useSoftWarnings() {
  const accepted = useRef<Record<string, string>>({})
  const [shown, setShown] = useState<Record<string, string[]>>({})

  const check = useCallback((field: string, text: string, experienceField: boolean): string[] => {
    const messages = accepted.current[field] === text ? [] : textHints(text, experienceField).map((hint) => hint.message)
    setShown((current) => ({ ...current, [field]: messages }))
    return messages
  }, [])

  const accept = useCallback((field: string, text: string) => {
    accepted.current[field] = text
    setShown((current) => ({ ...current, [field]: [] }))
  }, [])

  const dismiss = useCallback((field: string) => {
    setShown((current) => ({ ...current, [field]: [] }))
  }, [])

  return { shown, check, accept, dismiss }
}
