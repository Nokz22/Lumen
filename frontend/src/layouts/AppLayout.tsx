import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { LanguageSwitcher } from '../components/LanguageSwitcher'

export function AppLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation()

  return (
    <div className="mx-auto flex min-h-svh max-w-2xl flex-col gap-8 px-4 py-8">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">{t('app.title')}</h1>
          <p className="text-sm text-[var(--color-text-muted)]">{t('app.tagline')}</p>
        </div>
        <LanguageSwitcher />
      </header>
      <main className="flex flex-col gap-6">{children}</main>
    </div>
  )
}
