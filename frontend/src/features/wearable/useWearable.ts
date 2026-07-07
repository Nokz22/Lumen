import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchWearableInsights, simulateWearableReadings } from '../../api/wearable'
import { fetchWearableConsent, grantWearableConsent } from '../../api/wearableConsent'

const wearableConsentKey = (userId: string) => ['wearable-consent', userId]
const wearableInsightsKey = (userId: string) => ['wearable-insights', userId]

export function useWearableConsent(userId: string) {
  return useQuery({
    queryKey: wearableConsentKey(userId),
    queryFn: () => fetchWearableConsent(userId),
  })
}

export function useGrantWearableConsent(userId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () => grantWearableConsent(userId),
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
