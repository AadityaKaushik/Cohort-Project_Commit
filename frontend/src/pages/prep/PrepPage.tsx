import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertCircleIcon, CheckCircle2Icon, CircleIcon, PencilLineIcon, PlusIcon, Trash2Icon } from 'lucide-react'
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
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import { CategoryProgressBar } from '@/components/prep/CategoryProgressBar'
import { TopicGrid } from '@/components/prep/TopicGrid'

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

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Interview Prep</h2>
        <p className="text-sm text-muted-foreground">
          Practice tracking and topic mastery that fades over time, just like real skill does.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Progress by category</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          {progressQuery.isLoading && (
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, index) => (
                <Skeleton key={index} className="h-10 w-full" />
              ))}
            </div>
          )}
          {progressQuery.isError && (
            <Alert variant="destructive">
              <AlertCircleIcon className="h-4 w-4" />
              <AlertTitle>Couldn&apos;t load progress</AlertTitle>
              <AlertDescription>Something went wrong talking to the server.</AlertDescription>
            </Alert>
          )}
          {progressQuery.data &&
            progressQuery.data.categories.map((category) => <CategoryProgressBar key={category.category} progress={category} />)}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Preparation tasks</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <form onSubmit={handleCreateTask} className="flex flex-col gap-2 sm:flex-row sm:items-end">
            <div className="flex-1 space-y-1">
              <Label htmlFor="new-task-title">New task</Label>
              <Input
                id="new-task-title"
                placeholder="e.g. Practice 10 array problems"
                value={newTaskTitle}
                onChange={(event) => setNewTaskTitle(event.target.value)}
              />
            </div>
            <div className="w-full space-y-1 sm:w-56">
              <Label htmlFor="new-task-topic">Topic</Label>
              <Select value={newTaskTopicId} onValueChange={setNewTaskTopicId}>
                <SelectTrigger id="new-task-topic">
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
            </div>
            <Button type="submit" disabled={createMutation.isPending || !newTaskTitle.trim() || !newTaskTopicId}>
              <PlusIcon className="h-4 w-4" />
              Add task
            </Button>
          </form>

          {tasksQuery.isLoading && <Skeleton className="h-24 w-full" />}
          {tasksQuery.data && tasksQuery.data.length === 0 && (
            <div className="flex items-center gap-3 rounded-md border border-dashed p-6 text-center text-sm text-muted-foreground">
              <PencilLineIcon className="h-5 w-5" />
              <span>No preparation tasks yet — turn a weak topic into a scheduled task above.</span>
            </div>
          )}
          {tasksQuery.data && tasksQuery.data.length > 0 && (
            <ul className="divide-y rounded-md border">
              {tasksQuery.data.map((task) => {
                const overdue = task.status === 'open' && task.due_date !== null && isPast(new Date(task.due_date))
                return (
                  <li key={task.id} className="flex items-center justify-between gap-3 p-3">
                    <div className="flex items-center gap-2">
                      <button
                        type="button"
                        aria-label={task.status === 'completed' ? 'Mark as open' : 'Mark as completed'}
                        onClick={() => toggleMutation.mutate(task)}
                        className="text-muted-foreground hover:text-primary"
                      >
                        {task.status === 'completed' ? (
                          <CheckCircle2Icon className="h-5 w-5 text-emerald-600" />
                        ) : (
                          <CircleIcon className="h-5 w-5" />
                        )}
                      </button>
                      <div>
                        <p className={task.status === 'completed' ? 'text-sm line-through text-muted-foreground' : 'text-sm'}>
                          {task.title}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {task.topic_name}
                          {task.due_date && ` · Due ${format(new Date(task.due_date), 'MMM d, yyyy')}`}
                        </p>
                      </div>
                      {overdue && <Badge variant="destructive">Overdue</Badge>}
                    </div>
                    <button
                      type="button"
                      aria-label="Delete task"
                      onClick={() => deleteMutation.mutate(task.id)}
                      className="text-muted-foreground hover:text-destructive"
                    >
                      <Trash2Icon className="h-4 w-4" />
                    </button>
                  </li>
                )
              })}
            </ul>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Topic mastery</CardTitle>
        </CardHeader>
        <CardContent>
          {progressQuery.isLoading && <Skeleton className="h-40 w-full" />}
          {progressQuery.data && <TopicGrid topics={progressQuery.data.topics} onSelectTopic={setSelectedTopic} />}
          {selectedTopic && (
            <p className="mt-3 text-sm text-muted-foreground">
              {selectedTopic.topic_name}: {selectedTopic.observation_count} observation
              {selectedTopic.observation_count === 1 ? '' : 's'} so far.
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
