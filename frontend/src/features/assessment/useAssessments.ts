import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  acknowledgeRiskEvent,
  fetchAssessmentHistory,
  submitAssessment,
} from '../../api/assessments'
import type { AssessmentType } from '../../types/assessment'

const assessmentHistoryKey = (userId: string) => ['assessments', userId]

export function useAssessmentHistory(userId: string) {
  return useQuery({
    queryKey: assessmentHistoryKey(userId),
    queryFn: () => fetchAssessmentHistory(userId),
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
