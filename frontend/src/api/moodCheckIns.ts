import { apiFetch } from './client'
import type { MoodCheckInRequest, MoodCheckInResponse } from '../types/mood'
import { pageQueryString, type Page, type PageParams } from '../types/page'

export function submitMoodCheckIn(
  userId: string,
  payload: MoodCheckInRequest,
): Promise<MoodCheckInResponse> {
  return apiFetch<MoodCheckInResponse>(`/api/v1/users/${userId}/mood-check-ins`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchMoodHistory(
  userId: string,
  params?: PageParams,
): Promise<Page<MoodCheckInResponse>> {
  return apiFetch<Page<MoodCheckInResponse>>(
    `/api/v1/users/${userId}/mood-check-ins${pageQueryString(params)}`,
  )
}
