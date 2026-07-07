import type { ExerciseCategory } from './exercise'

/** Shape pushed over the /user/queue/recommendations WebSocket destination. */
export interface RecommendationNotification {
  recommendationId: string
  exerciseId: string
  exerciseName: string
  category: ExerciseCategory
  durationMinutes: number
  reason: string
}

/** Shape returned by the REST history endpoint (used for the initial page load). */
export interface RecommendationSummary {
  id: string
  exerciseId: string
  reason: string
  createdAt: string
}
