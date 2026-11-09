<script setup>
import { toasts, dismissToast } from '../composables/useToast'
import Icon from './Icon.vue'
</script>

<template>
  <Teleport to="body">
    <div class="toast-stack">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="toast"
        :class="`toast--${toast.type}`"
        @click="dismissToast(toast.id)"
      >
        <Icon :name="toast.type === 'error' ? 'alert' : toast.type === 'success' ? 'check' : 'spark'" :size="16" />
        <span>{{ toast.message }}</span>
      </div>
    </div>
  </Teleport>
</template>
