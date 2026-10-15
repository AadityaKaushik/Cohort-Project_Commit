import { useState, type ReactNode } from 'react'
import { CheckIcon, CopyIcon } from 'lucide-react'
import type { AiDraft } from '@/lib/ai-api'
import { useToast } from '@/hooks/use-toast'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'

// Parents give this a key that changes with every new draft so the text resets.
interface DraftEditorProps {
  label: string
  draft: AiDraft
  rows?: number
  actions?: (text: string) => ReactNode
}

function draftNotice(draft: AiDraft): string | null {
  if (!draft.ai_available) {
    return 'AI is unavailable, try again later. This is a plain template you can edit.'
  }
  if (draft.source === 'template') {
    return 'The model gave an answer that could not be used, so this is a plain template you can edit.'
  }
  return null
}

export function DraftEditor({ label, draft, rows = 12, actions }: DraftEditorProps) {
  const { toast } = useToast()
  const [text, setText] = useState(draft.draft)
  const [copied, setCopied] = useState(false)
  const notice = draftNotice(draft)

  async function copy() {
    try {
      await navigator.clipboard.writeText(text)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 2000)
    } catch {
      toast({ title: 'Could not copy to the clipboard', variant: 'error' })
    }
  }

  return (
    <div className="space-y-3">
      {notice && (
        <p role="status" className="rounded-lg border border-dashed bg-surface-2/50 p-3 text-sm text-muted-foreground">
          {notice}
        </p>
      )}
      <Textarea aria-label={label} value={text} rows={rows} onChange={(event) => setText(event.target.value)} />
      <p className="text-xs text-muted-foreground">This is a draft. Edit it before you use it. Nothing is sent anywhere.</p>
      <div className="flex flex-wrap gap-2">
        <Button type="button" variant="outline" size="sm" onClick={copy}>
          {copied ? <CheckIcon aria-hidden /> : <CopyIcon aria-hidden />}
          {copied ? 'Copied' : 'Copy'}
        </Button>
        {actions?.(text)}
      </div>
    </div>
  )
}
