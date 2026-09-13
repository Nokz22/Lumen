/** Mirrors the backend's PageResponse. */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export interface PageParams {
  page?: number
  size?: number
}

export function pageQueryString({ page, size }: PageParams = {}): string {
  const params = new URLSearchParams()
  if (page !== undefined) params.set('page', String(page))
  if (size !== undefined) params.set('size', String(size))
  const query = params.toString()
  return query ? `?${query}` : ''
}
