import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import { ToastProvider } from '@/hooks/use-toast'
import { UNDO_WINDOW_MS, usePendingDeletes, useUndoableDelete } from '@/hooks/use-undo-delete'

const send = vi.fn()
const onSettled = vi.fn()

function Harness() {
  const remove = useUndoableDelete()
  const hidden = usePendingDeletes()
  return (
    <>
      <button onClick={() => remove({ id: 'a1', title: 'Application deleted', send, onSettled, errorTitle: 'Could not delete' })}>delete</button>
      <p data-testid="hidden">{[...hidden].join(',')}</p>
    </>
  )
}

function renderHarness() {
  return render(
    <ToastProvider>
      <Harness />
    </ToastProvider>,
  )
}

describe('useUndoableDelete', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    send.mockReset().mockResolvedValue(undefined)
    onSettled.mockReset()
  })
  afterEach(() => vi.useRealTimers())

  it('sends the delete after the undo window', async () => {
    renderHarness()
    fireEvent.click(screen.getByText('delete'))
    expect(screen.getByTestId('hidden')).toHaveTextContent('a1')

    await act(async () => {
      vi.advanceTimersByTime(UNDO_WINDOW_MS - 1)
    })
    expect(send).not.toHaveBeenCalled()
    await act(async () => {
      vi.advanceTimersByTime(1)
    })
    expect(send).toHaveBeenCalledTimes(1)
    expect(onSettled).toHaveBeenCalled()
    expect(screen.getByTestId('hidden')).toBeEmptyDOMElement()
  })

  it('never sends the delete when undone', async () => {
    renderHarness()
    fireEvent.click(screen.getByText('delete'))
    fireEvent.click(screen.getByRole('button', { name: 'Undo' }))

    await act(async () => {
      vi.advanceTimersByTime(UNDO_WINDOW_MS * 2)
    })
    expect(send).not.toHaveBeenCalled()
    expect(screen.getByTestId('hidden')).toBeEmptyDOMElement()
  })
})
