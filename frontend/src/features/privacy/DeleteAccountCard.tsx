import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { useEraseAccount } from './usePrivacy'

const CONFIRMATION_PHRASE = 'DELETE'

/**
 * Deliberately more friction than anything else in the app. Erasure is irreversible and
 * takes the crisis history with it, so it asks the person to type the word rather than
 * offering a button that a mis-tap could reach.
 */
export function DeleteAccountCard({ userId }: { userId: string }) {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { clearSession } = useAuth()
  const [confirmation, setConfirmation] = useState('')
  const eraseAccount = useEraseAccount(userId)
  const canSubmit = confirmation.trim().toUpperCase() === CONFIRMATION_PHRASE

  const submit = () => {
    eraseAccount.mutate(undefined, {
      // The backend already cleared the cookies; this drops the in-memory user so the
      // route guard sends them to /login instead of rendering a dashboard for an
      // account that no longer exists.
      onSuccess: () => {
        clearSession()
        navigate('/login', { replace: true })
      },
    })
  }

  return (
    <section className="flex flex-col gap-3 rounded-2xl border border-red-500/40 bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('privacy.erase.title')}</h2>
      <p className="text-sm text-[var(--color-text-muted)]">{t('privacy.erase.description')}</p>
      <label htmlFor="erase-confirmation" className="text-sm">
        {t('privacy.erase.confirmationLabel', { phrase: CONFIRMATION_PHRASE })}
      </label>
      <input
        id="erase-confirmation"
        type="text"
        value={confirmation}
        autoComplete="off"
        onChange={(event) => setConfirmation(event.target.value)}
        className="rounded-lg border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2"
      />
      <div>
        <button
          type="button"
          disabled={!canSubmit || eraseAccount.isPending}
          onClick={submit}
          className="rounded-full bg-red-600 px-5 py-2.5 text-white disabled:opacity-40"
        >
          {eraseAccount.isPending ? t('privacy.erase.pending') : t('privacy.erase.submit')}
        </button>
      </div>
      {eraseAccount.isError && (
        <p role="alert" className="text-sm text-red-500">
          {t('privacy.erase.error')}
        </p>
      )}
    </section>
  )
}
