import { apiFetch } from './client'
import type { ConversationMessage, ConversationSubmissionResult } from '../types/companion'

export function sendMessage(
  userId: string,
  content: string,
): Promise<ConversationSubmissionResult> {
  return apiFetch<ConversationSubmissionResult>(`/api/v1/users/${userId}/conversation/messages`, {
    method: 'POST',
    body: JSON.stringify({ content }),
  })
}

export function fetchConversationHistory(userId: string): Promise<ConversationMessage[]> {
  return apiFetch<ConversationMessage[]>(`/api/v1/users/${userId}/conversation/messages`)
}
