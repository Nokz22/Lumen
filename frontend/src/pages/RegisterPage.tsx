import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { ApiError } from '../api/client'

function isAtLeastEighteen(dateOfBirth: string): boolean {
  if (!dateOfBirth) {
    return false
  }
  const birth = new Date(dateOfBirth)
  const eighteenYearsAgo = new Date()
  eighteenYearsAgo.setFullYear(eighteenYearsAgo.getFullYear() - 18)
  return birth <= eighteenYearsAgo
}

export function RegisterPage() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const { register } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [region, setRegion] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [error, setError] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError(false)

    if (!isAtLeastEighteen(dateOfBirth)) {
      setError(true)
      return
    }

    setIsSubmitting(true)
    try {
      await register({
        email,
        password,
        displayName,
        region,
        dateOfBirth,
        locale: i18n.resolvedLanguage ?? 'en',
      })
      navigate('/')
    } catch (err) {
      if (err instanceof ApiError) {
        setError(true)
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="mx-auto flex min-h-svh max-w-sm flex-col justify-center gap-6 px-4 py-8">
      <h1 className="text-2xl font-semibold">{t('auth.register.title')}</h1>
      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
      >
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.register.displayNameLabel')}
          <input
            required
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.register.emailLabel')}
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.register.passwordLabel')}
          <input
            type="password"
            required
            minLength={10}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.register.regionLabel')}
          <input
            required
            placeholder="PT"
            value={region}
            onChange={(event) => setRegion(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.register.dateOfBirthLabel')}
          <input
            type="date"
            required
            value={dateOfBirth}
            onChange={(event) => setDateOfBirth(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
          <span className="text-xs text-[var(--color-text-muted)]">{t('auth.register.ageNotice')}</span>
        </label>
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('auth.register.submit')}
        </button>
        {error && (
          <p role="alert" className="text-red-500">
            {t('auth.register.error')}
          </p>
        )}
      </form>
      <p className="text-center text-sm text-[var(--color-text-muted)]">
        {t('auth.register.haveAccount')}{' '}
        <Link to="/login" className="text-[var(--color-accent)]">
          {t('auth.register.loginLink')}
        </Link>
      </p>
    </div>
  )
}
