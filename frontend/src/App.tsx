import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AppLayout } from './layouts/AppLayout'
import { CheckInForm } from './features/mood/CheckInForm'
import { MoodHistory } from './features/mood/MoodHistory'

const queryClient = new QueryClient()

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AppLayout>
        <CheckInForm />
        <MoodHistory />
      </AppLayout>
    </QueryClientProvider>
  )
}

export default App
