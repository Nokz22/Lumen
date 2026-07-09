import { useTranslation } from 'react-i18next'
import type { CrisisResource } from '../../types/assessment'

interface CrisisScreenProps {
  resources: CrisisResource[]
  onAcknowledge: () => void
  isAcknowledging: boolean
}

/**
 * Shared between assessment- and chat-triggered crises (project-brief.md §6.2/§10)
 * — both use the exact same de-escalating copy. Deliberately calm, no red/warning
 * colors, no exclamation marks. Acknowledgement itself is left to the caller: the
 * two features invalidate different query caches afterwards.
 */
export function CrisisScreen({ resources, onAcknowledge, isAcknowledging }: CrisisScreenProps) {
  const { t } = useTranslation()

  return (
    <section
      aria-live="polite"
      className="flex flex-col gap-5 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
    >
      <h2 className="text-lg font-medium">{t('crisis.title')}</h2>
      <p className="text-sm text-[var(--color-text-muted)]">{t('crisis.message')}</p>

      <div className="flex flex-col gap-3">
        <h3 className="text-sm font-medium">{t('crisis.resourcesTitle')}</h3>
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

      <p className="text-xs text-[var(--color-text-muted)]">{t('crisis.disclaimer')}</p>

      <button
        type="button"
        onClick={onAcknowledge}
        disabled={isAcknowledging}
        className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
      >
        {t('crisis.acknowledge')}
      </button>
    </section>
  )
}
