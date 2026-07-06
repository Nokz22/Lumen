import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchHealthDataConsent, grantHealthDataConsent } from '../../api/consent'

const consentKey = (userId: string) => ['health-data-consent', userId]

export function useHealthDataConsent(userId: string) {
  return useQuery({
    queryKey: consentKey(userId),
    queryFn: () => fetchHealthDataConsent(userId),
  })
}

export function useGrantHealthDataConsent(userId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: () => grantHealthDataConsent(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: consentKey(userId) })
    },
  })
}
