import { apiFetch } from './client'
import type {
  AcknowledgeResponse,
  AssessmentSubmissionResult,
  AssessmentSummary,
  AssessmentType,
} from '../types/assessment'

export function submitAssessment(
  userId: string,
  assessmentType: AssessmentType,
  responses: number[],
): Promise<AssessmentSubmissionResult> {
  return apiFetch<AssessmentSubmissionResult>(
    `/api/v1/users/${userId}/assessments/${assessmentType}`,
    {
      method: 'POST',
      body: JSON.stringify({ responses }),
    },
  )
}

export function fetchAssessmentHistory(userId: string): Promise<AssessmentSummary[]> {
  return apiFetch<AssessmentSummary[]>(`/api/v1/users/${userId}/assessments`)
}

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
