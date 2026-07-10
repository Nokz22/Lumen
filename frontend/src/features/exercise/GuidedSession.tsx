import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Exercise } from '../../types/exercise'

export function GuidedSession({
  exercise,
  onFinish,
  onClose,
}: {
  exercise: Exercise
  onFinish: () => void
  onClose: () => void
}) {
  const { t } = useTranslation()
  const totalSteps = exercise.steps.length
  const secondsPerStep = useMemo(
    () => Math.max(1, Math.round((exercise.durationMinutes * 60) / totalSteps)),
    [exercise.durationMinutes, totalSteps],
  )

  const [stepIndex, setStepIndex] = useState(0)
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (done || totalSteps === 0) return

    const timer = window.setTimeout(() => {
      if (stepIndex + 1 < totalSteps) {
        setStepIndex(stepIndex + 1)
      } else {
        setDone(true)
      }
    }, secondsPerStep * 1000)

    return () => window.clearTimeout(timer)
  }, [stepIndex, totalSteps, secondsPerStep, done])

  const hasFinishedRef = useRef(false)
  useEffect(() => {
    if (done && !hasFinishedRef.current) {
      hasFinishedRef.current = true
      onFinish()
    }
  }, [done, onFinish])

  if (totalSteps === 0) {
    return null
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={t('exercise.session.title', { name: exercise.name })}
      className="flex flex-col items-center gap-6 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-8"
    >
      <div className="flex w-full items-center justify-between">
        <h3 className="text-lg font-medium">{exercise.name}</h3>
        <button
          type="button"
          onClick={onClose}
          className="rounded-full border border-[var(--color-border)] px-3 py-1 text-xs"
        >
          {t('exercise.session.exit')}
        </button>
      </div>

      {!done && <p className="text-center text-lg">{exercise.steps[stepIndex]}</p>}

      <p aria-live="polite" className="text-sm text-[var(--color-text-muted)]">
        {done
          ? t('exercise.session.completed')
          : t('exercise.session.step', { step: stepIndex + 1, totalSteps })}
      </p>
    </div>
  )
}
