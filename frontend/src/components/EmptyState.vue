<script setup>
import Icon from './Icon.vue'

defineProps({
  title: { type: String, default: '暂无数据' },
  desc: { type: String, default: '这里还没有内容，稍后再来看看吧。' },
  icon: { type: String, default: 'spark' }
})
</script>

<template>
  <div class="empty">
    <div class="empty-icon"><Icon :name="icon" :size="22" /></div>
    <h4>{{ title }}</h4>
    <p>{{ desc }}</p>
    <div v-if="$slots.default" class="mt-12"><slot /></div>
  </div>
</template>
