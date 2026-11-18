<script setup>
import { computed, onMounted, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, interviewConclusion, interviewStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const interviews = ref([])
const dashboard = ref(null)

const batch = computed(() => dashboard.value?.batch || {})
const completed = computed(() => interviews.value.filter((item) => item.status === 'COMPLETED'))
const pending = computed(() => interviews.value.filter((item) => item.status !== 'COMPLETED'))
const avgScore = computed(() => {
  const scores = completed.value.map((item) => item.score).filter((score) => score != null)
  if (!scores.length) return '—'
  return (scores.reduce((sum, score) => sum + score, 0) / scores.length).toFixed(1)
})

async function load() {
  loading.value = true
  try {
    const [list, dash] = await Promise.all([mentorApi.interviews(), mentorApi.dashboard()])
    interviews.value = list
    dashboard.value = dash
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHeader title="面试考核" desc="本实训室统一组织的面试记录，所有导师共享；可在报名学生页录入或修改。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/mentor/applicants" class="btn btn-primary"><Icon name="users" :size="15" /> 去录入面试</router-link>
    </template>
  </PageHeader>

  <div class="stat-grid">
    <StatCard label="面试总数" :value="interviews.length" hint="本实训室" tint="#fbe3ea" />
    <StatCard label="已完成" :value="completed.length" hint="已录入结论与评分" tint="#e6f2d8" />
    <StatCard label="待完成" :value="pending.length" hint="待面试或待录入" tint="#f6f2cf" />
    <StatCard label="平均分" :value="avgScore" hint="已完成面试" tint="#fbe9ee" />
  </div>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载面试记录…</div></div>

  <div v-else class="card">
    <div v-if="interviews.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>学生</th><th>专业 / 班级</th><th>面试时间</th><th>地点</th><th>评分</th><th>结论</th><th>状态</th><th>评语</th></tr></thead>
        <tbody>
          <tr v-for="item in interviews" :key="item.id">
            <td><strong>{{ item.studentName }}</strong><div class="tiny muted">{{ item.studentNo }}</div></td>
            <td>{{ item.major }}<div class="tiny muted">{{ item.className }}</div></td>
            <td>{{ fmt(item.scheduledAt) }}</td>
            <td>{{ item.location || '—' }}</td>
            <td><strong>{{ item.score ?? '—' }}</strong></td>
            <td><StatusPill v-if="item.conclusion" :value="item.conclusion" :map="interviewConclusion" /></td>
            <td><StatusPill :value="item.status" :map="interviewStatus" /></td>
            <td><span class="tiny muted" style="display: block; max-width: 260px">{{ item.comment || '—' }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无面试记录" desc="前往报名学生页，为候选人安排并记录面试。" icon="calendar">
      <router-link to="/mentor/applicants" class="btn btn-primary btn-sm">前往报名学生</router-link>
    </EmptyState>
  </div>

  <div class="notice notice--info">
    <Icon name="shield" :size="16" />
    <span>为保证独立评价，导师端不展示学生的其他平行意向导师信息。面试记录与初选结论会写入操作审计。</span>
  </div>
</template>
