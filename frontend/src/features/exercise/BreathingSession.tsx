import { useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { Exercise } from '../../types/exercise'

type PhaseType = 'INHALE' | 'HOLD_AFTER_INHALE' | 'EXHALE' | 'HOLD_AFTER_EXHALE'

interface Phase {
  type: PhaseType
  seconds: number
}

const SMALL_SCALE = 1
const LARGE_SCALE = 1.6

function scaleForPhase(type: PhaseType): number {
  return type === 'INHALE' || type === 'HOLD_AFTER_INHALE' ? LARGE_SCALE : SMALL_SCALE
}

function buildPhases(exercise: Exercise): Phase[] {
  const phases: Phase[] = []
  if (exercise.inhaleSeconds) phases.push({ type: 'INHALE', seconds: exercise.inhaleSeconds })
  if (exercise.holdAfterInhaleSeconds) {
    phases.push({ type: 'HOLD_AFTER_INHALE', seconds: exercise.holdAfterInhaleSeconds })
  }
  if (exercise.exhaleSeconds) phases.push({ type: 'EXHALE', seconds: exercise.exhaleSeconds })
  if (exercise.holdAfterExhaleSeconds) {
    phases.push({ type: 'HOLD_AFTER_EXHALE', seconds: exercise.holdAfterExhaleSeconds })
  }
  return phases
}

export function BreathingSession({
  exercise,
  onFinish,
  onClose,
}: {
  exercise: Exercise
  onFinish: () => void
  onClose: () => void
}) {
  const { t } = useTranslation()
  const phases = useMemo(() => buildPhases(exercise), [exercise])
  const cycleSeconds = useMemo(() => phases.reduce((sum, phase) => sum + phase.seconds, 0), [phases])
  const totalCycles = useMemo(
    () => Math.max(1, Math.round((exercise.durationMinutes * 60) / cycleSeconds)),
    [exercise.durationMinutes, cycleSeconds],
  )

  const [cycle, setCycle] = useState(1)
  const [phaseIndex, setPhaseIndex] = useState(0)
  const [done, setDone] = useState(false)

  const currentPhase = phases[phaseIndex]

  useEffect(() => {
    if (done || phases.length === 0) return

    const timer = window.setTimeout(() => {
      if (phaseIndex + 1 < phases.length) {
        setPhaseIndex(phaseIndex + 1)
      } else if (cycle < totalCycles) {
        setCycle(cycle + 1)
        setPhaseIndex(0)
      } else {
        setDone(true)
      }
    }, currentPhase.seconds * 1000)

    return () => window.clearTimeout(timer)
  }, [phaseIndex, cycle, totalCycles, phases, currentPhase, done])

  const hasFinishedRef = useRef(false)
  useEffect(() => {
    if (done && !hasFinishedRef.current) {
      hasFinishedRef.current = true
      onFinish()
    }
  }, [done, onFinish])

  if (phases.length === 0) {
    return null
  }

  const scale = done ? SMALL_SCALE : scaleForPhase(currentPhase.type)

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

      <div className="flex h-56 w-56 items-center justify-center">
        <div
          className="flex h-24 w-24 items-center justify-center rounded-full bg-[var(--color-accent)] text-center text-sm text-[var(--color-accent-contrast)] motion-safe:transition-transform motion-safe:ease-in-out"
          style={{
            transform: `scale(${scale})`,
            transitionDuration: done ? '400ms' : `${currentPhase.seconds}s`,
          }}
        >
          {!done && <span aria-hidden="true">{t(`exercise.session.phase.${currentPhase.type}`)}</span>}
        </div>
      </div>

      <p aria-live="polite" className="text-sm text-[var(--color-text-muted)]">
        {done
          ? t('exercise.session.completed')
          : t('exercise.session.progress', {
              phase: t(`exercise.session.phase.${currentPhase.type}`),
              cycle,
              totalCycles,
            })}
      </p>
    </div>
  )
}
