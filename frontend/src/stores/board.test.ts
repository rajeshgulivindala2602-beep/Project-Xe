import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { Alert } from '../api'

vi.mock('../api', () => ({
  ApiError: class ApiError extends Error {
    constructor(message: string, public readonly status?: number) {
      super(message)
    }
  },
  createAlert: vi.fn(),
  deleteAlert: vi.fn(),
  getAlerts: vi.fn(),
  getRates: vi.fn(),
}))

import { ApiError, createAlert, deleteAlert, getAlerts, getRates } from '../api'
import { useBoardStore } from './board'

const alert: Alert = {
  id: 'alert-1',
  pair: 'USD/CAD',
  threshold: 1.3,
  direction: 'above',
  triggered: false,
  createdAt: '2025-01-01T00:00:00Z',
  triggeredAt: null,
  currentRate: 1.2,
}

describe('board store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.resetAllMocks()
    vi.mocked(getRates).mockResolvedValue([])
    vi.mocked(getAlerts).mockResolvedValue([])
  })

  it('loads rates and alerts', async () => {
    const rate = { pair: 'USD/CAD', rate: 1.3, asOf: '2025-01-01T00:00:00Z' }
    vi.mocked(getRates).mockResolvedValue([rate])
    vi.mocked(getAlerts).mockResolvedValue([alert])
    const board = useBoardStore()

    await board.load()

    expect(board.rates).toEqual([rate])
    expect(board.alerts).toEqual([alert])
    expect(board.alertsLoaded).toBe(true)
  })

  it('keeps old alerts and reports an error when refresh fails', async () => {
    const board = useBoardStore()
    board.alerts = [alert]
    vi.mocked(getAlerts).mockRejectedValue(new Error('Cannot reach the server. Is the backend running?'))

    await board.load()

    expect(board.alerts).toEqual([alert])
    expect(board.alertsError).toContain('Cannot reach the server')
  })

  it('appends the created alert and returns no error', async () => {
    vi.mocked(createAlert).mockResolvedValue(alert)
    const board = useBoardStore()

    const error = await board.add('USD/CAD', 1.3, 'above')

    expect(error).toBeNull()
    expect(board.alerts).toEqual([alert])
  })

  it('returns backend error and does not append on add failure', async () => {
    vi.mocked(createAlert).mockRejectedValue(new Error('Threshold is invalid.'))
    const board = useBoardStore()

    const error = await board.add('USD/CAD', 1.3, 'above')

    expect(error).toBe('Threshold is invalid.')
    expect(board.alerts).toEqual([])
    expect(board.alertsError).toContain('Threshold is invalid.')
  })

  it('removes an alert after successful deletion', async () => {
    vi.mocked(deleteAlert).mockResolvedValue(undefined)
    const board = useBoardStore()
    board.alerts = [alert]

    await board.remove(alert.id)

    expect(board.alerts).toEqual([])
  })

  it('removes an alert when it has already been deleted by another request', async () => {
    vi.mocked(deleteAlert).mockRejectedValue(new ApiError('Not found', 404))
    const board = useBoardStore()
    board.alerts = [alert]

    await board.remove(alert.id)

    expect(board.alerts).toEqual([])
  })

  it('keeps an alert and reports other delete errors', async () => {
    vi.mocked(deleteAlert).mockRejectedValue(new Error('Server error'))
    const board = useBoardStore()
    board.alerts = [alert]

    await board.remove(alert.id)

    expect(board.alerts).toEqual([alert])
    expect(board.alertsError).toContain('Server error')
  })
})
