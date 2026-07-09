import { apiFetch } from './client'

const LLM_PROCESSING = 'LLM_PROCESSING'

interface ConsentStatusResponse {
  active: boolean
}

export function fetchCompanionConsent(userId: string): Promise<ConsentStatusResponse> {
  return apiFetch<ConsentStatusResponse>(`/api/v1/users/${userId}/consents/${LLM_PROCESSING}`)
}

export function grantCompanionConsent(userId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/consents/${LLM_PROCESSING}/grant`, {
    method: 'POST',
  })
}
