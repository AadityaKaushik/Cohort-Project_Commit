import { useRef, useState, type DragEvent } from 'react'
import { FileUpIcon, UploadCloudIcon } from 'lucide-react'
import { cn } from '@/lib/utils'
import { buttonVariants } from '@/components/ui/button'

interface UploadDropzoneProps {
  onFileSelected: (file: File) => void
  /** Comma-separated accept list, mirrors the backend's extension check
   * (ValidatingFileStorage, P19) — client-side only as a UX hint, since the
   * server re-validates extension, MIME, and magic bytes regardless. */
  accept?: string
  disabled?: boolean
  /** Shown while an upload triggered by this dropzone is in flight. */
  isUploading?: boolean
  helperText?: string
}

/**
 * Drag-and-drop or click-to-browse file picker (US-21). It never uploads
 * anything itself — it only ever hands the chosen `File` back to the
 * caller, which is what actually calls `uploadResumeFile` — so this
 * component stays reusable for any future upload surface.
 */
export function UploadDropzone({
  onFileSelected,
  accept = '.pdf,.doc,.docx',
  disabled = false,
  isUploading = false,
  helperText = 'PDF or Word document, up to 5 MB',
}: UploadDropzoneProps) {
  const [isDragOver, setIsDragOver] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault()
    setIsDragOver(false)
    if (disabled || isUploading) return
    const file = event.dataTransfer.files?.[0]
    if (file) onFileSelected(file)
  }

  return (
    <div
      role="button"
      tabIndex={0}
      aria-disabled={disabled || isUploading}
      className={cn(
        'flex flex-col items-center justify-center gap-2 rounded-xl border-2 border-dashed bg-surface-2/50 p-6 text-center transition-[background-color,border-color] duration-200 focus-visible:outline-2 focus-visible:outline-ring',
        isDragOver ? 'border-primary bg-primary/8' : 'border-border',
        (disabled || isUploading) && 'cursor-not-allowed opacity-60',
        !disabled && !isUploading && 'cursor-pointer hover:border-primary/60',
      )}
      onClick={() => !disabled && !isUploading && inputRef.current?.click()}
      onKeyDown={(event) => {
        if ((event.key === 'Enter' || event.key === ' ') && !disabled && !isUploading) {
          event.preventDefault()
          inputRef.current?.click()
        }
      }}
      onDragOver={(event) => {
        event.preventDefault()
        if (!disabled && !isUploading) setIsDragOver(true)
      }}
      onDragLeave={() => setIsDragOver(false)}
      onDrop={handleDrop}
    >
      <span className="flex size-10 items-center justify-center rounded-xl bg-card text-primary ring-1 ring-border">
        {isUploading ? <FileUpIcon aria-hidden className="size-5 animate-pulse" /> : <UploadCloudIcon aria-hidden className="size-5" />}
      </span>
      <p className="text-sm font-medium">{isUploading ? 'Uploading…' : 'Drag a file here, or click to browse'}</p>
      <p className="text-xs text-muted-foreground">{helperText}</p>
      <span aria-hidden className={buttonVariants({ variant: 'outline', size: 'sm', className: 'mt-1 pointer-events-none' })}>
        Choose file
      </span>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className="hidden"
        data-testid="upload-dropzone-input"
        disabled={disabled || isUploading}
        onChange={(event) => {
          const file = event.target.files?.[0]
          if (file) onFileSelected(file)
          event.target.value = ''
        }}
      />
    </div>
  )
}
