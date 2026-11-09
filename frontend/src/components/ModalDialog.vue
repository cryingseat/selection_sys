<script setup>
import { onBeforeUnmount, onMounted } from 'vue'
import Icon from './Icon.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  wide: { type: Boolean, default: false }
})
const emit = defineEmits(['close'])

function onKey(event) {
  if (event.key === 'Escape' && props.open) emit('close')
}

onMounted(() => document.addEventListener('keydown', onKey))
onBeforeUnmount(() => document.removeEventListener('keydown', onKey))
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="open" class="modal-backdrop" @click.self="emit('close')">
        <div class="modal" :class="{ wide }" role="dialog" aria-modal="true">
          <div class="modal-head">
            <div>
              <h3>{{ title }}</h3>
              <p v-if="subtitle" class="small muted mt-4">{{ subtitle }}</p>
            </div>
            <button class="modal-close" type="button" aria-label="关闭" @click="emit('close')">
              <Icon name="x" :size="17" />
            </button>
          </div>
          <slot />
          <div v-if="$slots.footer" class="modal-foot">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.fade-enter-active, .fade-leave-active { transition: opacity 0.16s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
