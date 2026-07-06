import { useState } from 'react'
import { useAuth } from '../../contexts/AuthContext'
import { AssessmentForm } from './AssessmentForm'
import { CrisisScreen } from './CrisisScreen'
import { ResultScreen } from './ResultScreen'
import { useSubmitAssessment } from './useAssessments'
import {
  isCrisisResult,
  type AssessmentType,
  type CrisisResource,
  type WellbeingBand,
} from '../../types/assessment'

type ViewState =
  | { kind: 'form' }
  | { kind: 'crisis'; riskEventId: string; resources: CrisisResource[] }
  | { kind: 'result'; wellbeingBand: WellbeingBand }

export function AssessmentPanel() {
  const { user } = useAuth()
  const [instrument, setInstrument] = useState<AssessmentType>('PHQ9')
  const [view, setView] = useState<ViewState>({ kind: 'form' })
  const submitAssessment = useSubmitAssessment(user!.id)

  function handleSubmit(responses: number[]) {
    submitAssessment.mutate(
      { assessmentType: instrument, responses },
      {
        onSuccess: (result) => {
          if (isCrisisResult(result)) {
            setView({
              kind: 'crisis',
              riskEventId: result.riskEventId,
              resources: result.resources,
            })
          } else {
            setView({ kind: 'result', wellbeingBand: result.wellbeingBand })
          }
        },
      },
    )
  }

  if (view.kind === 'crisis') {
    return (
      <CrisisScreen
        riskEventId={view.riskEventId}
        resources={view.resources}
        onAcknowledged={(result) => {
          if (result.wellbeingBand !== null) {
            setView({ kind: 'result', wellbeingBand: result.wellbeingBand })
          } else {
            setView({ kind: 'form' })
          }
        }}
      />
    )
  }

  if (view.kind === 'result') {
    return (
      <ResultScreen wellbeingBand={view.wellbeingBand} onDone={() => setView({ kind: 'form' })} />
    )
  }

  return (
    <AssessmentForm
      instrument={instrument}
      onInstrumentChange={setInstrument}
      onSubmit={handleSubmit}
      isSubmitting={submitAssessment.isPending}
      error={submitAssessment.error}
    />
  )
}
