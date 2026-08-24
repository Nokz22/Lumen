import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchConsent, grantConsent, revokeConsent, type ConsentType } from '../../api/consents'
import { downloadDataExport, eraseAccount } from '../../api/privacy'

export const consentKey = (userId: string, consentType: ConsentType) => [
  'consent',
  userId,
  consentType,
]

export function useConsentStatus(userId: string, consentType: ConsentType) {
  return useQuery({
    queryKey: consentKey(userId, consentType),
    queryFn: () => fetchConsent(userId, consentType),
  })
}

export function useSetConsent(userId: string, consentType: ConsentType) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (granted: boolean) =>
      granted ? grantConsent(userId, consentType) : revokeConsent(userId, consentType),
    // Revoking disables the dependent feature in the same request cycle (ADR-0005), so
    // every cached read has to be treated as stale, not just this consent's own entry.
    onSuccess: () => queryClient.invalidateQueries(),
  })
}

export function useDataExport(userId: string) {
  return useMutation({ mutationFn: () => downloadDataExport(userId) })
}

export function useEraseAccount(userId: string) {
  return useMutation({ mutationFn: () => eraseAccount(userId) })
}
