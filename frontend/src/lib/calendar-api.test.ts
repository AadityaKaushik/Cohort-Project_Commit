import { beforeEach, describe, expect, it, vi } from 'vitest'
import { downloadInterviewCalendar, downloadUpcomingCalendar } from '@/lib/calendar-api'
import { apiFetchBlob } from '@/lib/api-client'
import { saveBlob } from '@/lib/download'

vi.mock('@/lib/api-client', () => ({ apiFetchBlob: vi.fn() }))
vi.mock('@/lib/download', () => ({ saveBlob: vi.fn() }))

describe('calendar downloads', () => {
  beforeEach(() => {
    vi.mocked(apiFetchBlob).mockReset()
    vi.mocked(saveBlob).mockReset()
  })

  it('fetches one interview through the authenticated client and saves it', async () => {
    const blob = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(apiFetchBlob).mockResolvedValue({ blob, filename: 'interview.ics' })

    await downloadInterviewCalendar('abc')

    expect(apiFetchBlob).toHaveBeenCalledWith('/interviews/abc/calendar.ics')
    expect(saveBlob).toHaveBeenCalledWith(blob, 'interview.ics')
  })

  it('falls back to a default file name for the upcoming feed', async () => {
    const blob = new Blob(['BEGIN:VCALENDAR'])
    vi.mocked(apiFetchBlob).mockResolvedValue({ blob, filename: null })

    await downloadUpcomingCalendar()

    expect(apiFetchBlob).toHaveBeenCalledWith('/calendar/upcoming.ics')
    expect(saveBlob).toHaveBeenCalledWith(blob, 'cohort-upcoming.ics')
  })
})
