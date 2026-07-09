import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchConversationHistory, sendMessage } from '../../api/companion'
import { fetchCompanionConsent, grantCompanionConsent } from '../../api/companionConsent'
import { acknowledgeRiskEvent } from '../../api/riskEvents'

const companionConsentKey = (userId: string) => ['companion-consent', userId]
export const conversationHistoryKey = (userId: string) => ['conversation-history', userId]

export function useCompanionConsent(userId: string) {
  return useQuery({
    queryKey: companionConsentKey(userId),
    queryFn: () => fetchCompanionConsent(userId),
  })
}

export function useGrantCompanionConsent(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => grantCompanionConsent(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: companionConsentKey(userId) })
    },
  })
}

export function useConversationHistory(userId: string, enabled: boolean) {
  return useQuery({
    queryKey: conversationHistoryKey(userId),
    queryFn: () => fetchConversationHistory(userId),
    enabled,
  })
}

export function useSendMessage(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (content: string) => sendMessage(userId, content),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: conversationHistoryKey(userId) })
    },
  })
}

/** Own invalidation from the assessment feature's equivalent hook: refetches chat
 * history (not assessment history) once a chat-triggered crisis is acknowledged. */
export function useAcknowledgeConversationRiskEvent(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (riskEventId: string) => acknowledgeRiskEvent(userId, riskEventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: conversationHistoryKey(userId) })
    },
  })
}
