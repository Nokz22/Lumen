import { useTranslation } from 'react-i18next'
import { useMoodHistory } from './useMoodCheckIns'
import { DEMO_USER_ID } from '../../config/demoUser'

export function MoodHistory() {
  const { t } = useTranslation()
  const { data, isLoading, isError } = useMoodHistory(DEMO_USER_ID)

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('dashboard.title')}</h2>

      {isLoading && <p aria-live="polite">{t('dashboard.loading')}</p>}

      {isError && (
        <p role="alert" className="text-red-500">
          {t('checkin.error')}
        </p>
      )}

      {data && data.length === 0 && <p>{t('dashboard.empty')}</p>}

      {data && data.length > 0 && (
        <ul role="list" aria-live="polite" className="flex flex-col gap-3">
          {data.map((entry) => (
            <li
              key={entry.id}
              className="flex flex-col gap-1 rounded-xl border border-[var(--color-border)] px-4 py-3 text-sm"
            >
              <span className="font-medium">{entry.checkInDate}</span>
              <span>
                {t('dashboard.loggedFeeling', {
                  emotion: t(`checkin.emotion.${entry.emotion}`).toLowerCase(),
                })}
              </span>
              <span className="text-[var(--color-text-muted)]">
                {t('dashboard.energy', { level: entry.energyLevel })} ·{' '}
                {t('dashboard.sleep', { hours: entry.sleepHours, quality: entry.sleepQuality })}
              </span>
              {entry.note && <span className="italic">{entry.note}</span>}
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
