import { useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../../contexts/AuthContext'
import { CrisisScreen } from '../crisis/CrisisScreen'
import {
  conversationHistoryKey,
  useAcknowledgeConversationRiskEvent,
  useCompanionConsent,
  useConversationHistory,
  useGrantCompanionConsent,
  useSendMessage,
} from './useCompanion'
import { useCompanionSocket } from './useCompanionSocket'
import { isConversationCrisisResult, type CrisisResource } from '../../types/companion'

type ViewState =
  { kind: 'chat' } | { kind: 'crisis'; riskEventId: string; resources: CrisisResource[] }

export function CompanionChat() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const userId = user!.id
  const queryClient = useQueryClient()

  const { data: consent, isLoading: isConsentLoading } = useCompanionConsent(userId)
  const grantConsent = useGrantCompanionConsent(userId)

  const [view, setView] = useState<ViewState>({ kind: 'chat' })
  const [draft, setDraft] = useState('')
  const [streamingText, setStreamingText] = useState('')
  const [isAwaitingReply, setIsAwaitingReply] = useState(false)
  const [streamError, setStreamError] = useState<string | null>(null)

  const {
    data: history,
    isLoading: isHistoryLoading,
    isError: isHistoryError,
  } = useConversationHistory(userId, consent?.active ?? false)
  const sendMessage = useSendMessage(userId)
  const acknowledge = useAcknowledgeConversationRiskEvent(userId)

  useCompanionSocket((message) => {
    if (message.type === 'CHUNK' && message.chunk) {
      setStreamingText((previous) => previous + message.chunk)
    } else if (message.type === 'COMPLETE') {
      setStreamingText('')
      setIsAwaitingReply(false)
      queryClient.invalidateQueries({ queryKey: conversationHistoryKey(userId) })
    } else if (message.type === 'ERROR') {
      setStreamingText('')
      setIsAwaitingReply(false)
      setStreamError(message.errorMessage)
      queryClient.invalidateQueries({ queryKey: conversationHistoryKey(userId) })
    }
  })

  if (isConsentLoading) {
    return <p aria-live="polite">{t('companion.loading')}</p>
  }

  if (!consent?.active) {
    return (
      <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
        <h2 className="text-lg font-medium">{t('companion.consent.title')}</h2>
        <p className="text-sm text-[var(--color-text-muted)]">
          {t('companion.consent.description')}
        </p>
        <button
          type="button"
          onClick={() => grantConsent.mutate()}
          disabled={grantConsent.isPending}
          className="self-start rounded-full bg-[var(--color-accent)] px-5 py-2.5 text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('companion.consent.grant')}
        </button>
      </section>
    )
  }

  if (view.kind === 'crisis') {
    const riskEventId = view.riskEventId
    return (
      <CrisisScreen
        resources={view.resources}
        isAcknowledging={acknowledge.isPending}
        onAcknowledge={() => {
          acknowledge.mutate(riskEventId, { onSuccess: () => setView({ kind: 'chat' }) })
        }}
      />
    )
  }

  function handleSend() {
    const content = draft.trim()
    if (!content) {
      return
    }
    setDraft('')
    setStreamError(null)
    sendMessage.mutate(content, {
      onSuccess: (result) => {
        if (isConversationCrisisResult(result)) {
          setView({ kind: 'crisis', riskEventId: result.riskEventId, resources: result.resources })
        } else {
          setIsAwaitingReply(true)
        }
      },
    })
  }

  return (
    <section className="flex flex-col gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <h2 className="text-lg font-medium">{t('companion.title')}</h2>
      <p className="text-xs text-[var(--color-text-muted)]">{t('companion.disclaimer')}</p>

      {isHistoryLoading && <p aria-live="polite">{t('companion.history.loading')}</p>}

      {isHistoryError && (
        <p role="alert" className="text-red-500">
          {t('companion.history.error')}
        </p>
      )}

      {history && history.length === 0 && !isAwaitingReply && (
        <p className="text-sm text-[var(--color-text-muted)]">{t('companion.history.empty')}</p>
      )}

      {history && (
        <ul role="list" aria-live="polite" className="flex flex-col gap-3">
          {history.map((message) => (
            <li
              key={message.id}
              className={
                message.role === 'USER'
                  ? 'self-end rounded-xl bg-[var(--color-accent)] px-4 py-2 text-sm text-[var(--color-accent-contrast)]'
                  : 'self-start rounded-xl border border-[var(--color-border)] px-4 py-2 text-sm'
              }
            >
              {message.content}
            </li>
          ))}
          {isAwaitingReply && (
            <li className="self-start rounded-xl border border-[var(--color-border)] px-4 py-2 text-sm">
              {streamingText || t('companion.typing')}
            </li>
          )}
        </ul>
      )}

      {streamError && (
        <p role="alert" className="text-red-500">
          {streamError}
        </p>
      )}

      {sendMessage.isError && (
        <p role="alert" className="text-red-500">
          {t('companion.sendError')}
        </p>
      )}

      <form
        onSubmit={(event) => {
          event.preventDefault()
          handleSend()
        }}
        className="flex gap-2"
      >
        <label htmlFor="companion-message" className="sr-only">
          {t('companion.inputLabel')}
        </label>
        <input
          id="companion-message"
          type="text"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          placeholder={t('companion.inputPlaceholder')}
          className="flex-1 rounded-full border border-[var(--color-border)] px-4 py-2 text-sm"
        />
        <button
          type="submit"
          disabled={sendMessage.isPending || draft.trim().length === 0}
          className="rounded-full bg-[var(--color-accent)] px-5 py-2 text-sm text-[var(--color-accent-contrast)] disabled:opacity-60"
        >
          {t('companion.send')}
        </button>
      </form>
    </section>
  )
}
