import { useTranslation } from 'react-i18next'
import { AppLayout } from '../layouts/AppLayout'
import { useAuth } from '../contexts/AuthContext'
import { ConsentSettings } from '../features/privacy/ConsentSettings'
import { DataExportCard } from '../features/privacy/DataExportCard'
import { DeleteAccountCard } from '../features/privacy/DeleteAccountCard'

export function PrivacyPage() {
  const { t } = useTranslation()
  const { user } = useAuth()

  return (
    <AppLayout>
      <div className="flex flex-col gap-2">
        <h1 className="text-xl font-semibold">{t('privacy.title')}</h1>
        <p className="text-sm text-[var(--color-text-muted)]">{t('privacy.subtitle')}</p>
      </div>
      <ConsentSettings userId={user!.id} />
      <DataExportCard userId={user!.id} />
      <DeleteAccountCard userId={user!.id} />
    </AppLayout>
  )
}
