import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowDownIcon, ArrowUpIcon, DownloadIcon, PencilIcon, PlusIcon, Trash2Icon } from 'lucide-react'
import {
  addResumeSection,
  deleteResumeSection,
  exportResumePdf,
  listResumeSections,
  reorderResumeSections,
  updateResume,
  updateResumeSection,
  type ResumeSection,
  type ResumeSectionInput,
  type ResumeSectionType,
} from '@/lib/resumes-api'
import { ApiError } from '@/lib/api-client'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { FormError, FormField } from '@/components/shared/Form'
import { SectionCard } from '@/components/shared/SectionCard'

const TYPE_LABELS: Record<ResumeSectionType, string> = {
  experience: 'Experience',
  project: 'Project',
  skill: 'Skill',
  education: 'Education',
  achievement: 'Achievement',
}

const EMPTY: ResumeSectionInput = {
  section_type: 'experience',
  title: '',
  organisation: '',
  start_date: '',
  end_date: '',
  description: '',
}

function toInput(section: ResumeSection): ResumeSectionInput {
  return {
    section_type: section.section_type,
    title: section.title ?? '',
    organisation: section.organisation ?? '',
    start_date: section.start_date ?? '',
    end_date: section.end_date ?? '',
    description: section.description ?? '',
  }
}

function blankToNull(input: ResumeSectionInput): ResumeSectionInput {
  return {
    section_type: input.section_type,
    title: input.title || null,
    organisation: input.organisation || null,
    start_date: input.start_date || null,
    end_date: input.end_date || null,
    description: input.description || null,
  }
}

function errorText(error: unknown) {
  return error instanceof ApiError ? error.message : 'Something went wrong. Please try again.'
}

interface EntryFormProps {
  initial: ResumeSectionInput
  submitLabel: string
  pending: boolean
  onSubmit: (input: ResumeSectionInput) => void
  onCancel: () => void
}

function EntryForm({ initial, submitLabel, pending, onSubmit, onCancel }: EntryFormProps) {
  const [value, setValue] = useState(initial)
  const set = (patch: Partial<ResumeSectionInput>) => setValue((current) => ({ ...current, ...patch }))

  return (
    <form
      className="space-y-3 rounded-lg border bg-surface-2/50 p-3 sm:p-4"
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit(blankToNull(value))
      }}
    >
      <div className="grid gap-3 sm:grid-cols-2">
        <FormField id="section-type" label="Type">
          <NativeSelect
            id="section-type"
            value={value.section_type}
            onChange={(event) => set({ section_type: event.target.value as ResumeSectionType })}
          >
            {Object.entries(TYPE_LABELS).map(([type, label]) => (
              <option key={type} value={type}>
                {label}
              </option>
            ))}
          </NativeSelect>
        </FormField>
        <FormField id="section-title" label="Title">
          <Input
            id="section-title"
            value={value.title ?? ''}
            maxLength={200}
            onChange={(event) => set({ title: event.target.value })}
          />
        </FormField>
        <FormField id="section-organisation" label="Organisation">
          <Input
            id="section-organisation"
            value={value.organisation ?? ''}
            maxLength={200}
            onChange={(event) => set({ organisation: event.target.value })}
          />
        </FormField>
        <div className="grid grid-cols-2 gap-3">
          <FormField id="section-start" label="Start">
            <Input
              id="section-start"
              type="date"
              value={value.start_date ?? ''}
              onChange={(event) => set({ start_date: event.target.value })}
            />
          </FormField>
          <FormField id="section-end" label="End">
            <Input
              id="section-end"
              type="date"
              value={value.end_date ?? ''}
              onChange={(event) => set({ end_date: event.target.value })}
            />
          </FormField>
        </div>
      </div>
      <FormField id="section-description" label="Description">
        <Textarea
          id="section-description"
          rows={3}
          value={value.description ?? ''}
          maxLength={4000}
          onChange={(event) => set({ description: event.target.value })}
        />
      </FormField>
      <div className="flex flex-wrap justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={pending}>
          {pending ? 'Saving…' : submitLabel}
        </Button>
      </div>
    </form>
  )
}

interface ResumeSectionsEditorProps {
  resumeId: string
  summary: string | null
}

