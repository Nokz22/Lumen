import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { useCompleteExercise, useExercises } from './useExercises'
import { BreathingSession } from './BreathingSession'
import { GuidedSession } from './GuidedSession'
import type { Exercise } from '../../types/exercise'

function hasGuidedSession(exercise: Exercise): boolean {
  return Boolean(exercise.inhaleSeconds) || exercise.steps.length > 0
}

export function ExerciseLibrary() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { data, isLoading, isError } = useExercises()
  const completeExercise = useCompleteExercise(user!.id)
  const [activeSession, setActiveSession] = useState<Exercise | null>(null)

  if (activeSession) {
    const onFinish = () => completeExercise.mutate({ exerciseId: activeSession.id })
    const onClose = () => setActiveSession(null)
    return activeSession.inhaleSeconds ? (
      <BreathingSession exercise={activeSession} onFinish={onFinish} onClose={onClose} />
    ) : (
      <GuidedSession exercise={activeSession} onFinish={onFinish} onClose={onClose} />
    )
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('exercise.library.title')}</h2>

      {isLoading && <p aria-live="polite">{t('exercise.library.loading')}</p>}

      {isError && (
        <p role="alert" className="text-red-500">
          {t('exercise.library.error')}
        </p>
      )}

      {data && data.length === 0 && <p>{t('exercise.library.empty')}</p>}

      {data && data.length > 0 && (
        <ul role="list" className="flex flex-col gap-3">
          {data.map((exercise) => (
            <li
              key={exercise.id}
              className="flex flex-col gap-1 rounded-xl border border-[var(--color-border)] px-4 py-3 text-sm"
            >
              <span className="font-medium">{exercise.name}</span>
              <span className="text-[var(--color-text-muted)]">
                {t(`exercise.category.${exercise.category}`)} ·{' '}
                {t('exercise.durationMinutes', { minutes: exercise.durationMinutes })}
              </span>
              <span>{exercise.rationale}</span>
              <div className="mt-2 flex gap-2">
                {hasGuidedSession(exercise) && (
                  <button
                    type="button"
                    onClick={() => setActiveSession(exercise)}
                    className="self-start rounded-full bg-[var(--color-accent)] px-4 py-1.5 text-xs text-[var(--color-accent-contrast)]"
                  >
                    {t('exercise.start')}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => completeExercise.mutate({ exerciseId: exercise.id })}
                  disabled={completeExercise.isPending}
                  className="self-start rounded-full border border-[var(--color-border)] px-4 py-1.5 text-xs disabled:opacity-60"
                >
                  {t('exercise.markDone')}
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
