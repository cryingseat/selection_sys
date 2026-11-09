<script setup>
import { computed, ref } from 'vue'
import Icon from './Icon.vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  labs: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] },
  max: { type: Number, default: 3 },
  disabled: { type: Boolean, default: false },
  disabledLabs: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue'])

const keyword = ref('')

const selectedMentors = computed(() => {
  const all = props.labs.flatMap((lab) => (lab.mentors || []).map((mentor) => ({ ...mentor, labName: lab.name })))
  return props.modelValue
    .map((id) => all.find((mentor) => mentor.id === id))
    .filter(Boolean)
})

const visibleLabs = computed(() => {
  const key = keyword.value.trim()
  return props.labs
    .map((lab) => {
      const mentors = (lab.mentors || []).filter((mentor) => {
        if (!key) return true
        return `${lab.name}${lab.direction}${mentor.name}${mentor.researchDirection}`.includes(key)
      })
      return { ...lab, mentors }
    })
    .filter((lab) => lab.mentors.length > 0)
})

function isDisabledLab(labId) {
  return props.disabledLabs.includes(labId)
}

function add(mentor) {
  if (props.disabled || props.modelValue.includes(mentor.id) || props.modelValue.length >= props.max) return
  if (isDisabledLab(mentor.labId)) return
  emit('update:modelValue', [...props.modelValue, mentor.id])
}

function remove(mentorId) {
  if (props.disabled) return
  emit('update:modelValue', props.modelValue.filter((id) => id !== mentorId))
}

function isSelected(mentorId) {
  return props.modelValue.includes(mentorId)
}
</script>

<template>
  <div class="card card-pad">
    <div class="section-head">
      <div class="section-title">平行意向清单</div>
      <span class="pill" :class="modelValue.length ? 'pill--blue' : 'pill--gray'">{{ modelValue.length }} / {{ max }}</span>
    </div>
    <p class="small muted" style="margin-top: -6px">平行意向不设先后顺序，最多选择 {{ max }} 位导师。</p>

    <div v-if="selectedMentors.length" class="stack mt-12" style="gap: 8px">
      <div v-for="(mentor, index) in selectedMentors" :key="mentor.id" class="chip" style="justify-content: space-between">
        <span class="flex">
          <span class="badge-no">{{ index + 1 }}</span>
          <span><strong>{{ mentor.name }}</strong> <span class="muted tiny">{{ mentor.title }} · {{ mentor.labName }}</span></span>
        </span>
        <button v-if="!disabled" class="remove" type="button" title="移除" @click="remove(mentor.id)">×</button>
      </div>
    </div>
    <div v-else class="notice notice--info mt-12">
      <Icon name="spark" :size="15" />
      <span>还没有选择导师，从下方列表中添加你感兴趣的方向。</span>
    </div>

    <div class="field mt-16">
      <div style="position: relative">
        <Icon name="search" :size="15" style="position: absolute; left: 12px; top: 10px; color: var(--muted-2)" />
        <input v-model="keyword" class="input" style="padding-left: 34px" placeholder="搜索实训室、导师或研究方向" />
      </div>
    </div>

    <div class="stack mt-12" style="gap: 14px; max-height: 520px; overflow: auto; padding-right: 2px">
      <div v-for="lab in visibleLabs" :key="lab.id">
        <div class="flex" style="justify-content: space-between; gap: 8px">
          <div>
            <strong style="font-size: 13px">{{ lab.name }}</strong>
            <span class="muted tiny"> · {{ lab.direction }}</span>
          </div>
          <span v-if="isDisabledLab(lab.id)" class="pill pill--red">已拒绝调剂</span>
          <span v-else class="pill pill--gray">余 {{ lab.remainingQuota ?? '—' }}</span>
        </div>
        <div class="mt-8" style="border: 1px solid var(--line); border-radius: 12px; overflow: hidden">
          <div
            v-for="mentor in lab.mentors"
            :key="mentor.id"
            class="flex mentor-row"
            style="padding: 10px 12px; border-bottom: 1px solid #eef2f8; gap: 10px"
          >
            <div style="min-width: 0; flex: 1">
              <div><strong>{{ mentor.name }}</strong> <span class="muted tiny">{{ mentor.title }}</span></div>
              <div class="tiny muted">{{ mentor.researchDirection }}</div>
            </div>
            <span class="tiny muted nowrap">名额 {{ mentor.quota }}</span>
            <button
              class="btn btn-sm"
              :class="isSelected(mentor.id) ? 'btn-ghost' : 'btn-soft'"
              type="button"
              :disabled="disabled || isSelected(mentor.id) || modelValue.length >= max || isDisabledLab(lab.id)"
              @click="add(mentor)"
            >
              {{ isSelected(mentor.id) ? '已选' : '加入' }}
            </button>
          </div>
        </div>
      </div>
      <EmptyState v-if="!visibleLabs.length" title="没有匹配的导师" desc="换个关键词试试。" icon="search" />
    </div>
  </div>
</template>
