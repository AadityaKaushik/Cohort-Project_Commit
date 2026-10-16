import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlarmClockIcon, CheckCircle2Icon, CircleIcon, LibraryIcon, ListTodoIcon, PencilLineIcon, PlusIcon, TargetIcon, TimerIcon, Trash2Icon } from 'lucide-react'
import { format, isPast } from 'date-fns'
import {
  createPreparationTask,
  deletePreparationTask,
  getPreparationProgress,
  listPreparationTasks,
  listTopics,
  updatePreparationTask,
  type PreparationTask,
  type TopicMasteryView,
} from '@/lib/prep-api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { ReviewTodayCard } from '@/components/prep/ReviewTodayCard'
import { CategoryProgressBar } from '@/components/prep/CategoryProgressBar'
import { TopicGrid } from '@/components/prep/TopicGrid'
import { EmptyState } from '@/components/shared/EmptyState'
import { FormField } from '@/components/shared/Form'
import { PageHeader } from '@/components/shared/PageHeader'
import { PageGuide } from '@/components/onboarding/PageGuide'
import { ErrorState, LoadingCards, LoadingRows } from '@/components/shared/QueryStates'
import { SectionCard } from '@/components/shared/SectionCard'
import { StatGrid, StatTile } from '@/components/shared/StatTile'
import { ToneBadge } from '@/components/shared/ToneBadge'
import { cn } from '@/lib/utils'

