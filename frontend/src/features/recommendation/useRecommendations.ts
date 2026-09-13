import { useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchRecommendationHistory } from '../../api/recommendations'
import type { RecommendationNotification, RecommendationSummary } from '../../types/recommendation'
import type { Page } from '../../types/page'

const recommendationHistoryKey = (userId: string, size: number) => ['recommendations', userId, size]

export function useRecommendationHistory(userId: string, size: number) {
  return useQuery({
    queryKey: recommendationHistoryKey(userId, size),
    queryFn: () => fetchRecommendationHistory(userId, { size }),
  })
}

/**
 * Prepends a live-pushed notification to the cached first page without a refetch.
 *
 * <p>It has to keep the page a page: dropping the last entry so the length still matches
 * the requested size, and raising the total so the "5 of 112" line stays honest. Pushing
 * onto the front without trimming would grow the rendered list with every message, which
 * is the unbounded dashboard this change exists to remove.
 */
export function usePrependRecommendation(userId: string, size: number) {
  const queryClient = useQueryClient()

  return (notification: RecommendationNotification) => {
    queryClient.setQueryData<Page<RecommendationSummary>>(
      recommendationHistoryKey(userId, size),
      (current) => {
        const entry: RecommendationSummary = {
          id: notification.recommendationId,
          exerciseId: notification.exerciseId,
          reason: notification.reason,
          createdAt: new Date().toISOString(),
        }
        if (!current) {
          return {
            content: [entry],
            page: 0,
            size,
            totalElements: 1,
            totalPages: 1,
            hasNext: false,
          }
        }
        const totalElements = current.totalElements + 1
        return {
          ...current,
          content: [entry, ...current.content].slice(0, current.size),
          totalElements,
          totalPages: Math.ceil(totalElements / current.size),
          hasNext: current.size < totalElements,
        }
      },
    )
  }
}
