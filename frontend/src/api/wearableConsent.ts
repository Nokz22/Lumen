import { apiFetch } from './client'

const WEARABLE_INGESTION = 'WEARABLE_INGESTION'

interface ConsentStatusResponse {
  active: boolean
}

export function fetchWearableConsent(userId: string): Promise<ConsentStatusResponse> {
  return apiFetch<ConsentStatusResponse>(`/api/v1/users/${userId}/consents/${WEARABLE_INGESTION}`)
}

export function grantWearableConsent(userId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/consents/${WEARABLE_INGESTION}/grant`, {
    method: 'POST',
  })
}
