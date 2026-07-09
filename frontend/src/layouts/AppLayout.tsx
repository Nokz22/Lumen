import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { NavLink } from 'react-router-dom'
import { LanguageSwitcher } from '../components/LanguageSwitcher'
import { useAuth } from '../contexts/AuthContext'

const navLinkClassName = ({ isActive }: { isActive: boolean }) =>
  isActive
    ? 'rounded-full bg-[var(--color-accent)] px-3 py-1 text-xs text-[var(--color-accent-contrast)]'
    : 'rounded-full border border-[var(--color-border)] px-3 py-1 text-xs text-[var(--color-text-muted)]'

export function AppLayout({ children }: { children: ReactNode }) {
  const { t } = useTranslation()
  const { user, logout } = useAuth()

  return (
    <div className="mx-auto flex min-h-svh max-w-2xl flex-col gap-8 px-4 py-8">
      <header className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">{t('app.title')}</h1>
          <p className="text-sm text-[var(--color-text-muted)]">{t('app.tagline')}</p>
        </div>
        <div className="flex items-center gap-3">
          {user && (
            <nav className="flex items-center gap-2">
              <NavLink to="/" end className={navLinkClassName}>
                {t('nav.dashboard')}
              </NavLink>
              <NavLink to="/companion" className={navLinkClassName}>
                {t('nav.companion')}
              </NavLink>
            </nav>
          )}
          <LanguageSwitcher />
          {user && (
            <button
              type="button"
              onClick={() => logout()}
              className="rounded-full border border-[var(--color-border)] px-3 py-1 text-xs text-[var(--color-text-muted)]"
            >
              {t('auth.logout')}
            </button>
          )}
        </div>
      </header>
      <main className="flex flex-col gap-6">{children}</main>
    </div>
  )
}
