import { useTranslation } from 'react-i18next'
import { useMoodHistory } from './useMoodCheckIns'
import { useAuth } from '../../contexts/AuthContext'

/**
 * A fortnight, and only a fortnight is fetched. This used to slice a full history the API
 * had already sent; now the size is a request parameter, so eight weeks of history costs
 * one page of rows over the wire instead of all of them.
 */
const VISIBLE_ENTRIES = 14

export function MoodHistory() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { data, isLoading, isError } = useMoodHistory(user!.id, VISIBLE_ENTRIES)

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('dashboard.title')}</h2>

      {isLoading && <p aria-live="polite">{t('dashboard.loading')}</p>}

      {isError && (
        <p role="alert" className="text-red-500">
          {t('checkin.error')}
        </p>
      )}

      {data && data.totalElements === 0 && <p>{t('dashboard.empty')}</p>}

      {data && data.content.length > 0 && (
        <ul role="list" aria-live="polite" className="flex flex-col gap-3">
          {data.content.map((entry) => (
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

      {data && data.totalElements > data.content.length && (
        <p className="text-sm text-[var(--color-text-muted)]">
          {t('dashboard.showingRecent', { shown: data.content.length, total: data.totalElements })}
        </p>
      )}
    </section>
  )
}
