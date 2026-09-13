import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it } from 'vitest'
import { ThemeProvider } from '../ThemeContext'
import { ThemeToggle } from '../../components/ThemeToggle'
import { renderWithProviders } from '../../test/renderWithProviders'

function renderToggle() {
  return renderWithProviders(
    <ThemeProvider>
      <ThemeToggle />
    </ThemeProvider>,
  )
}

describe('theme', () => {
  beforeEach(() => {
    localStorage.clear()
    delete document.documentElement.dataset.theme
  })

  /** project-brief section 10: dark is the default, not a preference to be discovered. */
  it('starts dark', () => {
    renderToggle()

    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('switches to light and back', async () => {
    renderToggle()

    await userEvent.click(screen.getByRole('button'))
    expect(document.documentElement.dataset.theme).toBe('light')

    await userEvent.click(screen.getByRole('button'))
    expect(document.documentElement.dataset.theme).toBe('dark')
  })

  it('remembers the choice across reloads', async () => {
    const { unmount } = renderToggle()
    await userEvent.click(screen.getByRole('button'))
    unmount()

    renderToggle()

    expect(document.documentElement.dataset.theme).toBe('light')
  })

  /** The label has to say what the button does; there is no text beside the icon. */
  it('labels the button with the action, not the current state', async () => {
    renderToggle()

    expect(screen.getByRole('button')).toHaveAccessibleName(/switch to light/i)
    await userEvent.click(screen.getByRole('button'))
    expect(screen.getByRole('button')).toHaveAccessibleName(/switch to dark/i)
  })
})
