import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchMoodHistory, submitMoodCheckIn } from '../../api/moodCheckIns'
import type { MoodCheckInRequest } from '../../types/mood'

const moodHistoryKey = (userId: string, size: number) => ['mood-check-ins', userId, size]

export function useMoodHistory(userId: string, size: number) {
  return useQuery({
    queryKey: moodHistoryKey(userId, size),
    queryFn: () => fetchMoodHistory(userId, { size }),
  })
}

export function useSubmitMoodCheckIn(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: MoodCheckInRequest) => submitMoodCheckIn(userId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['mood-check-ins', userId] })
    },
  })
}
