import { useTranslation } from 'react-i18next'
import type { WellbeingBand } from '../../types/assessment'

interface ResultScreenProps {
  wellbeingBand: WellbeingBand
  onDone: () => void
}

/** Shows the wellbeing band only — never the raw score or a clinical label (ADR-0001). */
export function ResultScreen({ wellbeingBand, onDone }: ResultScreenProps) {
  const { t } = useTranslation()

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('assessment.result.title')}</h2>
      <p className="text-sm">{t(`assessment.result.band.${wellbeingBand}`)}</p>
      <button
        type="button"
        onClick={onDone}
        className="self-start rounded-full border border-[var(--color-border)] px-5 py-2.5 text-sm"
      >
        {t('assessment.result.done')}
      </button>
    </section>
  )
}
