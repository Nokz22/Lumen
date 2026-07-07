export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp(`(?:^|; )${name}=([^;]*)`))
  return match ? decodeURIComponent(match[1]) : null
}

function rawFetch(path: string, init?: RequestInit): Promise<Response> {
  const headers = new Headers(init?.headers)
  if (!headers.has('Content-Type') && init?.body) {
    headers.set('Content-Type', 'application/json')
  }

  const method = (init?.method ?? 'GET').toUpperCase()
  if (method !== 'GET' && method !== 'HEAD') {
    const csrfToken = readCookie('XSRF-TOKEN')
    if (csrfToken) {
      headers.set('X-XSRF-TOKEN', csrfToken)
    }
  }

  return fetch(`${API_BASE_URL}${path}`, { credentials: 'include', ...init, headers })
}

let pendingRefresh: Promise<boolean> | null = null

function refreshAccessToken(): Promise<boolean> {
  pendingRefresh ??= rawFetch('/api/v1/auth/refresh', { method: 'POST' })
    .then((response) => response.ok)
    .finally(() => {
      pendingRefresh = null
    })
  return pendingRefresh
}

/** On a 401, tries a silent refresh once and retries the original request before giving up. */
export async function apiFetch<T>(path: string, init?: RequestInit, isRetry = false): Promise<T> {
  const response = await rawFetch(path, init)

  if (response.status === 401 && !isRetry && (await refreshAccessToken())) {
    return apiFetch<T>(path, init, true)
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(problem?.detail ?? response.statusText, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
