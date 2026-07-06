export type Role = 'USER' | 'ADMIN'

export interface UserSummary {
  id: string
  email: string
  displayName: string
  role: Role
  createdAt: string
}

export interface RegisterRequest {
  email: string
  password: string
  displayName: string
  locale: string
  region: string
  dateOfBirth: string
}

export interface LoginRequest {
  email: string
  password: string
}
