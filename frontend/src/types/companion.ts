import type { CrisisResource } from './assessment'

export type ConversationRole = 'USER' | 'ASSISTANT'

export interface ConversationMessage {
  id: string
  role: ConversationRole
  content: string
  createdAt: string
}

export interface ConversationCrisisResult {
  riskEventId: string
  resources: CrisisResource[]
}

export interface ConversationProcessingResult {
  userMessageId: string
}

export type ConversationSubmissionResult = ConversationCrisisResult | ConversationProcessingResult

/** The two result shapes never share a field, so presence of riskEventId is enough. */
export function isConversationCrisisResult(
  result: ConversationSubmissionResult,
): result is ConversationCrisisResult {
  return 'riskEventId' in result
}

export type CompanionStreamMessageType = 'CHUNK' | 'COMPLETE' | 'ERROR'

export interface CompanionStreamMessage {
  type: CompanionStreamMessageType
  chunk: string | null
  messageId: string | null
  errorMessage: string | null
}
