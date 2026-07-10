import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { BreathingSession } from './BreathingSession'
import type { Exercise } from '../../types/exercise'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, options?: Record<string, unknown>) =>
      options ? `${key}:${JSON.stringify(options)}` : key,
  }),
}))

const boxBreathing: Exercise = {
  id: 'ex-1',
  category: 'BREATHING',
  name: 'Box Breathing',
  durationMinutes: 0.1, // 6s, so with 3s cycles (1+1+1) this runs exactly 2 cycles
  intensity: 'LOW',
  rationale: 'rationale',
  inhaleSeconds: 1,
  holdAfterInhaleSeconds: 1,
  exhaleSeconds: 1,
  holdAfterExhaleSeconds: null,
  steps: [],
}

describe('BreathingSession', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the first phase and starts at cycle 1', () => {
    render(<BreathingSession exercise={boxBreathing} onFinish={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('exercise.session.phase.INHALE')).toBeInTheDocument()
    expect(screen.getByText(/exercise\.session\.progress/)).toHaveTextContent('"cycle":1')
  })

  it('advances through phases and cycles, then calls onFinish exactly once', () => {
    const onFinish = vi.fn()
    render(<BreathingSession exercise={boxBreathing} onFinish={onFinish} onClose={vi.fn()} />)

    // phases per cycle: inhale (1s) -> hold (1s) -> exhale (1s)
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('exercise.session.phase.HOLD_AFTER_INHALE')).toBeInTheDocument()
    expect(onFinish).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('exercise.session.phase.EXHALE')).toBeInTheDocument()
    expect(onFinish).not.toHaveBeenCalled()

    // exhale ends the cycle -> cycle 2 starts back at inhale
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('exercise.session.phase.INHALE')).toBeInTheDocument()
    expect(screen.getByText(/exercise\.session\.progress/)).toHaveTextContent('"cycle":2')
    expect(onFinish).not.toHaveBeenCalled()

    // second cycle: inhale -> hold -> exhale -> session complete
    // (advanced one phase at a time so each effect gets a chance to schedule the next timer)
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('exercise.session.completed')).toBeInTheDocument()
    expect(onFinish).toHaveBeenCalledTimes(1)

    // further timer ticks must not call onFinish again
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(onFinish).toHaveBeenCalledTimes(1)
  })

  it('calls onClose without completing when exited early', async () => {
    vi.useRealTimers()
    const user = userEvent.setup()
    const onFinish = vi.fn()
    const onClose = vi.fn()
    render(<BreathingSession exercise={boxBreathing} onFinish={onFinish} onClose={onClose} />)

    await user.click(screen.getByText('exercise.session.exit'))

    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onFinish).not.toHaveBeenCalled()
  })

  it('renders nothing for an exercise without a breathing pattern', () => {
    const walk: Exercise = {
      ...boxBreathing,
      category: 'WALKING',
      inhaleSeconds: null,
      holdAfterInhaleSeconds: null,
      exhaleSeconds: null,
      holdAfterExhaleSeconds: null,
    }

    const { container } = render(
      <BreathingSession exercise={walk} onFinish={vi.fn()} onClose={vi.fn()} />,
    )

    expect(container.firstChild).toBeNull()
  })
})
