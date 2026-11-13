<script setup>
import { computed, onMounted, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, offerSource, offerStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const students = ref([])
const offers = ref([])
const dashboard = ref(null)

const batch = computed(() => dashboard.value?.batch || {})
const mentor = computed(() => dashboard.value?.mentor || {})
const activeOffers = computed(() => offers.value.filter((offer) => offer.status === 'OFFERED'))
const confirmedOffers = computed(() => offers.value.filter((offer) => offer.status === 'CONFIRMED'))

async function load() {
  loading.value = true
  try {
    const [list, offerList, dash] = await Promise.all([
      mentorApi.students(),
      mentorApi.offers(),
      mentorApi.dashboard()
    ])
    students.value = list
    offers.value = offerList
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
  <PageHeader title="我的学生" desc="最终确认进入你名下培养的学生名单。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
    </template>
  </PageHeader>

  <div class="stat-grid">
    <StatCard label="最终学生" :value="students.length" :hint="`招生名额 ${mentor.quota || 0} 人`" tint="#e8f7f1" />
    <StatCard label="待确认拟录取" :value="activeOffers.length" hint="等待学生确认" tint="#fff4e2" />
    <StatCard label="已确认意向" :value="confirmedOffers.length" hint="含补录/递补" tint="#f8edf1" />
    <StatCard label="剩余名额" :value="Math.max(0, (mentor.quota || 0) - students.length)" hint="最终名单口径" tint="#f7f0f1" />
  </div>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载学生名单…</div></div>

  <div v-else class="card">
    <div v-if="students.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>学生</th><th>专业 / 班级</th><th>录取方式</th><th>确认时间</th><th>状态</th></tr></thead>
        <tbody>
          <tr v-for="student in students" :key="student.id">
            <td><strong>{{ student.studentName }}</strong><div class="tiny muted">{{ student.studentNo }}</div></td>
            <td>{{ student.major }}<div class="tiny muted">{{ student.className }}</div></td>
            <td><span class="pill pill--blue">{{ resolveMeta(assignmentSource, student.source).label }}</span></td>
            <td>{{ fmt(student.confirmedAt) }}</td>
            <td><span class="pill pill--green"><span class="dot" />已录取</span></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无最终学生" desc="学生确认拟录取后，会出现在最终名单中。" icon="users" />
  </div>

  <div v-if="activeOffers.length" class="card card-pad">
    <div class="section-head"><div class="section-title">待学生确认</div></div>
    <div class="stack" style="gap: 8px">
      <div v-for="offer in activeOffers" :key="offer.id" class="offer-card" style="padding: 12px">
        <div class="flex" style="justify-content: space-between">
          <div><strong>{{ offer.studentName }}</strong><span class="muted tiny"> · {{ offer.studentNo }}</span></div>
          <StatusPill :value="offer.status" :map="offerStatus" />
        </div>
        <div class="tiny muted">{{ resolveMeta(offerSource, offer.source).label }} · 发放于 {{ fmt(offer.createdAt) }}</div>
      </div>
    </div>
  </div>
</template>
