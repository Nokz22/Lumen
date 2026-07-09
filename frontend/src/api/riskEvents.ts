import { apiFetch } from './client'
import type { AcknowledgeResponse } from '../types/assessment'

/** Shared between assessment- and chat-triggered crises — same endpoint either way. */
export function acknowledgeRiskEvent(
  userId: string,
  riskEventId: string,
): Promise<AcknowledgeResponse> {
  return apiFetch<AcknowledgeResponse>(
    `/api/v1/users/${userId}/risk-events/${riskEventId}/acknowledge`,
    {
      method: 'POST',
    },
  )
}
