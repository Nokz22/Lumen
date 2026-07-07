import { apiFetch } from './client'
import type { CompleteExerciseRequest, Exercise, ExerciseCompletion } from '../types/exercise'

export function fetchExercises(): Promise<Exercise[]> {
  return apiFetch<Exercise[]>('/api/v1/exercises')
}

export function completeExercise(
  userId: string,
  payload: CompleteExerciseRequest,
): Promise<ExerciseCompletion> {
  return apiFetch<ExerciseCompletion>(`/api/v1/users/${userId}/exercise-completions`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}
