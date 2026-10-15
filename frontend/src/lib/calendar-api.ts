import { apiFetchBlob } from '@/lib/api-client'
import { saveBlob } from '@/lib/download'

export async function downloadInterviewCalendar(interviewId: string) {
  const { blob, filename } = await apiFetchBlob(`/interviews/${interviewId}/calendar.ics`)
  saveBlob(blob, filename ?? 'interview.ics')
}

export async function downloadUpcomingCalendar() {
  const { blob, filename } = await apiFetchBlob('/calendar/upcoming.ics')
  saveBlob(blob, filename ?? 'cohort-upcoming.ics')
}
