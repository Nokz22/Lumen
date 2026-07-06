import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { ApiError } from '../api/client'

export function LoginPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setError(false)
    setIsSubmitting(true)
    try {
      await login({ email, password })
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
    <div className="mx-auto flex min-h-svh max-w-sm flex-col justify-center gap-6 px-4">
      <h1 className="text-2xl font-semibold">{t('auth.login.title')}</h1>
      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6"
      >
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.login.emailLabel')}
          <input
            type="email"
            required
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          {t('auth.login.passwordLabel')}
          <input
            type="password"
            required
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
          />
        </label>
        <button
          type="submit"
          disabled={isSubmitting}
          className="rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('auth.login.submit')}
        </button>
        {error && (
          <p role="alert" className="text-red-500">
            {t('auth.login.error')}
          </p>
        )}
      </form>
      <p className="text-center text-sm text-[var(--color-text-muted)]">
        {t('auth.login.noAccount')}{' '}
        <Link to="/register" className="text-[var(--color-accent)]">
          {t('auth.login.registerLink')}
        </Link>
      </p>
    </div>
  )
}
