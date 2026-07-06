import { useTranslation } from 'react-i18next'

const LANGUAGES = ['en', 'pt'] as const

export function LanguageSwitcher() {
  const { t, i18n } = useTranslation()

  return (
    <div className="flex gap-1" role="group" aria-label="Language">
      {LANGUAGES.map((lng) => (
        <button
          key={lng}
          type="button"
          onClick={() => i18n.changeLanguage(lng)}
          aria-pressed={i18n.resolvedLanguage === lng}
          className={`rounded-full border px-3 py-1 text-xs ${
            i18n.resolvedLanguage === lng
              ? 'border-[var(--color-accent)] text-[var(--color-accent)]'
              : 'border-[var(--color-border)] text-[var(--color-text-muted)]'
          }`}
        >
          {t(`language.${lng}`)}
        </button>
      ))}
    </div>
  )
}
