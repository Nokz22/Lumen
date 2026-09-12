import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement, ReactNode } from 'react'
import { I18nextProvider } from 'react-i18next'
import { MemoryRouter } from 'react-router-dom'
import i18n from '../i18n'

/**
 * Real i18n rather than a stub that echoes keys. These tests assert on the words a
 * person actually reads — the ethical boundary lives in the copy (ADR-0001), so a
 * test that only sees `crisis.title` cannot tell whether the screen says anything
 * it should not.
 */
export function renderWithProviders(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <I18nextProvider i18n={i18n}>
          <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
        </I18nextProvider>
      </QueryClientProvider>
    )
  }

  return render(ui, { wrapper: Wrapper })
}
