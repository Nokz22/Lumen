import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ConsentGate } from '../ConsentGate'
import { renderWithProviders } from '../../../test/renderWithProviders'

const fetchConsent = vi.fn()

vi.mock('../../../api/consents', () => ({
  CONSENT_TYPES: ['HEALTH_DATA_PROCESSING', 'LLM_PROCESSING', 'WEARABLE_INGESTION'],
  fetchConsent: (userId: string, type: string) => fetchConsent(userId, type),
  grantConsent: vi.fn().mockResolvedValue(undefined),
  revokeConsent: vi.fn().mockResolvedValue(undefined),
}))

vi.mock('../../../contexts/AuthContext', () => ({
  useAuth: () => ({ user: { id: '11111111-1111-1111-1111-111111111111' } }),
}))

describe('ConsentGate', () => {
  beforeEach(() => fetchConsent.mockReset())

  /**
   * The gate is the visible half of ADR-0005; the backend enforces the same rule, so a
   * failure here is a confusing experience rather than a leak. It still must not show
   * health data to someone who has not consented to its processing.
   */
  it('withholds the protected content until consent is active', async () => {
    fetchConsent.mockResolvedValue({ active: false })
    renderWithProviders(
      <ConsentGate>
        <p>daily check-in form</p>
      </ConsentGate>,
    )

    expect(await screen.findByRole('button', { name: /i consent/i })).toBeInTheDocument()
    expect(screen.queryByText('daily check-in form')).not.toBeInTheDocument()
  })

  it('shows the protected content once consent is active', async () => {
    fetchConsent.mockResolvedValue({ active: true })
    renderWithProviders(
      <ConsentGate>
        <p>daily check-in form</p>
      </ConsentGate>,
    )

    expect(await screen.findByText('daily check-in form')).toBeInTheDocument()
  })

  /** An unanswered request is not consent. */
  it('withholds the content while consent is still unknown', async () => {
    // Deferred rather than a promise that never settles: an unresolved request left
    // dangling keeps the test running until the suite times out.
    let resolveConsent: (value: { active: boolean }) => void = () => {}
    fetchConsent.mockReturnValue(
      new Promise<{ active: boolean }>((resolve) => {
        resolveConsent = resolve
      }),
    )
    renderWithProviders(
      <ConsentGate>
        <p>daily check-in form</p>
      </ConsentGate>,
    )

    expect(screen.queryByText('daily check-in form')).not.toBeInTheDocument()
    expect(screen.getByText(/checking your consent status/i)).toBeInTheDocument()

    resolveConsent({ active: true })
    expect(await screen.findByText('daily check-in form')).toBeInTheDocument()
  })

  /** Says what it is for and that it can be withdrawn — consent that is not informed is not consent. */
  it('explains what is being consented to before asking', async () => {
    fetchConsent.mockResolvedValue({ active: false })
    const { container } = renderWithProviders(
      <ConsentGate>
        <p>daily check-in form</p>
      </ConsentGate>,
    )

    await screen.findByRole('button', { name: /i consent/i })
    expect(container.textContent).toMatch(/mood, energy, sleep/i)
    expect(container.textContent).toMatch(/revoke/i)
  })
})
