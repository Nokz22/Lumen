import { useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchRecommendationHistory } from '../../api/recommendations'
import type { RecommendationNotification, RecommendationSummary } from '../../types/recommendation'

const recommendationHistoryKey = (userId: string) => ['recommendations', userId]

export function useRecommendationHistory(userId: string) {
  return useQuery({
    queryKey: recommendationHistoryKey(userId),
    queryFn: () => fetchRecommendationHistory(userId),
  })
}

/** Prepends a live-pushed notification to the cached history without a refetch. */
export function usePrependRecommendation(userId: string) {
  const queryClient = useQueryClient()

  return (notification: RecommendationNotification) => {
    queryClient.setQueryData<RecommendationSummary[]>(
      recommendationHistoryKey(userId),
      (current) => {
        const entry: RecommendationSummary = {
          id: notification.recommendationId,
          exerciseId: notification.exerciseId,
          reason: notification.reason,
          createdAt: new Date().toISOString(),
        }
        return current ? [entry, ...current] : [entry]
      },
    )
  }
}
