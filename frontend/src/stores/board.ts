import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ApiError, createAlert, deleteAlert, getAlerts, getRates } from '../api'
import type { Alert, Rate } from '../api'

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong.'
}

export const useBoardStore = defineStore('board', () => {
  const rates = ref<Rate[]>([])
  const alerts = ref<Alert[]>([])
  const lastUpdated = ref('')
  const ratesError = ref('')
  const alertsError = ref('')
  const alertsLoaded = ref(false)

  async function load() {
    const ratesRequest = getRates()
      .then((data) => {
        rates.value = data
        ratesError.value = ''
        lastUpdated.value = new Date().toLocaleTimeString()
      })
      .catch((error: unknown) => {
        ratesError.value = `Could not load rates: ${errorMessage(error)}`
      })

    const alertsRequest = getAlerts()
      .then((data) => {
        alerts.value = data
        alertsError.value = ''
        alertsLoaded.value = true
      })
      .catch((error: unknown) => {
        alertsError.value = `Could not load alerts: ${errorMessage(error)}`
      })

    await Promise.all([ratesRequest, alertsRequest])
  }

  async function add(pair: string, threshold: number, direction: Alert['direction']) {
    try {
      const alert = await createAlert(pair, threshold, direction)
      alerts.value = [...alerts.value, alert]
      alertsError.value = ''
      alertsLoaded.value = true
      return null
    } catch (error) {
      const message = errorMessage(error)
      alertsError.value = `Could not add alert: ${message}`
      return message
    }
  }

  async function remove(id: string) {
    try {
      await deleteAlert(id)
      alerts.value = alerts.value.filter((alert) => alert.id !== id)
      alertsError.value = ''
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        alerts.value = alerts.value.filter((alert) => alert.id !== id)
        alertsError.value = ''
        return
      }
      alertsError.value = `Could not delete alert: ${errorMessage(error)}`
    }
  }

  return {
    rates,
    alerts,
    lastUpdated,
    ratesError,
    alertsError,
    alertsLoaded,
    load,
    add,
    remove,
  }
})
