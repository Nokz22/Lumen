import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchWearableInsights, simulateWearableReadings } from '../../api/wearable'
import { fetchConsent, grantConsent } from '../../api/consents'

const wearableConsentKey = (userId: string) => ['wearable-consent', userId]
const wearableInsightsKey = (userId: string) => ['wearable-insights', userId]

export function useWearableConsent(userId: string) {
  return useQuery({
    queryKey: wearableConsentKey(userId),
    queryFn: () => fetchConsent(userId, 'WEARABLE_INGESTION'),
  })
}

export function useGrantWearableConsent(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => grantConsent(userId, 'WEARABLE_INGESTION'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wearableConsentKey(userId) })
    },
  })
}

export function useWearableInsights(userId: string, enabled: boolean) {
  return useQuery({
    queryKey: wearableInsightsKey(userId),
    queryFn: () => fetchWearableInsights(userId),
    enabled,
  })
}

export function useSimulateWearableReadings(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (days: number) => simulateWearableReadings(userId, days),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wearableInsightsKey(userId) })
    },
  })
}
