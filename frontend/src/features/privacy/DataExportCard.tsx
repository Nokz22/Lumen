import { useTranslation } from 'react-i18next'
import { useDataExport } from './usePrivacy'

export function DataExportCard({ userId }: { userId: string }) {
  const { t } = useTranslation()
  const exportData = useDataExport(userId)

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('privacy.export.title')}</h2>
      <p className="text-sm text-[var(--color-text-muted)]">{t('privacy.export.description')}</p>
      <div>
        <button
          type="button"
          disabled={exportData.isPending}
          onClick={() => exportData.mutate()}
          className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {exportData.isPending ? t('privacy.export.pending') : t('privacy.export.submit')}
        </button>
      </div>
      {exportData.isError && (
        <p role="alert" className="text-sm text-red-500">
          {t('privacy.export.error')}
        </p>
      )}
      {exportData.isSuccess && (
        <p aria-live="polite" className="text-sm text-[var(--color-text-muted)]">
          {t('privacy.export.success')}
        </p>
      )}
    </section>
  )
}
