import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { GuidedSession } from './GuidedSession'
import type { Exercise } from '../../types/exercise'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({
    t: (key: string, options?: Record<string, unknown>) =>
      options ? `${key}:${JSON.stringify(options)}` : key,
  }),
}))

const groundingExercise: Exercise = {
  id: 'ex-2',
  category: 'GROUNDING',
  name: 'Feet on the Floor',
  durationMinutes: 0.05, // 3s total, split across 3 steps = 1s per step
  intensity: 'LOW',
  rationale: 'rationale',
  inhaleSeconds: null,
  holdAfterInhaleSeconds: null,
  exhaleSeconds: null,
  holdAfterExhaleSeconds: null,
  steps: ['Step one text', 'Step two text', 'Step three text'],
}

describe('GuidedSession', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders the first step and its progress', () => {
    render(<GuidedSession exercise={groundingExercise} onFinish={vi.fn()} onClose={vi.fn()} />)

    expect(screen.getByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Step one text')).toBeInTheDocument()
    expect(screen.getByText(/exercise\.session\.step/)).toHaveTextContent('"step":1')
  })

  it('advances through steps and calls onFinish exactly once at the end', () => {
    const onFinish = vi.fn()
    render(<GuidedSession exercise={groundingExercise} onFinish={onFinish} onClose={vi.fn()} />)

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('Step two text')).toBeInTheDocument()
    expect(onFinish).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('Step three text')).toBeInTheDocument()
    expect(onFinish).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(screen.getByText('exercise.session.completed')).toBeInTheDocument()
    expect(onFinish).toHaveBeenCalledTimes(1)

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
    render(<GuidedSession exercise={groundingExercise} onFinish={onFinish} onClose={onClose} />)

    await user.click(screen.getByText('exercise.session.exit'))

    expect(onClose).toHaveBeenCalledTimes(1)
    expect(onFinish).not.toHaveBeenCalled()
  })

  it('renders nothing for an exercise without steps', () => {
    const noSteps: Exercise = { ...groundingExercise, steps: [] }

    const { container } = render(
      <GuidedSession exercise={noSteps} onFinish={vi.fn()} onClose={vi.fn()} />,
    )

    expect(container.firstChild).toBeNull()
  })
})
