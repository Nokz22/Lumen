import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { ApiError } from '../../api/client'
import type { AssessmentType } from '../../types/assessment'

const ITEM_COUNTS: Record<AssessmentType, number> = { PHQ9: 9, GAD7: 7 }
const SCALE_VALUES = [0, 1, 2, 3]
const INSTRUMENTS: AssessmentType[] = ['PHQ9', 'GAD7']

interface AssessmentFormProps {
  instrument: AssessmentType
  onInstrumentChange: (instrument: AssessmentType) => void
  onSubmit: (responses: number[]) => void
  isSubmitting: boolean
  error: unknown
}

export function AssessmentForm({
  instrument,
  onInstrumentChange,
  onSubmit,
  isSubmitting,
  error,
}: AssessmentFormProps) {
  const { t } = useTranslation()
  const [responses, setResponses] = useState<Array<number | null>>(() =>
    Array(ITEM_COUNTS[instrument]).fill(null),
  )

  function handleInstrumentChange(next: AssessmentType) {
    onInstrumentChange(next)
    setResponses(Array(ITEM_COUNTS[next]).fill(null))
  }

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    if (responses.some((value) => value === null)) {
      return
    }
    onSubmit(responses as number[])
  }

  const isComplete = responses.every((value) => value !== null)
  const isTooSoon = error instanceof ApiError && error.status === 403

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
    >
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-medium">{t('assessment.title')}</h2>
        <div className="flex gap-2">
          {INSTRUMENTS.map((option) => (
            <button
              key={option}
              type="button"
              onClick={() => handleInstrumentChange(option)}
              aria-pressed={instrument === option}
              className={`rounded-full border px-4 py-1.5 text-sm transition-colors ${
                instrument === option
                  ? 'border-[var(--color-accent)] bg-[var(--color-accent)] text-[var(--color-accent-contrast)]'
                  : 'border-[var(--color-border)]'
              }`}
            >
              {t(`assessment.instrument.${option}`)}
            </button>
          ))}
        </div>
      </div>

      <p className="text-sm text-[var(--color-text-muted)]">{t('assessment.instructions')}</p>

      <ol className="flex flex-col gap-4">
        {responses.map((value, index) => {
          const itemLabel = t(`assessment.${instrument.toLowerCase()}.items.${index + 1}`)
          return (
            <li key={`${instrument}-${index}`} className="flex flex-col gap-2">
              <span className="text-sm">{itemLabel}</span>
              <div role="radiogroup" aria-label={itemLabel} className="flex flex-wrap gap-2">
                {SCALE_VALUES.map((scaleValue) => (
                  <button
                    key={scaleValue}
                    type="button"
                    role="radio"
                    aria-checked={value === scaleValue}
                    onClick={() =>
                      setResponses((previous) =>
                        previous.map((current, itemIndex) =>
                          itemIndex === index ? scaleValue : current,
                        ),
                      )
                    }
                    className={`rounded-full border px-3 py-1.5 text-xs transition-colors ${
                      value === scaleValue
                        ? 'border-[var(--color-accent)] bg-[var(--color-accent)] text-[var(--color-accent-contrast)]'
                        : 'border-[var(--color-border)]'
                    }`}
                  >
                    {t(`assessment.scale.${scaleValue}`)}
                  </button>
                ))}
              </div>
            </li>
          )
        })}
      </ol>

      <button
        type="submit"
        disabled={!isComplete || isSubmitting}
        className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
      >
        {t('assessment.submit')}
      </button>

      {isTooSoon && (
        <p role="alert" className="text-sm text-[var(--color-text-muted)]">
          {t('assessment.tooSoon')}
        </p>
      )}
      {error != null && !isTooSoon && (
        <p role="alert" className="text-red-500">
          {t('assessment.error')}
        </p>
      )}
    </form>
  )
}
