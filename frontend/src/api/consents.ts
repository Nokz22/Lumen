import { apiFetch } from './client'

/** Mirrors the backend ConsentType enum — one entry per processing purpose. */
export const CONSENT_TYPES = [
  'HEALTH_DATA_PROCESSING',
  'LLM_PROCESSING',
  'WEARABLE_INGESTION',
] as const

export type ConsentType = (typeof CONSENT_TYPES)[number]

export interface ConsentStatus {
  active: boolean
}

export function fetchConsent(userId: string, consentType: ConsentType): Promise<ConsentStatus> {
  return apiFetch<ConsentStatus>(`/api/v1/users/${userId}/consents/${consentType}`)
}

export function grantConsent(userId: string, consentType: ConsentType): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/consents/${consentType}/grant`, { method: 'POST' })
}

export function revokeConsent(userId: string, consentType: ConsentType): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/consents/${consentType}/revoke`, {
    method: 'POST',
  })
}
