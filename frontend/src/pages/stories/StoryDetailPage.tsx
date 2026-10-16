import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { PencilIcon, SearchXIcon, Trash2Icon } from 'lucide-react'
import { deleteStory, getStory, storyKeys } from '@/lib/stories-api'
import { formatLabel } from '@/lib/labels'
import { useUndoableDelete } from '@/hooks/use-undo-delete'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { EmptyState } from '@/components/shared/EmptyState'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageSkeleton } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { ToneBadge } from '@/components/shared/ToneBadge'

const SECTIONS = [
  ['situation', 'Situation'],
  ['task', 'Task'],
  ['action', 'Action'],
  ['result', 'Result'],
] as const

export function StoryDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const undoableDelete = useUndoableDelete()
  const [deleteOpen, setDeleteOpen] = useState(false)

  const { data, isLoading, isError } = useQuery({ queryKey: storyKeys.detail(id ?? ''), queryFn: () => getStory(id!), enabled: Boolean(id) })

  function handleDelete() {
    setDeleteOpen(false)
    undoableDelete({
      id: id!,
      title: 'Story deleted',
      send: () => deleteStory(id!),
      onSettled: () => queryClient.invalidateQueries({ queryKey: storyKeys.all }),
      errorTitle: 'Could not delete the story',
    })
    navigate('/stories')
  }

  if (isLoading) {
    return <PageSkeleton label="Loading story" />
  }
  if (isError || !data) {
    return (
      <EmptyState
        icon={SearchXIcon}
        title="Story not found"
        description="It may have been deleted, or the link may be wrong."
        action={
          <Button asChild>
            <Link to="/stories">Back to stories</Link>
          </Button>
        }
      />
    )
  }

  return (
    <div className="max-w-3xl space-y-6">
      <PageHeader
        back={{ to: '/stories', label: 'Stories' }}
        title={data.title}
        meta={data.competencies.map((item) => (
          <ToneBadge key={item} tone="neutral">
            {formatLabel(item)}
          </ToneBadge>
        ))}
        actions={
          <>
            <Button variant="outline" asChild>
              <Link to={`/stories/${data.id}/edit`}>
                <PencilIcon aria-hidden />
                Edit
              </Link>
            </Button>
            <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
              <DialogTrigger asChild>
                <Button variant="destructive">
                  <Trash2Icon aria-hidden />
                  Delete
                </Button>
              </DialogTrigger>
              <DialogContent>
                <DialogHeader>
                  <DialogTitle>Delete this story?</DialogTitle>
                  <DialogDescription>You can undo for a few seconds after deleting.</DialogDescription>
                </DialogHeader>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setDeleteOpen(false)}>
                    Cancel
                  </Button>
                  <Button variant="destructive" onClick={handleDelete}>
                    Delete
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        }
      />
      {data.interview_id && (
        <p className="text-sm text-muted-foreground">
          Linked to{' '}
          <Link to={`/interviews/${data.interview_id}`} className="font-medium text-foreground underline-offset-4 hover:underline">
            an interview
          </Link>
          .
        </p>
      )}
      {SECTIONS.map(([key, label]) => (
        <SectionCard key={key} title={label}>
          <p className="text-sm whitespace-pre-wrap">{data[key] || 'Nothing written yet.'}</p>
        </SectionCard>
      ))}
    </div>
  )
}
