import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { DeleteAccountCard } from '../DeleteAccountCard'
import { renderWithProviders } from '../../../test/renderWithProviders'

const eraseAccount = vi.fn()
const clearSession = vi.fn()

vi.mock('../../../api/privacy', () => ({
  eraseAccount: (userId: string) => eraseAccount(userId),
  downloadDataExport: vi.fn(),
}))

vi.mock('../../../contexts/AuthContext', () => ({
  useAuth: () => ({ clearSession }),
}))

const USER_ID = '11111111-1111-1111-1111-111111111111'

describe('DeleteAccountCard', () => {
  beforeEach(() => {
    eraseAccount.mockReset().mockResolvedValue(undefined)
    clearSession.mockReset()
  })

  /**
   * Erasure is irreversible and takes the crisis history with it. It must not be
   * reachable by a mis-tap, which is the only place in this app that deliberately
   * adds friction.
   */
  it('cannot be triggered without typing the confirmation', () => {
    renderWithProviders(<DeleteAccountCard userId={USER_ID} />)

    expect(screen.getByRole('button', { name: /delete my account/i })).toBeDisabled()
  })

  it('stays disabled for a confirmation that is nearly right', async () => {
    renderWithProviders(<DeleteAccountCard userId={USER_ID} />)

    await userEvent.type(screen.getByLabelText(/type delete/i), 'DELET')

    expect(screen.getByRole('button', { name: /delete my account/i })).toBeDisabled()
  })

  it('erases the account only after the confirmation is typed', async () => {
    renderWithProviders(<DeleteAccountCard userId={USER_ID} />)

    await userEvent.type(screen.getByLabelText(/type delete/i), 'DELETE')
    await userEvent.click(screen.getByRole('button', { name: /delete my account/i }))

    expect(eraseAccount).toHaveBeenCalledWith(USER_ID)
  })

  /**
   * The server has already cleared the cookies by this point; leaving the in-memory user
   * in place would render a dashboard for an account that no longer exists.
   */
  it('drops the session once the account is gone', async () => {
    renderWithProviders(<DeleteAccountCard userId={USER_ID} />)

    await userEvent.type(screen.getByLabelText(/type delete/i), 'DELETE')
    await userEvent.click(screen.getByRole('button', { name: /delete my account/i }))

    await vi.waitFor(() => expect(clearSession).toHaveBeenCalled())
  })

  it('keeps the session when erasure fails', async () => {
    eraseAccount.mockRejectedValue(new Error('server said no'))
    renderWithProviders(<DeleteAccountCard userId={USER_ID} />)

    await userEvent.type(screen.getByLabelText(/type delete/i), 'DELETE')
    await userEvent.click(screen.getByRole('button', { name: /delete my account/i }))

    await screen.findByRole('alert')
    expect(clearSession).not.toHaveBeenCalled()
  })
})
