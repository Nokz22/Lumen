import { apiFetch } from './client'

/**
 * Fetched as a blob and saved from memory rather than linked to directly: the auth cookie
 * is SameSite=Strict, so a plain <a href> to the API origin would navigate without it and
 * land on a 401.
 */
export async function downloadDataExport(userId: string): Promise<void> {
  const data = await apiFetch<unknown>(`/api/v1/users/${userId}/privacy/export`)
  const url = URL.createObjectURL(
    new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }),
  )
  try {
    const link = document.createElement('a')
    link.href = url
    link.download = 'lumen-data-export.json'
    link.click()
  } finally {
    URL.revokeObjectURL(url)
  }
}

export function eraseAccount(userId: string): Promise<void> {
  return apiFetch<void>(`/api/v1/users/${userId}/privacy/account`, { method: 'DELETE' })
}
