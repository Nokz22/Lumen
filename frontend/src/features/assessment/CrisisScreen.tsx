import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { useAcknowledgeRiskEvent } from './useAssessments'
import type { AcknowledgeResponse, CrisisResource } from '../../types/assessment'

interface CrisisScreenProps {
  riskEventId: string
  resources: CrisisResource[]
  onAcknowledged: (result: AcknowledgeResponse) => void
}

/**
 * Deliberately calm, no red/warning colors, no exclamation marks in copy — the crisis
 * flow's whole point is to de-escalate, not alarm (project-brief.md §6.2/§10).
 */
export function CrisisScreen({ riskEventId, resources, onAcknowledged }: CrisisScreenProps) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const acknowledge = useAcknowledgeRiskEvent(user!.id)

  function handleAcknowledge() {
    acknowledge.mutate(riskEventId, {
      onSuccess: onAcknowledged,
    })
  }

  return (
    <section
      aria-live="polite"
      className="flex flex-col gap-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
    >
      <h2 className="text-lg font-medium">{t('assessment.crisis.title')}</h2>
      <p className="text-sm text-[var(--color-text-muted)]">{t('assessment.crisis.message')}</p>

      <div className="flex flex-col gap-3">
        <h3 className="text-sm font-medium">{t('assessment.crisis.resourcesTitle')}</h3>
        <ul className="flex flex-col gap-3">
          {resources.map((resource) => (
            <li
              key={resource.name}
              className="rounded-xl border border-[var(--color-border)] p-3 text-sm"
            >
              <p className="font-medium">{resource.name}</p>
              <p>{resource.contact}</p>
              <p className="text-[var(--color-text-muted)]">{resource.availability}</p>
            </li>
          ))}
        </ul>
      </div>

      <p className="text-xs text-[var(--color-text-muted)]">{t('assessment.crisis.disclaimer')}</p>

      <button
        type="button"
        onClick={handleAcknowledge}
        disabled={acknowledge.isPending}
        className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
      >
        {t('assessment.crisis.acknowledge')}
      </button>
    </section>
  )
}
