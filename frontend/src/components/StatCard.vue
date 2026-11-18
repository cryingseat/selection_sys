<script setup>
defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: '—' },
  hint: { type: String, default: '' },
  tint: { type: String, default: '#fbe3ea' }
})
</script>

<template>
  <div class="stat-card" :style="{ '--stat-tint': tint }">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value">{{ value }}</div>
    <div v-if="hint" class="stat-foot">{{ hint }}</div>
  </div>
</template>
