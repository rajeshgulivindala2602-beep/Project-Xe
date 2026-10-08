export type Rate = {
  pair: string
  rate: number
  asOf: string
}

export type Alert = {
  id: string
  pair: string
  threshold: number
  direction: 'above' | 'below'
  triggered: boolean
  createdAt: string
  triggeredAt: string | null
  currentRate: number | null
}

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

async function request<T>(url: string, options?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(url, options)
  } catch {
    throw new Error('Cannot reach the server. Is the backend running?')
  }

  if (!response.ok) {
    let message = response.status === 500
      ? 'Cannot reach the server. Is the backend running?'
      : `Request failed (${response.status}).`
    try {
      const body: { error?: string } = await response.json()
      if (body.error) message = body.error
    } catch {
      // Use the generic request error when the response has no JSON error body.
    }
    throw new ApiError(message, response.status)
  }

  if (response.status === 204) return undefined as T

  try {
    return await response.json() as T
  } catch {
    throw new Error('The server returned an invalid response.')
  }
}

export function getRates(): Promise<Rate[]> {
  return request<Rate[]>('/api/rates')
}

export function getAlerts(): Promise<Alert[]> {
  return request<Alert[]>('/api/alerts')
}

export function createAlert(
  pair: string,
  threshold: number,
  direction: Alert['direction'],
): Promise<Alert> {
  return request<Alert>('/api/alerts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ pair, threshold, direction }),
  })
}

export function deleteAlert(id: string): Promise<void> {
  return request<void>(`/api/alerts/${encodeURIComponent(id)}`, { method: 'DELETE' })
}
