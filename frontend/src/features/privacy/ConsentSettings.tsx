import { useTranslation } from 'react-i18next'
import { CONSENT_TYPES, type ConsentType } from '../../api/consents'
import { useConsentStatus, useSetConsent } from './usePrivacy'

function ConsentRow({ userId, consentType }: { userId: string; consentType: ConsentType }) {
  const { t } = useTranslation()
  const { data, isLoading, isError } = useConsentStatus(userId, consentType)
  const setConsent = useSetConsent(userId, consentType)
  const active = data?.active ?? false
  const labelId = `consent-${consentType}`

  return (
    <li className="flex flex-col gap-2 border-t border-[var(--color-border)] py-4 first:border-t-0 sm:flex-row sm:items-center sm:justify-between sm:gap-6">
      <div className="flex flex-col gap-1">
        <span id={labelId} className="font-medium">
          {t(`privacy.consents.${consentType}.title`)}
        </span>
        <span className="text-sm text-[var(--color-text-muted)]">
          {t(`privacy.consents.${consentType}.description`)}
        </span>
      </div>
      {isLoading && (
        <span aria-live="polite" className="text-sm text-[var(--color-text-muted)]">
          {t('privacy.consents.loading')}
        </span>
      )}
      {isError && (
        <span role="alert" className="text-sm text-red-500">
          {t('privacy.consents.error')}
        </span>
      )}
      {!isLoading && !isError && (
        <button
          type="button"
          aria-describedby={labelId}
          aria-pressed={active}
          disabled={setConsent.isPending}
          onClick={() => setConsent.mutate(!active)}
          className={
            active
              ? 'shrink-0 rounded-full border border-[var(--color-border)] px-4 py-2 text-sm text-[var(--color-text-muted)] disabled:opacity-60'
              : 'shrink-0 rounded-full bg-[var(--color-accent)] px-4 py-2 text-sm text-[var(--color-accent-contrast)] disabled:opacity-60'
          }
        >
          {active ? t('privacy.consents.revoke') : t('privacy.consents.grant')}
        </button>
      )}
    </li>
  )
}

export function ConsentSettings({ userId }: { userId: string }) {
  const { t } = useTranslation()

  return (
    <section className="flex flex-col gap-2 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('privacy.consents.title')}</h2>
      <p className="text-sm text-[var(--color-text-muted)]">{t('privacy.consents.description')}</p>
      <ul role="list" className="mt-2 flex flex-col">
        {CONSENT_TYPES.map((consentType) => (
          <ConsentRow key={consentType} userId={userId} consentType={consentType} />
        ))}
      </ul>
    </section>
  )
}
