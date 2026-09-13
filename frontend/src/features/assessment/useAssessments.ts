import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAssessmentHistory, submitAssessment } from '../../api/assessments'
import { acknowledgeRiskEvent } from '../../api/riskEvents'
import type { AssessmentType } from '../../types/assessment'

const assessmentHistoryKey = (userId: string) => ['assessments', userId]

/** Instruments are monthly, so a page of twelve is a year of them. */
const ASSESSMENT_PAGE_SIZE = 12

export function useAssessmentHistory(userId: string) {
  return useQuery({
    queryKey: assessmentHistoryKey(userId),
    queryFn: () => fetchAssessmentHistory(userId, { size: ASSESSMENT_PAGE_SIZE }),
  })
}

export function useSubmitAssessment(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({
      assessmentType,
      responses,
    }: {
      assessmentType: AssessmentType
      responses: number[]
    }) => submitAssessment(userId, assessmentType, responses),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assessmentHistoryKey(userId) })
    },
  })
}

export function useAcknowledgeRiskEvent(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (riskEventId: string) => acknowledgeRiskEvent(userId, riskEventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: assessmentHistoryKey(userId) })
    },
  })
}