export function ResumeSectionsEditor({ resumeId, summary }: ResumeSectionsEditorProps) {
  const queryClient = useQueryClient()
  const key = ['resume-sections', resumeId]
  const [adding, setAdding] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [summaryDraft, setSummaryDraft] = useState(summary ?? '')
  const [error, setError] = useState<string | null>(null)

  const { data: sections = [], isLoading } = useQuery({ queryKey: key, queryFn: () => listResumeSections(resumeId) })

  const refresh = () => queryClient.invalidateQueries({ queryKey: key })
  const onError = (mutationError: unknown) => setError(errorText(mutationError))

  const addMutation = useMutation({
    mutationFn: (input: ResumeSectionInput) => addResumeSection(resumeId, input),
    onSuccess: () => {
      setError(null)
      setAdding(false)
      refresh()
    },
    onError,
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, input }: { id: string; input: ResumeSectionInput }) => updateResumeSection(resumeId, id, input),
    onSuccess: () => {
      setError(null)
      setEditingId(null)
      refresh()
    },
    onError,
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deleteResumeSection(resumeId, id),
    onSuccess: () => {
      setError(null)
      refresh()
    },
    onError,
  })

  const reorderMutation = useMutation({
    mutationFn: (ids: string[]) => reorderResumeSections(resumeId, ids),
    onSuccess: () => {
      setError(null)
      refresh()
    },
    onError,
  })

  const summaryMutation = useMutation({
    mutationFn: () => updateResume(resumeId, { summary: summaryDraft }),
    onSuccess: () => {
      setError(null)
      queryClient.invalidateQueries({ queryKey: ['resume', resumeId] })
    },
    onError,
  })

  const exportMutation = useMutation({
    mutationFn: () => exportResumePdf(resumeId),
    onSuccess: ({ blob, filename }) => {
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = filename ?? 'resume.pdf'
      document.body.appendChild(link)
      link.click()
      link.remove()
      URL.revokeObjectURL(url)
    },
    onError,
  })

  function move(index: number, offset: number) {
    const ids = sections.map((section) => section.id)
    const target = index + offset
    ;[ids[index], ids[target]] = [ids[target]!, ids[index]!]
    reorderMutation.mutate(ids)
  }

  return (
    <SectionCard
      title="Resume content"
      description="Build the resume from entries. Export gives a one-page PDF."
      action={
        <>
          <Button variant="outline" size="sm" onClick={() => exportMutation.mutate()} disabled={exportMutation.isPending}>
            <DownloadIcon aria-hidden />
            {exportMutation.isPending ? 'Exporting…' : 'Export PDF'}
          </Button>
          <Button size="sm" onClick={() => setAdding(true)} disabled={adding}>
            <PlusIcon aria-hidden />
            Add entry
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="space-y-2">
          <FormField id="resume-summary" label="Summary">
            <Textarea
              id="resume-summary"
              rows={3}
              value={summaryDraft}
              onChange={(event) => setSummaryDraft(event.target.value)}
            />
          </FormField>
          <Button
            variant="outline"
            size="sm"
            onClick={() => summaryMutation.mutate()}
            disabled={summaryMutation.isPending || summaryDraft === (summary ?? '')}
          >
            {summaryMutation.isPending ? 'Saving…' : 'Save summary'}
          </Button>
        </div>

        {error && <FormError>{error}</FormError>}

        {adding && (
          <EntryForm
            initial={EMPTY}
            submitLabel="Add entry"
            pending={addMutation.isPending}
            onSubmit={(input) => addMutation.mutate(input)}
            onCancel={() => setAdding(false)}
          />
        )}

        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading entries…</p>
        ) : sections.length === 0 && !adding ? (
          <p className="text-sm text-muted-foreground">No entries yet. Add experience, projects, skills or education.</p>
        ) : (
          <ul className="space-y-2">
            {sections.map((section, index) =>
              editingId === section.id ? (
                <li key={section.id}>
                  <EntryForm
                    initial={toInput(section)}
                    submitLabel="Save entry"
                    pending={updateMutation.isPending}
                    onSubmit={(input) => updateMutation.mutate({ id: section.id, input })}
                    onCancel={() => setEditingId(null)}
                  />
                </li>
              ) : (
                <li key={section.id} className="flex flex-wrap items-start gap-3 rounded-lg border p-3">
                  <div className="min-w-0 flex-1 basis-40">
                    <p className="text-xs font-medium uppercase text-muted-foreground">{TYPE_LABELS[section.section_type]}</p>
                    <p className="break-words text-sm font-medium">
                      {[section.title, section.organisation].filter(Boolean).join(' at ') || 'Untitled entry'}
                    </p>
                    {section.description && (
                      <p className="mt-1 line-clamp-2 break-words text-sm text-muted-foreground">{section.description}</p>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Move ${section.title ?? 'entry'} up`}
                      disabled={index === 0 || reorderMutation.isPending}
                      onClick={() => move(index, -1)}
                    >
                      <ArrowUpIcon aria-hidden />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Move ${section.title ?? 'entry'} down`}
                      disabled={index === sections.length - 1 || reorderMutation.isPending}
                      onClick={() => move(index, 1)}
                    >
                      <ArrowDownIcon aria-hidden />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Edit ${section.title ?? 'entry'}`}
                      onClick={() => setEditingId(section.id)}
                    >
                      <PencilIcon aria-hidden />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon-sm"
                      aria-label={`Delete ${section.title ?? 'entry'}`}
                      disabled={deleteMutation.isPending}
                      onClick={() => deleteMutation.mutate(section.id)}
                    >
                      <Trash2Icon aria-hidden />
                    </Button>
                  </div>
                </li>
              ),
            )}
          </ul>
        )}
      </div>
    </SectionCard>
  )
}
