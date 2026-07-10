export type ExerciseCategory =
  'BREATHING' | 'WALKING' | 'STRETCHING' | 'SLEEP_HYGIENE' | 'BEHAVIORAL_ACTIVATION' | 'GROUNDING'

export type ExerciseIntensity = 'LOW' | 'MEDIUM' | 'HIGH'

export interface Exercise {
  id: string
  category: ExerciseCategory
  name: string
  durationMinutes: number
  intensity: ExerciseIntensity
  rationale: string
  inhaleSeconds: number | null
  holdAfterInhaleSeconds: number | null
  exhaleSeconds: number | null
  holdAfterExhaleSeconds: number | null
}

export interface ExerciseCompletion {
  id: string
  exerciseId: string
  recommendationId: string | null
  completedAt: string
}

export interface CompleteExerciseRequest {
  exerciseId: string
  recommendationId?: string | null
}
