<script setup>
import { computed, onMounted, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, offerSource, offerStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, quotaPercent } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StepFlow from '../../components/StepFlow.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const data = ref(null)
const offers = ref([])

const batch = computed(() => data.value?.batch || {})
const mentor = computed(() => data.value?.mentor || {})
const lab = computed(() => data.value?.lab || {})
const stats = computed(() => data.value?.stats || {})
const pending = computed(() => data.value?.pendingApplicants || [])
const activeOffers = computed(() => offers.value.filter((offer) => offer.status === 'OFFERED'))
const confirmedOffers = computed(() => offers.value.filter((offer) => offer.status === 'CONFIRMED'))
const used = computed(() => Math.max(stats.value.offerCount || 0, stats.value.confirmedCount || 0))

async function load() {
  loading.value = true
  try {
    const [dashboard, offerList] = await Promise.all([mentorApi.dashboard(), mentorApi.offers()])
    data.value = dashboard
    offers.value = offerList
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHeader title="导师工作台" desc="查看报名情况、面试进度、名额使用与初选待办。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/mentor/applicants" class="btn btn-primary"><Icon name="users" :size="15" /> 报名学生</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载工作台…</div></div>

  <template v-else-if="data">
    <section class="card card-pad" style="background: linear-gradient(120deg, #f7f9ff, #fff 60%, #f7f4ff)">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 18px">
        <div>
          <div class="flex"><span class="pill pill--violet">{{ lab.name }}</span><span class="pill pill--gray">{{ mentor.title }}</span></div>
          <h2 class="mt-12">{{ mentor.name }} 老师，{{ resolveMeta(batchStatus, batch.status).label }}</h2>
          <p class="muted small mt-4">{{ mentor.researchDirection }} · 招生名额 {{ mentor.quota }} 人 · 已用 {{ used }} 人</p>
        </div>
        <div style="min-width: 220px">
          <div class="flex" style="justify-content: space-between; font-size: 12px; color: var(--muted)">
            <span>名额使用</span><span>{{ used }} / {{ mentor.quota }}</span>
          </div>
          <div class="quota-bar mt-4"><span :style="{ width: `${quotaPercent(used, mentor.quota)}%` }" /></div>
          <div class="tiny muted mt-4">剩余 {{ stats.remainingQuota }} 个名额</div>
        </div>
      </div>
    </section>

    <div class="stat-grid">
      <StatCard label="报名学生" :value="stats.applicantCount" hint="报名你的学生数" tint="#e9efff" />
      <StatCard label="待处理意向" :value="stats.pendingCount" hint="需要给出初选结论" tint="#fff0e6" />
      <StatCard label="已完成面试" :value="stats.interviewCount" hint="本实训室统一面试" tint="#f0ebff" />
      <StatCard label="候补 / 拟录取" :value="`${stats.waitlistCount} / ${stats.offerCount}`" hint="拟录取待学生确认" tint="#e5f8f4" />
    </div>

    <div class="split">
      <div class="stack">
        <section class="card card-pad">
          <div class="section-head">
            <div class="section-title">待处理报名</div>
            <router-link to="/mentor/applicants" class="link-btn small">查看全部</router-link>
          </div>
          <div v-if="pending.length" class="stack" style="gap: 9px">
            <div v-for="item in pending" :key="item.intentId" class="offer-card" style="padding: 12px 14px">
              <div class="flex" style="justify-content: space-between; gap: 10px">
                <div>
                  <strong>{{ item.studentName }}</strong>
                  <span class="muted tiny"> · {{ item.studentNo }} · {{ item.major }}</span>
                  <div class="tiny muted">面试：{{ item.interview ? (item.interview.status === 'COMPLETED' ? `已完成（${item.interview.score ?? '—'} 分）` : '已安排') : '尚未安排' }}</div>
                </div>
                <router-link to="/mentor/applicants" class="btn btn-soft btn-sm">去考核</router-link>
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无待处理报名" desc="新的报名或待初选学生会出现在这里。" icon="users" />
        </section>

        <section class="card card-pad">
          <div class="section-head"><div class="section-title">选拔进度</div></div>
          <StepFlow :stages="data.stages" />
        </section>
      </div>

      <div class="stack">
        <section class="card card-pad">
          <div class="section-head"><div class="section-title">我的拟录取</div><span class="pill pill--gray">{{ offers.length }} 条</span></div>
          <div v-if="offers.length" class="stack" style="gap: 8px">
            <div v-for="offer in offers.slice(0, 5)" :key="offer.id" class="offer-card" style="padding: 12px">
              <div class="flex" style="justify-content: space-between">
                <strong class="small">{{ offer.studentName }}</strong>
                <StatusPill :value="offer.status" :map="offerStatus" />
              </div>
              <div class="tiny muted">{{ offer.studentNo }} · {{ resolveMeta(offerSource, offer.source).label }} · {{ fmt(offer.createdAt) }}</div>
            </div>
          </div>
          <EmptyState v-else title="暂无拟录取记录" desc="完成面试并提交初选建议后生成。" icon="award" />
        </section>

        <section class="card card-pad">
          <div class="section-title">快捷入口</div>
          <div class="stack mt-12" style="gap: 8px">
            <router-link to="/mentor/interviews" class="btn btn-ghost btn-block" style="justify-content: space-between">面试考核 <Icon name="calendar" :size="15" /></router-link>
            <router-link to="/mentor/decisions" class="btn btn-ghost btn-block" style="justify-content: space-between">初选建议 <Icon name="check" :size="15" /></router-link>
            <router-link to="/mentor/students" class="btn btn-ghost btn-block" style="justify-content: space-between">我的学生 <Icon name="award" :size="15" /></router-link>
          </div>
        </section>

        <section class="card card-pad">
          <div class="section-title">本实训室</div>
          <p class="small muted mt-8">{{ lab.intro }}</p>
          <div class="kv mt-12">
            <dt>研究方向</dt><dd>{{ lab.direction }}</dd>
            <dt>总名额</dt><dd>{{ lab.totalQuota }} 人</dd>
            <dt>联系方式</dt><dd>{{ lab.contact || '—' }}</dd>
          </div>
        </section>
      </div>
    </div>
  </template>
</template>
