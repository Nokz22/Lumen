import { useTranslation } from 'react-i18next'
import { useTheme } from '../contexts/ThemeContext'

/**
 * Inline SVG rather than a ☀/☾ character. The glyph is the button's only visible
 * content, and it renders as a missing-character box on any system without that font —
 * a control that looks broken is worse than no control.
 */
function SunIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
    </svg>
  )
}

function MoonIcon() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="14"
      height="14"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z" />
    </svg>
  )
}

export function ThemeToggle() {
  const { t } = useTranslation()
  const { theme, toggleTheme } = useTheme()

  return (
    <button
      type="button"
      onClick={toggleTheme}
      // The label says what pressing it does, not what the current state is — a
      // screen reader user has no icon to disambiguate it from.
      aria-label={t(theme === 'dark' ? 'theme.switchToLight' : 'theme.switchToDark')}
      className="flex items-center rounded-full border border-[var(--color-border)] px-3 py-1.5 text-[var(--color-text-muted)]"
    >
      {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
    </button>
  )
}
