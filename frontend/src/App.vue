<script setup lang="ts">
import { computed, onMounted, onUnmounted } from 'vue'
import AlertForm from './components/AlertForm.vue'
import AlertList from './components/AlertList.vue'
import { useBoardStore } from './stores/board'

const board = useBoardStore()
const triggeredCount = computed(() => board.alerts.filter((alert) => alert.triggered).length)
let refreshTimer: ReturnType<typeof setInterval>

onMounted(() => {
  void board.load()
  refreshTimer = setInterval(() => void board.load(), 15_000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<template>
  <main class="page">
    <div v-if="triggeredCount > 0" class="trigger-banner" role="status">
      {{ triggeredCount }} {{ triggeredCount === 1 ? 'alert is' : 'alerts are' }} triggered.
    </div>

    <header class="header">
      <h1>Xe Rate Board</h1>
      <span v-if="board.lastUpdated" class="updated">Last updated {{ board.lastUpdated }}</span>
    </header>

    <p v-if="board.ratesError" class="error-message" role="alert">{{ board.ratesError }}</p>
    <p
      v-if="board.alertsError && !board.alertsError.startsWith('Could not add')"
      class="error-message"
      role="alert"
    >
      {{ board.alertsError }}
    </p>

    <section class="cards">
      <div v-for="rate in board.rates" :key="rate.pair" class="card">
        <div class="pair">{{ rate.pair.replace('/', ' / ') }}</div>
        <div class="rate">{{ rate.rate.toFixed(4) }}</div>
        <div class="caption">1 {{ rate.pair.split('/')[0] }} in {{ rate.pair.split('/')[1] }}</div>
      </div>
    </section>

    <button class="refresh" type="button" @click="board.load()">Refresh rates</button>

    <AlertForm />
    <AlertList :alerts="board.alerts" :loaded="board.alertsLoaded" @remove="board.remove" />
  </main>
</template>

<style>
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  font-family: 'Segoe UI', system-ui, sans-serif;
  background: #f4f6f8;
  color: #1a2233;
}

.page {
  max-width: 860px;
  margin: 0 auto;
  padding: 32px 20px;
}

.header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}

h1 {
  font-size: 1.6rem;
  margin: 0;
}

.updated {
  font-size: 0.85rem;
  color: #66718a;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.card {
  background: #ffffff;
  border: 1px solid #e1e6ee;
  border-radius: 10px;
  padding: 20px;
}

.pair {
  font-size: 0.9rem;
  font-weight: 600;
  color: #66718a;
  letter-spacing: 0.04em;
}

.rate {
  font-size: 2rem;
  font-weight: 700;
  margin: 8px 0 4px;
  font-variant-numeric: tabular-nums;
}

.caption {
  font-size: 0.8rem;
  color: #8a93a8;
}

.refresh {
  margin-top: 24px;
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #16345c;
  color: #ffffff;
  font-size: 0.9rem;
  cursor: pointer;
}

.refresh:hover {
  background: #1d4377;
}

.trigger-banner {
  margin-bottom: 18px;
  padding: 12px 16px;
  border: 1px solid #f1aaa5;
  border-radius: 8px;
  background: #fde8e7;
  color: #b42318;
  font-weight: 600;
}

.error-message {
  margin: 0 0 16px;
  color: #b42318;
  font-size: 0.9rem;
}
</style>
