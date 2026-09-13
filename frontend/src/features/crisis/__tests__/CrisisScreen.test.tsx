import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CrisisScreen } from '../CrisisScreen'
import { renderWithProviders } from '../../../test/renderWithProviders'
import type { CrisisResource } from '../../../types/assessment'

const RESOURCES: CrisisResource[] = [
  { name: 'SNS 24', type: 'HELPLINE', contact: '808 24 24 24', availability: '24/7' },
  { name: 'Emergência (112)', type: 'EMERGENCY_SERVICE', contact: '112', availability: '24/7' },
]

describe('CrisisScreen', () => {
  it('shows every regional resource with the contact and when it is reachable', () => {
    renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={false} />,
    )

    expect(screen.getByText('SNS 24')).toBeInTheDocument()
    expect(screen.getByText('808 24 24 24')).toBeInTheDocument()
    expect(screen.getByText('Emergência (112)')).toBeInTheDocument()
    expect(screen.getByText('112')).toBeInTheDocument()
    expect(screen.getAllByText('24/7')).toHaveLength(2)
  })

  /**
   * A contact nobody can read is the same as no contact. This is the one screen where
   * that is not a cosmetic failure.
   */
  it('renders no resource with a missing contact', () => {
    renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={false} />,
    )

    for (const resource of RESOURCES) {
      const entry = screen.getByText(resource.name).closest('li')
      expect(entry).toHaveTextContent(resource.contact)
    }
  })

  it('requires an explicit acknowledgment before the flow can continue', async () => {
    const onAcknowledge = vi.fn()
    renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={onAcknowledge} isAcknowledging={false} />,
    )

    expect(onAcknowledge).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button'))
    expect(onAcknowledge).toHaveBeenCalledTimes(1)
  })

  it('cannot be acknowledged twice while the first acknowledgment is in flight', () => {
    renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={true} />,
    )

    expect(screen.getByRole('button')).toBeDisabled()
  })

  /**
   * ADR-0001: the app describes what a person reported, it never tells them what they
   * are. The rule is about interpretation, not about the words "you are" — "if you are
   * in immediate danger, contact emergency services" is a conditional pointing at help,
   * which is the whole purpose of this screen.
   */
  it('never tells the person what they are', () => {
    const { container } = renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={false} />,
    )

    const text = container.textContent ?? ''
    expect(text).not.toMatch(/you are (depressed|anxious|suicidal|at risk|in crisis|unwell|ill)/i)
    expect(text).not.toMatch(/depress|diagnos|disorder|mental illness/i)
  })

  /** Calm, not alarming (project-brief section 10): no shouting on this screen. */
  it('raises its voice at nobody', () => {
    const { container } = renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={false} />,
    )

    const text = container.textContent ?? ''
    expect(text).not.toContain('!')
    expect(text).not.toMatch(/[A-Z]{4,}/)
  })

  /** A screen reader should announce this appearing, not wait to be asked. */
  it('announces itself politely to assistive technology', () => {
    const { container } = renderWithProviders(
      <CrisisScreen resources={RESOURCES} onAcknowledge={vi.fn()} isAcknowledging={false} />,
    )

    expect(container.querySelector('[aria-live="polite"]')).not.toBeNull()
  })
})
