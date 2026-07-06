import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { useGrantHealthDataConsent, useHealthDataConsent } from './useConsent'

export function ConsentGate({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { data, isLoading } = useHealthDataConsent(user!.id)
  const grantConsent = useGrantHealthDataConsent(user!.id)

  if (isLoading) {
    return <p aria-live="polite">{t('consent.loading')}</p>
  }

  if (!data?.active) {
    return (
      <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
        <h2 className="text-lg font-medium">{t('consent.title')}</h2>
        <p className="text-sm text-[var(--color-text-muted)]">{t('consent.description')}</p>
        <button
          type="button"
          onClick={() => grantConsent.mutate()}
          disabled={grantConsent.isPending}
          className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('consent.grant')}
        </button>
      </section>
    )
  }

  return children
}
