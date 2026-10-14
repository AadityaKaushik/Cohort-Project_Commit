import { useContext, useEffect, useRef, type ReactNode } from 'react'
import { UNSAFE_DataRouterContext, useBlocker } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'

function LeaveDialog({ isDirty }: { isDirty: () => boolean }) {
  const blocker = useBlocker(() => isDirty())

  return (
    <Dialog open={blocker.state === 'blocked'} onOpenChange={(open) => !open && blocker.reset?.()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Leave without saving?</DialogTitle>
          <DialogDescription>You have changes that have not been saved. They will be lost if you leave this page.</DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={() => blocker.reset?.()}>
            Stay on page
          </Button>
          <Button variant="destructive" onClick={() => blocker.proceed?.()}>
            Leave
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function RouteBlocker({ isDirty }: { isDirty: () => boolean }) {
  const dataRouter = useContext(UNSAFE_DataRouterContext)
  return dataRouter ? <LeaveDialog isDirty={isDirty} /> : null
}

export function useUnsavedChanges(dirty: boolean): { guard: ReactNode; markSaved: () => void } {
  const saved = useRef(false)
  const dirtyRef = useRef(dirty)

  useEffect(() => {
    dirtyRef.current = dirty
  })

  useEffect(() => {
    if (!dirty) {
      return
    }
    function warn(event: BeforeUnloadEvent) {
      if (dirtyRef.current && !saved.current) {
        event.preventDefault()
      }
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  return {
    guard: <RouteBlocker isDirty={() => dirtyRef.current && !saved.current} />,
    markSaved: () => {
      saved.current = true
    },
  }
}
