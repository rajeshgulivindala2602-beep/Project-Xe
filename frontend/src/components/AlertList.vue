<script setup lang="ts">
import type { Alert } from '../api'

defineProps<{
  alerts: Alert[]
  loaded: boolean
}>()

const emit = defineEmits<{
  remove: [id: string]
}>()

function formatTime(value: string): string {
  return new Date(value).toLocaleString()
}
</script>

<template>
  <section class="panel">
    <h2>Alerts</h2>
    <p v-if="!loaded" class="empty">Loading alerts...</p>
    <p v-else-if="alerts.length === 0" class="empty">No alerts yet.</p>
    <ul v-else class="alerts">
      <li v-for="alert in alerts" :key="alert.id" class="alert">
        <div class="details">
          <strong>{{ alert.pair }} {{ alert.direction }} {{ alert.threshold.toFixed(2) }}</strong>
          <span :class="['badge', alert.triggered ? 'triggered' : 'waiting']">
            {{ alert.triggered ? 'Triggered' : 'Waiting' }}
          </span>
          <span v-if="alert.triggered && alert.triggeredAt" class="extra">
            Triggered {{ formatTime(alert.triggeredAt) }}
          </span>
          <span v-else-if="alert.currentRate !== null" class="extra">
            Current rate {{ alert.currentRate.toFixed(4) }}
          </span>
          <span v-else class="extra">Current rate unavailable</span>
        </div>
        <button class="delete" type="button" @click="emit('remove', alert.id)">Delete</button>
      </li>
    </ul>
  </section>
</template>

<style scoped>
.panel {
  margin-top: 20px;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e1e6ee;
  border-radius: 10px;
}

h2 {
  margin: 0 0 16px;
  font-size: 1.15rem;
}

.empty {
  margin: 0;
  color: #66718a;
}

.alerts {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.alert {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 0;
  border-top: 1px solid #edf0f5;
}

.details {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.badge {
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 0.78rem;
  font-weight: 600;
}

.waiting {
  background: #edf2f8;
  color: #53627b;
}

.triggered {
  background: #fde8e7;
  color: #b42318;
}

.extra {
  color: #66718a;
  font-size: 0.85rem;
}

.delete {
  padding: 7px 11px;
  border: 1px solid #d7deea;
  border-radius: 7px;
  background: #ffffff;
  color: #344054;
  cursor: pointer;
}

.delete:hover {
  background: #f4f6f8;
}
</style>
