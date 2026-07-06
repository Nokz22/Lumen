import { AppLayout } from '../layouts/AppLayout'
import { CheckInForm } from '../features/mood/CheckInForm'
import { MoodHistory } from '../features/mood/MoodHistory'
import { ConsentGate } from '../features/consent/ConsentGate'

export function DashboardPage() {
  return (
    <AppLayout>
      <ConsentGate>
        <CheckInForm />
        <MoodHistory />
      </ConsentGate>
    </AppLayout>
  )
}
