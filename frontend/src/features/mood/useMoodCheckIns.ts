import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMoodHistory, submitMoodCheckIn } from '../../api/moodCheckIns'
import type { MoodCheckInRequest } from '../../types/mood'

const moodHistoryKey = (userId: string) => ['mood-check-ins', userId]

export function useMoodHistory(userId: string) {
  return useQuery({
    queryKey: moodHistoryKey(userId),
    queryFn: () => fetchMoodHistory(userId),
  })
}

export function useSubmitMoodCheckIn(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: MoodCheckInRequest) => submitMoodCheckIn(userId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: moodHistoryKey(userId) })
    },
  })
}
