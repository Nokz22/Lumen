import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import {
  useGrantWearableConsent,
  useSimulateWearableReadings,
  useWearableConsent,
  useWearableInsights,
} from './useWearable'

const SIMULATE_DAYS = 14

export function WearablePanel() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { data: consent, isLoading: isConsentLoading } = useWearableConsent(user!.id)
  const grantConsent = useGrantWearableConsent(user!.id)
  const simulate = useSimulateWearableReadings(user!.id)
  const {
    data: insights,
    isLoading: isInsightsLoading,
    isError: isInsightsError,
  } = useWearableInsights(user!.id, consent?.active ?? false)

  if (isConsentLoading) {
    return <p aria-live="polite">{t('wearable.loading')}</p>
  }

  if (!consent?.active) {
    return (
      <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
        <h2 className="text-lg font-medium">{t('wearable.consent.title')}</h2>
        <p className="text-sm text-[var(--color-text-muted)]">
          {t('wearable.consent.description')}
        </p>
        <button
          type="button"
          onClick={() => grantConsent.mutate()}
          disabled={grantConsent.isPending}
          className="self-start rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('wearable.consent.grant')}
        </button>
      </section>
    )
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h2 className="text-lg font-medium">{t('wearable.title')}</h2>
        <button
          type="button"
          onClick={() => simulate.mutate(SIMULATE_DAYS)}
          disabled={simulate.isPending}
          className="rounded-full border border-[var(--color-border)] px-4 py-1.5 text-sm disabled:opacity-60"
        >
          {t('wearable.generateDemoData')}
        </button>
      </div>
      <p className="text-xs text-[var(--color-text-muted)]">{t('wearable.disclaimer')}</p>

      {isInsightsLoading && <p aria-live="polite">{t('wearable.insights.loading')}</p>}

      {isInsightsError && (
        <p role="alert" className="text-red-500">
          {t('wearable.insights.error')}
        </p>
      )}

      {insights && insights.length === 0 && <p>{t('wearable.insights.empty')}</p>}

      {insights && insights.length > 0 && (
        <ul role="list" aria-live="polite" className="flex flex-col gap-3">
          {insights.map((insight) => (
            <li
              key={insight.metric}
              className="rounded-xl border border-[var(--color-border)] px-4 py-3 text-sm"
            >
              {insight.description}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
