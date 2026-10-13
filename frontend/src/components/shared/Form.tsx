import { cloneElement, isValidElement, useId, type ReactElement, type ReactNode } from 'react'
import { AlertCircleIcon } from 'lucide-react'
import { Label } from '@/components/ui/label'
import { cn } from '@/lib/utils'

interface FormFieldProps {
  id: string
  label: ReactNode
  hint?: ReactNode
  error?: string | null
  className?: string
  children: ReactElement<Record<string, unknown>>
}

/** Label, control, then either the hint or the error; the control is wired to both through aria attributes. */
export function FormField({ id, label, hint, error, className, children }: FormFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = error ? errorId : hint ? hintId : undefined
  const control = isValidElement(children)
    ? cloneElement(children, { 'aria-invalid': error ? true : undefined, 'aria-describedby': describedBy })
    : children

  return (
    <div className={cn('min-w-0 space-y-1.5', className)}>
      <Label htmlFor={id}>{label}</Label>
      {control}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && <FieldMessage id={errorId}>{error}</FieldMessage>}
    </div>
  )
}

export function FieldMessage({ id, children }: { id: string; children: ReactNode }) {
  return (
    <p id={id} className="flex items-center gap-1 text-xs font-medium text-destructive">
      <AlertCircleIcon aria-hidden className="size-3.5 shrink-0" />
      {children}
    </p>
  )
}

interface FormSectionProps {
  title: string
  description?: string
  columns?: 1 | 2 | 3
  children: ReactNode
}

/** A labelled group of fields: the heading and its context sit beside the fields on wide screens and above them on narrow ones. */
export function FormSection({ title, description, columns = 2, children }: FormSectionProps) {
  const headingId = useId()
  return (
    <fieldset
      aria-labelledby={headingId}
      className="grid gap-4 border-t pt-6 first:border-t-0 first:pt-0 lg:grid-cols-[12rem_minmax(0,1fr)] lg:gap-8"
    >
      <div className="space-y-1">
        <h3 id={headingId} className="font-heading text-sm font-semibold tracking-tight">
          {title}
        </h3>
        {description && <p className="text-xs text-pretty text-muted-foreground">{description}</p>}
      </div>
      <div className={cn('grid gap-4', columns === 2 && 'sm:grid-cols-2', columns === 3 && 'sm:grid-cols-3')}>{children}</div>
    </fieldset>
  )
}

export function FormActions({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn('flex flex-col-reverse gap-2 border-t pt-5 sm:flex-row sm:items-center sm:justify-end', className)}>{children}</div>
  )
}

export function FormError({ children }: { children: ReactNode }) {
  return (
    <p role="alert" className="flex items-start gap-2 rounded-lg border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive">
      <AlertCircleIcon aria-hidden className="mt-0.5 size-4 shrink-0" />
      <span>{children}</span>
    </p>
  )
}

export function CheckboxField({
  checked,
  onChange,
  label,
  description,
  className,
}: {
  checked: boolean
  onChange: (checked: boolean) => void
  label: string
  description?: string
  className?: string
}) {
  return (
    <label
      className={cn(
        'flex cursor-pointer items-start gap-3 rounded-lg border bg-surface-2/50 p-3 text-sm transition-colors hover:border-primary/30 has-[:checked]:border-primary/40 has-[:checked]:bg-primary/5',
        className,
      )}
    >
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} className="mt-0.5 size-4 rounded accent-primary" />
      <span className="space-y-0.5">
        <span className="block font-medium">{label}</span>
        {description && <span className="block text-xs text-muted-foreground">{description}</span>}
      </span>
    </label>
  )
}
