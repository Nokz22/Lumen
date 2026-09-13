import { apiFetch } from './client'
import { pageQueryString, type Page, type PageParams } from '../types/page'
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

/**
 * The endpoint returns newest-first, because a chat opens at the bottom and the first page
 * a client needs is the most recent one. The transcript renders oldest-first, so the page
 * is reversed here rather than leaving every caller to remember.
 */
export async function fetchConversationHistory(
  userId: string,
  params?: PageParams,
): Promise<Page<ConversationMessage>> {
  const page = await apiFetch<Page<ConversationMessage>>(
    `/api/v1/users/${userId}/conversation/messages${pageQueryString(params)}`,
  )
  return { ...page, content: [...page.content].reverse() }
}
