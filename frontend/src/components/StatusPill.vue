<script setup>
import { computed } from 'vue'
import { meta as resolveMeta } from '../utils/labels'

const props = defineProps({
  value: { type: String, default: '' },
  map: { type: Object, required: true }
})

const item = computed(() => resolveMeta(props.map, props.value))
</script>

<template>
  <span class="pill" :class="`pill--${item.tone}`">
    <span class="dot" />
    {{ item.label }}
  </span>
</template>
