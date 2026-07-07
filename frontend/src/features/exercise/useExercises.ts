import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { completeExercise, fetchExercises } from '../../api/exercises'
import type { CompleteExerciseRequest } from '../../types/exercise'

export function useExercises() {
  return useQuery({
    queryKey: ['exercises'],
    queryFn: fetchExercises,
  })
}

export function useCompleteExercise(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CompleteExerciseRequest) => completeExercise(userId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['exercise-completions', userId] })
    },
  })
}
