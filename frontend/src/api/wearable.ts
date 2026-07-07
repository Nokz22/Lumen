import { apiFetch } from './client'
import type { CorrelationInsight, WearableReading } from '../types/wearable'

export function simulateWearableReadings(userId: string, days: number): Promise<WearableReading[]> {
  return apiFetch<WearableReading[]>(`/api/v1/users/${userId}/wearable-readings/simulate`, {
    method: 'POST',
    body: JSON.stringify({ days }),
  })
}

export function fetchWearableInsights(userId: string): Promise<CorrelationInsight[]> {
  return apiFetch<CorrelationInsight[]>(`/api/v1/users/${userId}/wearable-insights`)
}
