import { apiFetch } from './client'
import { pageQueryString, type Page, type PageParams } from '../types/page'
import type { RecommendationSummary } from '../types/recommendation'

export function fetchRecommendationHistory(
  userId: string,
  params?: PageParams,
): Promise<Page<RecommendationSummary>> {
  return apiFetch<Page<RecommendationSummary>>(
    `/api/v1/users/${userId}/recommendations${pageQueryString(params)}`,
  )
}
