import { AppLayout } from '../layouts/AppLayout'
import { CheckInForm } from '../features/mood/CheckInForm'
import { MoodHistory } from '../features/mood/MoodHistory'
import { ConsentGate } from '../features/consent/ConsentGate'
import { AssessmentPanel } from '../features/assessment/AssessmentPanel'
import { RecommendationFeed } from '../features/recommendation/RecommendationFeed'
import { ExerciseLibrary } from '../features/exercise/ExerciseLibrary'

export function DashboardPage() {
  return (
    <AppLayout>
      <ConsentGate>
        <CheckInForm />
        <MoodHistory />
        <RecommendationFeed />
        <ExerciseLibrary />
        <AssessmentPanel />
      </ConsentGate>
    </AppLayout>
  )
}
