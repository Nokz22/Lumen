export type AssessmentType = 'PHQ9' | 'GAD7'

export type AssessmentStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'SCORED'

export type WellbeingBand = 'MINIMAL' | 'MILD' | 'MODERATE' | 'PRONOUNCED' | 'ELEVATED'

export type CrisisResourceType = 'HELPLINE' | 'EMERGENCY_SERVICE'

export interface CrisisResource {
  name: string
  type: CrisisResourceType
  contact: string
  availability: string
}

export interface ScoredAssessmentResult {
  assessmentId: string
  totalScore: number
  wellbeingBand: WellbeingBand
}

export interface CrisisTriggeredResult {
  riskEventId: string
  resources: CrisisResource[]
}

export type AssessmentSubmissionResult = ScoredAssessmentResult | CrisisTriggeredResult

/** The two result shapes never share a field, so presence of riskEventId is enough. */
export function isCrisisResult(
  result: AssessmentSubmissionResult,
): result is CrisisTriggeredResult {
  return 'riskEventId' in result
}

export interface AssessmentSummary {
  id: string
  assessmentType: AssessmentType
  status: AssessmentStatus
  totalScore: number | null
  wellbeingBand: WellbeingBand | null
  createdAt: string
}

export interface AcknowledgeResponse {
  totalScore: number | null
  wellbeingBand: WellbeingBand | null
}