export function PrepPage() {
  const queryClient = useQueryClient()

  const progressQuery = useQuery({ queryKey: ['preparation-progress'], queryFn: getPreparationProgress })
  const tasksQuery = useQuery({ queryKey: ['preparation-tasks'], queryFn: listPreparationTasks })
  const topicsQuery = useQuery({ queryKey: ['topics'], queryFn: listTopics })

  const [selectedTopic, setSelectedTopic] = useState<TopicMasteryView | null>(null)
  const [newTaskTitle, setNewTaskTitle] = useState('')
  const [newTaskTopicId, setNewTaskTopicId] = useState('')

  const createMutation = useMutation({
    mutationFn: () => createPreparationTask({ topic_id: newTaskTopicId, title: newTaskTitle }),
    onSuccess: () => {
      setNewTaskTitle('')
      setNewTaskTopicId('')
      queryClient.invalidateQueries({ queryKey: ['preparation-tasks'] })
      queryClient.invalidateQueries({ queryKey: ['preparation-progress'] })
    },
  })

  const toggleMutation = useMutation({
    mutationFn: (task: PreparationTask) =>
      updatePreparationTask(task.id, { status: task.status === 'completed' ? 'open' : 'completed' }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['preparation-tasks'] })
      queryClient.invalidateQueries({ queryKey: ['preparation-progress'] })
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (id: string) => deletePreparationTask(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['preparation-tasks'] })
      queryClient.invalidateQueries({ queryKey: ['preparation-progress'] })
    },
  })

  function handleCreateTask(event: FormEvent) {
    event.preventDefault()
    if (!newTaskTitle.trim() || !newTaskTopicId) {
      return
    }
    createMutation.mutate()
  }

  const tasks = tasksQuery.data ?? []
  const completed = tasks.filter((task) => task.status === 'completed').length
  const overdue = tasks.filter((task) => task.status === 'open' && task.due_date !== null && isPast(new Date(task.due_date))).length
  const topics = progressQuery.data?.topics ?? []
  const strongTopics = topics.filter((topic) => topic.sufficient_evidence && topic.mastery_score >= 0.7).length

  return (
    <div className="space-y-6">
      <PageHeader
        title="Interview Prep"
        description="Practice tracking and topic mastery that fades over time, just like real skill does."
        actions={
          <>
            <PageGuide guideId="prep" />
            <Button asChild variant="outline">
              <Link to="/prep/mock-interview">
                <TimerIcon aria-hidden />
                Mock interview
              </Link>
            </Button>
            <Button asChild>
              <Link to="/prep/questions">
                <LibraryIcon aria-hidden />
                Question bank
              </Link>
            </Button>
          </>
        }
      />

      <StatGrid>
        <StatTile label="Tasks done" icon={CheckCircle2Icon} tone="success" value={tasksQuery.data ? completed : null} loading={tasksQuery.isLoading} hint={`of ${tasks.length} in your plan`} />
        <StatTile label="Open tasks" icon={ListTodoIcon} value={tasksQuery.data ? tasks.length - completed : null} loading={tasksQuery.isLoading} hint="Still to practise" />
        <StatTile
          label="Overdue"
          icon={AlarmClockIcon}
          tone="warning"
          value={tasksQuery.data ? overdue : null}
          loading={tasksQuery.isLoading}
          hint={overdue > 0 ? 'Past their due date' : 'Nothing slipping'}
        />
        <StatTile label="Strong topics" icon={TargetIcon} tone="info" value={progressQuery.data ? strongTopics : null} loading={progressQuery.isLoading} hint={`of ${topics.length} tracked topics`} />
      </StatGrid>

      <div data-guide="review">
        <ReviewTodayCard limit={20} />
      </div>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.3fr)]">
        <SectionCard title="Progress by category" description="Task completion, with average mastery alongside.">
          {progressQuery.isLoading && <LoadingCards count={3} label="Loading progress" className="[&>*]:p-3" />}
          {progressQuery.isError && <ErrorState title="Couldn't load progress" error={progressQuery.error} onRetry={() => progressQuery.refetch()} />}
          {progressQuery.data && (
            <div className="space-y-5">
              {progressQuery.data.categories.map((category) => (
                <CategoryProgressBar key={category.category} progress={category} />
              ))}
            </div>
          )}
        </SectionCard>

        <div data-guide="tasks">
        <SectionCard title="Preparation tasks" description="Turn a weak topic into a scheduled task.">
          <div className="space-y-4">
            <form onSubmit={handleCreateTask} className="flex flex-col gap-3 rounded-lg border bg-surface-2/50 p-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <FormField id="new-task-title" label="New task">
                  <Input
                    id="new-task-title"
                    className="bg-card"
                    placeholder="e.g. Practice 10 array problems"
                    value={newTaskTitle}
                    onChange={(event) => setNewTaskTitle(event.target.value)}
                  />
                </FormField>
              </div>
              <div className="w-full sm:w-48">
                <FormField id="new-task-topic" label="Topic">
                  <Select value={newTaskTopicId} onValueChange={setNewTaskTopicId}>
                    <SelectTrigger id="new-task-topic" className="w-full bg-card">
                      <SelectValue placeholder="Choose a topic" />
                    </SelectTrigger>
                    <SelectContent>
                      {(topicsQuery.data ?? []).map((topic) => (
                        <SelectItem key={topic.id} value={topic.id}>
                          {topic.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </FormField>
              </div>
              <Button type="submit" disabled={createMutation.isPending || !newTaskTitle.trim() || !newTaskTopicId}>
                <PlusIcon aria-hidden />
                Add task
              </Button>
            </form>

            {tasksQuery.isLoading && <LoadingRows rows={3} label="Loading tasks" />}
            {tasksQuery.isError && <ErrorState title="Couldn't load your tasks" error={tasksQuery.error} onRetry={() => tasksQuery.refetch()} />}
            {tasksQuery.data && tasksQuery.data.length === 0 && (
              <EmptyState
                compact
                icon={PencilLineIcon}
                title="No preparation tasks yet"
                description="Turn a weak topic into a scheduled task above."
                action={
                  <Button onClick={() => document.getElementById('new-task-title')?.focus()}>
                    <PlusIcon aria-hidden />
                    Add your first task
                  </Button>
                }
              />
            )}
            {tasksQuery.data && tasksQuery.data.length > 0 && (
              <ul className="divide-y overflow-hidden rounded-lg border">
                {tasksQuery.data.map((task) => {
                  const isOverdue = task.status === 'open' && task.due_date !== null && isPast(new Date(task.due_date))
                  const done = task.status === 'completed'
                  return (
                    <li key={task.id} className="group/task flex items-center justify-between gap-3 px-3 py-2.5 transition-colors hover:bg-accent/40">
                      <div className="flex min-w-0 items-center gap-3">
                        <button
                          type="button"
                          aria-label={done ? 'Mark as open' : 'Mark as completed'}
                          onClick={() => toggleMutation.mutate(task)}
                          className="rounded-full text-muted-foreground transition-[color,transform] duration-150 hover:text-primary active:scale-90"
                        >
                          {done ? <CheckCircle2Icon className="size-5 text-success" /> : <CircleIcon className="size-5" />}
                        </button>
                        <div className="min-w-0">
                          <p className={cn('truncate text-sm', done && 'text-muted-foreground line-through')}>{task.title}</p>
                          <p className="truncate text-xs text-muted-foreground">
                            {task.topic_name}
                            {task.due_date && ` · Due ${format(new Date(task.due_date), 'MMM d, yyyy')}`}
                          </p>
                        </div>
                        {isOverdue && <ToneBadge tone="danger">Overdue</ToneBadge>}
                      </div>
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        aria-label="Delete task"
                        onClick={() => deleteMutation.mutate(task.id)}
                        className="text-muted-foreground hover:text-destructive sm:opacity-0 sm:group-hover/task:opacity-100 sm:focus-visible:opacity-100"
                      >
                        <Trash2Icon aria-hidden />
                      </Button>
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </SectionCard>
        </div>
      </div>

      <div data-guide="mastery">
      <SectionCard title="Topic mastery" description="Select a topic to see how much evidence it rests on, then practise it.">
        {progressQuery.isLoading && <Skeleton className="h-40 w-full rounded-lg" />}
        {progressQuery.data && <TopicGrid topics={progressQuery.data.topics} onSelectTopic={setSelectedTopic} />}
        {selectedTopic && (
          <p className="mt-4 flex flex-wrap items-center gap-x-2 rounded-lg bg-surface-2/60 px-3 py-2 text-sm text-muted-foreground">
            <span>
              <span className="font-medium text-foreground">{selectedTopic.topic_name}</span>: {selectedTopic.observation_count} observation
              {selectedTopic.observation_count === 1 ? '' : 's'} so far.
            </span>
            <Link to={`/prep/questions?topic_id=${selectedTopic.topic_id}`} className="font-medium text-primary underline-offset-4 hover:underline">
              Practice this topic
            </Link>
          </p>
        )}
      </SectionCard>
      </div>
    </div>
  )
}
