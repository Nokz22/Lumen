import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchConsent, grantConsent } from '../../api/consents'

const consentKey = (userId: string) => ['health-data-consent', userId]

export function useHealthDataConsent(userId: string) {
  return useQuery({
    queryKey: consentKey(userId),
    queryFn: () => fetchConsent(userId, 'HEALTH_DATA_PROCESSING'),
  })
}

export function useGrantHealthDataConsent(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => grantConsent(userId, 'HEALTH_DATA_PROCESSING'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: consentKey(userId) })
    },
  })
}
