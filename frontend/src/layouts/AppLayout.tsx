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
      {/* Wraps rather than compressing: a third nav item squeezed the tagline into three
          lines at this max width, and it only gets worse on a narrow phone. */}
      <header className="flex flex-wrap items-center justify-between gap-x-4 gap-y-3">
        <div className="min-w-0">
          <h1 className="text-2xl font-semibold">{t('app.title')}</h1>
          <p className="text-sm text-[var(--color-text-muted)]">{t('app.tagline')}</p>
        </div>
        <div className="flex flex-wrap items-center gap-x-3 gap-y-2">
          {user && (
            <nav className="flex items-center gap-2">
              <NavLink to="/" end className={navLinkClassName}>
                {t('nav.dashboard')}
              </NavLink>
              <NavLink to="/companion" className={navLinkClassName}>
                {t('nav.companion')}
              </NavLink>
              <NavLink to="/privacy" className={navLinkClassName}>
                {t('nav.privacy')}
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
