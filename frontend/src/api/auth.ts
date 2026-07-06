import { apiFetch } from './client'
import type { LoginRequest, RegisterRequest, UserSummary } from '../types/auth'

export function register(payload: RegisterRequest): Promise<UserSummary> {
  return apiFetch<UserSummary>('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function login(payload: LoginRequest): Promise<UserSummary> {
  return apiFetch<UserSummary>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function logout(): Promise<void> {
  return apiFetch<void>('/api/v1/auth/logout', { method: 'POST' })
}

export function fetchCurrentUser(): Promise<UserSummary> {
  return apiFetch<UserSummary>('/api/v1/auth/me')
}
