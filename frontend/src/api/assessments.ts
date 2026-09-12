import { apiFetch } from './client'
import { pageQueryString, type Page, type PageParams } from '../types/page'
import type {
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

export function fetchAssessmentHistory(
  userId: string,
  params?: PageParams,
): Promise<Page<AssessmentSummary>> {
  return apiFetch<Page<AssessmentSummary>>(
    `/api/v1/users/${userId}/assessments${pageQueryString(params)}`,
  )
}
