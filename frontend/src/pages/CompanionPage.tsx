import { AppLayout } from '../layouts/AppLayout'
import { CompanionChat } from '../features/companion/CompanionChat'

export function CompanionPage() {
  return (
    <AppLayout>
      <CompanionChat />
    </AppLayout>
  )
}
