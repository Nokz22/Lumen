import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { useCompleteExercise, useExercises } from '../exercise/useExercises'
import { useRecommendationHistory, usePrependRecommendation } from './useRecommendations'
import { useRecommendationSocket } from './useRecommendationSocket'
import type { RecommendationSummary } from '../../types/recommendation'

/** Same reasoning as MoodHistory: suggestions accumulate, the request should not. */
const VISIBLE_RECOMMENDATIONS = 5

export function RecommendationFeed() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { data, isLoading, isError } = useRecommendationHistory(user!.id, VISIBLE_RECOMMENDATIONS)
  const { data: exercises } = useExercises()
  const completeExercise = useCompleteExercise(user!.id)
  const prependRecommendation = usePrependRecommendation(user!.id, VISIBLE_RECOMMENDATIONS)

  useRecommendationSocket(prependRecommendation)

  function exerciseName(recommendation: RecommendationSummary): string {
    return (
      exercises?.find((exercise) => exercise.id === recommendation.exerciseId)?.name ??
      recommendation.exerciseId
    )
  }

  return (
    <section
      aria-live="polite"
      className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
    >
      <h2 className="text-lg font-medium">{t('recommendation.feed.title')}</h2>

      {isLoading && <p aria-live="polite">{t('recommendation.feed.loading')}</p>}

      {isError && (
        <p role="alert" className="text-red-500">
          {t('recommendation.feed.error')}
        </p>
      )}

      {data && data.totalElements === 0 && <p>{t('recommendation.feed.empty')}</p>}

      {data && data.content.length > 0 && (
        <ul role="list" className="flex flex-col gap-3">
          {data.content.map((recommendation) => (
            <li
              key={recommendation.id}
              className="flex flex-col gap-1 rounded-xl border border-[var(--color-border)] px-4 py-3 text-sm"
            >
              <span className="font-medium">{exerciseName(recommendation)}</span>
              <span className="text-[var(--color-text-muted)]">{recommendation.reason}</span>
              <button
                type="button"
                onClick={() =>
                  completeExercise.mutate({
                    exerciseId: recommendation.exerciseId,
                    recommendationId: recommendation.id,
                  })
                }
                disabled={completeExercise.isPending}
                className="mt-2 self-start rounded-full border border-[var(--color-border)] px-4 py-1.5 text-xs disabled:opacity-60"
              >
                {t('exercise.markDone')}
              </button>
            </li>
          ))}
        </ul>
      )}

      {data && data.totalElements > data.content.length && (
        <p className="text-sm text-[var(--color-text-muted)]">
          {t('recommendation.feed.showingRecent', {
            shown: data.content.length,
            total: data.totalElements,
          })}
        </p>
      )}
    </section>
  )
}
