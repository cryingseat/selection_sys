<script setup>
import Icon from './Icon.vue'

defineProps({
  stages: { type: Array, default: () => [] },
  horizontal: { type: Boolean, default: false },
  compact: { type: Boolean, default: false }
})
</script>

<template>
  <div class="stepper" :class="{ horizontal }">
    <div v-for="stage in stages" :key="stage.no" class="step" :class="stage.state">
      <div class="step-no">
        <Icon v-if="stage.state === 'done'" name="check" :size="14" />
        <template v-else>{{ stage.no }}</template>
      </div>
      <div class="step-body">
        <div class="step-title">
          {{ stage.title }}
          <span v-if="stage.state === 'active'" class="pill pill--green"><span class="dot" />进行中</span>
        </div>
        <div v-if="!compact" class="step-desc">{{ stage.desc }}</div>
        <div class="step-time">{{ stage.time }}</div>
      </div>
    </div>
  </div>
</template>
