<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useBoardStore } from '../stores/board'
import { validateThreshold } from '../validation'

const board = useBoardStore()
const pair = ref('')
const direction = ref<'above' | 'below'>('above')
const threshold = ref('')
const validationError = ref('')
const availablePairs = computed(() => board.rates.map((rate) => rate.pair))

watch(availablePairs, (pairs) => {
  if (!pairs.includes(pair.value)) pair.value = pairs[0] ?? ''
}, { immediate: true })

async function submit() {
  const result = validateThreshold(threshold.value)
  if (typeof result === 'string') {
    validationError.value = result
    return
  }

  validationError.value = ''
  const error = await board.add(pair.value, result, direction.value)
  if (error === null) threshold.value = ''
}
</script>

<template>
  <section class="panel alert-form">
    <h2>Add an alert</h2>
    <form @submit.prevent="submit">
      <label>
        Pair
        <select v-model="pair" required>
          <option v-for="option in availablePairs" :key="option" :value="option">
            {{ option }}
          </option>
        </select>
      </label>

      <label>
        Direction
        <select v-model="direction">
          <option value="above">Above</option>
          <option value="below">Below</option>
        </select>
      </label>

      <label>
        Threshold
        <input
          v-model="threshold"
          type="text"
          inputmode="decimal"
          placeholder="e.g. 1.84"
          autocomplete="off"
        />
      </label>

      <button type="submit" :disabled="availablePairs.length === 0">Add alert</button>
    </form>
    <p v-if="validationError" class="form-error" role="alert">{{ validationError }}</p>
    <p v-else-if="board.alertsError.startsWith('Could not add')" class="form-error" role="alert">
      {{ board.alertsError }}
    </p>
  </section>
</template>

<style scoped>
.panel {
  margin-top: 28px;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e1e6ee;
  border-radius: 10px;
}

h2 {
  margin: 0 0 16px;
  font-size: 1.15rem;
}

form {
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  color: #66718a;
  font-size: 0.85rem;
}

input,
select,
button {
  min-height: 40px;
  padding: 8px 10px;
  border: 1px solid #d7deea;
  border-radius: 7px;
  font: inherit;
}

button {
  border: none;
  background: #16345c;
  color: #ffffff;
  cursor: pointer;
}

button:hover:not(:disabled) {
  background: #1d4377;
}

button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.form-error {
  margin: 12px 0 0;
  color: #b42318;
  font-size: 0.9rem;
}
</style>
