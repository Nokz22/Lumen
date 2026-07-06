import { apiFetch } from './client'
import type { MoodCheckInRequest, MoodCheckInResponse } from '../types/mood'

export function submitMoodCheckIn(
  userId: string,
  payload: MoodCheckInRequest,
): Promise<MoodCheckInResponse> {
  return apiFetch<MoodCheckInResponse>(`/api/v1/users/${userId}/mood-check-ins`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function fetchMoodHistory(userId: string): Promise<MoodCheckInResponse[]> {
  return apiFetch<MoodCheckInResponse[]>(`/api/v1/users/${userId}/mood-check-ins`)
}
