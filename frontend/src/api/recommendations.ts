import { apiFetch } from './client'
import type { RecommendationSummary } from '../types/recommendation'

export function fetchRecommendationHistory(userId: string): Promise<RecommendationSummary[]> {
  return apiFetch<RecommendationSummary[]>(`/api/v1/users/${userId}/recommendations`)
}
