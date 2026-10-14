import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangleIcon, Trash2Icon } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { NativeSelect } from '@/components/ui/native-select'
import { EmptyState } from '@/components/shared/EmptyState'
import { LoadingRows } from '@/components/shared/QueryStates'
import {
  deleteGoal,
  getActivity,
  GOAL_METRICS,
  goalKeys,
  listGoals,
  setGoal,
  type Activity,
  type Goal,
  type GoalMetric,
} from '@/lib/goals-api'
import { formatLabel } from '@/lib/labels'
import { cn } from '@/lib/utils'

function intensity(count: number) {
  if (count === 0) return 'bg-muted'
  if (count === 1) return 'bg-primary/30'
  if (count <= 3) return 'bg-primary/60'
  return 'bg-primary'
}

function Heatmap({ activity }: { activity: Activity }) {
  const firstDay = new Date(`${activity.days[0]?.date ?? '1970-01-01'}T00:00:00Z`)
  const offset = (firstDay.getUTCDay() + 6) % 7
  return (
    <div className="space-y-2">
      <div className="overflow-x-auto pb-1">
        <div className="grid grid-flow-col grid-rows-7 gap-1" role="img" aria-label="Activity over the last weeks">
          {Array.from({ length: offset }, (_, index) => (
            <span key={`pad-${index}`} className="size-3" />
          ))}
          {activity.days.map((day) => (
            <span
              key={day.date}
              title={`${day.date}: ${day.count} ${day.count === 1 ? 'action' : 'actions'}`}
              className={cn('size-3 rounded-[3px]', intensity(day.count))}
            />
          ))}
        </div>
      </div>
      <p className="text-xs text-muted-foreground">
        Current streak {activity.current_streak} {activity.current_streak === 1 ? 'day' : 'days'} · longest{' '}
        {activity.longest_streak}
      </p>
    </div>
  )
}

function GoalRow({ goal }: { goal: Goal }) {
  const queryClient = useQueryClient()
  const remove = useMutation({
    mutationFn: () => deleteGoal(goal.id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: goalKeys.all }),
  })
  const percent = Math.min(100, Math.round((goal.progress / goal.weekly_target) * 100))

  return (
    <Card className="py-0">
      <CardContent className="space-y-2 p-4">
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-sm font-semibold">{formatLabel(goal.metric)}</h3>
          <div className="flex items-center gap-2">
            <span className="text-sm text-muted-foreground">
              {goal.progress} of {goal.weekly_target} this week
            </span>
            <Button
              variant="ghost"
              size="icon-sm"
              aria-label={`Remove ${formatLabel(goal.metric)} goal`}
              onClick={() => remove.mutate()}
              disabled={remove.isPending}
            >
              <Trash2Icon className="size-4" />
            </Button>
          </div>
        </div>
        <div
          role="progressbar"
          aria-label={`${formatLabel(goal.metric)} progress`}
          aria-valuemin={0}
          aria-valuemax={goal.weekly_target}
          aria-valuenow={Math.min(goal.progress, goal.weekly_target)}
          className="h-2 overflow-hidden rounded-full bg-muted"
        >
          <div className="h-full bg-primary transition-[width] duration-200" style={{ width: `${percent}%` }} />
        </div>
      </CardContent>
    </Card>
  )
}

export function GoalsPage() {
  const queryClient = useQueryClient()
  const [metric, setMetric] = useState<GoalMetric>('applications')
  const [target, setTarget] = useState('5')
  const goals = useQuery({ queryKey: goalKeys.all, queryFn: listGoals })
  const activity = useQuery({ queryKey: goalKeys.activity, queryFn: () => getActivity() })
  const save = useMutation({
    mutationFn: () => setGoal(metric, Number(target)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: goalKeys.all }),
  })

  function submit(event: FormEvent) {
    event.preventDefault()
    save.mutate()
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Goals</h2>
        <p className="text-sm text-muted-foreground">Weekly targets, counted from Monday.</p>
      </div>

      <form onSubmit={submit} className="flex flex-wrap items-end gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="goal-metric">Track</Label>
          <NativeSelect id="goal-metric" value={metric} onChange={(event) => setMetric(event.target.value as GoalMetric)}>
            {GOAL_METRICS.map((value) => (
              <option key={value} value={value}>
                {formatLabel(value)}
              </option>
            ))}
          </NativeSelect>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="goal-target">Per week</Label>
          <Input
            id="goal-target"
            type="number"
            min="1"
            max="1000"
            className="w-28"
            value={target}
            onChange={(event) => setTarget(event.target.value)}
          />
        </div>
        <Button type="submit" disabled={save.isPending || target === ''}>
          Set goal
        </Button>
      </form>

      {goals.isLoading ? (
        <LoadingRows rows={2} label="Loading goals" />
      ) : goals.isError ? (
        <EmptyState
          compact
          icon={AlertTriangleIcon}
          title="Couldn't load goals"
          description="Something went wrong while fetching your goals."
          action={<Button variant="outline" onClick={() => goals.refetch()}>Try again</Button>}
        />
      ) : goals.data && goals.data.length > 0 ? (
        <div className="space-y-2">
          {goals.data.map((goal) => (
            <GoalRow key={goal.id} goal={goal} />
          ))}
        </div>
      ) : (
        <p className="text-sm text-muted-foreground">No goals yet. Pick something to track above.</p>
      )}

      <section aria-labelledby="activity-heading" className="space-y-2">
        <h3 id="activity-heading" className="text-sm font-semibold">
          Activity
        </h3>
        {activity.data ? <Heatmap activity={activity.data} /> : <LoadingRows rows={1} label="Loading activity" />}
      </section>
    </div>
  )
}
