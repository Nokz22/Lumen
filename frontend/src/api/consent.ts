import { apiFetch } from './client'

const HEALTH_DATA_PROCESSING = 'HEALTH_DATA_PROCESSING'

interface ConsentStatusResponse {
  active: boolean
}

export function fetchHealthDataConsent(userId: string): Promise<ConsentStatusResponse> {
  return apiFetch<ConsentStatusResponse>(`/api/v1/users/${userId}/consents/${HEALTH_DATA_PROCESSING}`)
}

export function grantHealthDataConsent(userId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/consents/${HEALTH_DATA_PROCESSING}/grant`, {
    method: 'POST',
  })
}
