<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  batchStatus,
  intentStatus,
  interviewConclusion,
  interviewStatus,
  offerStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const data = ref(null)
const filter = ref('')
const modal = reactive({ open: false, row: null })
const form = reactive({ decision: 'OFFER', waitlistRank: 1, comment: '' })
const saving = ref(false)

const batch = computed(() => data.value?.batch || {})
const rows = computed(() => data.value?.rows || [])
const quota = computed(() => data.value?.quota || 0)
const activeOffers = computed(() => data.value?.activeOffers || 0)
const canEvaluate = computed(() => batch.value.status === 'INTERVIEW')
const filtered = computed(() => {
  if (!filter.value) return rows.value
  return rows.value.filter((row) => row.intent.status === filter.value)
})
const pendingCount = computed(() => rows.value.filter((row) => row.intent.status === 'PENDING').length)
const waitlistCount = computed(() => rows.value.filter((row) => row.intent.status === 'WAITLISTED').length)

async function load() {
  loading.value = true
  try {
    data.value = await mentorApi.decisions()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openDecision(row) {
  modal.row = row
  form.decision = 'OFFER'
  form.waitlistRank = 1
  form.comment = row.intent.comment || ''
  modal.open = true
}

async function submit() {
  saving.value = true
  try {
    await mentorApi.decide({
      intentId: modal.row.intent.id,
      decision: form.decision,
      waitlistRank: form.decision === 'WAITLIST' ? Number(form.waitlistRank) : null,
      comment: form.comment,
      reason: '导师完成面试后提交初选意见'
    })
    notify('初选结论已提交', 'success')
    modal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="初选建议" desc="基于已完成的面试记录，对学生给出拟录取、候补或不录取建议。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/mentor/applicants" class="btn btn-ghost">报名学生</router-link>
    </template>
  </PageHeader>

  <div class="stat-grid">
    <StatCard label="意向总数" :value="rows.length" hint="报名你的学生" tint="#e9efff" />
    <StatCard label="待处理" :value="pendingCount" hint="尚未提交结论" tint="#fff0e6" />
    <StatCard label="候补" :value="waitlistCount" hint="按顺位参与递补" tint="#fff5db" />
    <StatCard label="名额使用" :value="`${activeOffers} / ${quota}`" hint="拟录取 + 已确认" tint="#e5f8f4" />
  </div>

  <div class="card card-pad" style="padding: 13px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
      <div class="button-row">
        <button class="btn btn-sm" :class="filter === '' ? 'btn-primary' : 'btn-ghost'" @click="filter = ''">全部</button>
        <button class="btn btn-sm" :class="filter === 'PENDING' ? 'btn-primary' : 'btn-ghost'" @click="filter = 'PENDING'">待处理</button>
        <button class="btn btn-sm" :class="filter === 'WAITLISTED' ? 'btn-primary' : 'btn-ghost'" @click="filter = 'WAITLISTED'">候补</button>
        <button class="btn btn-sm" :class="filter === 'OFFERED' ? 'btn-primary' : 'btn-ghost'" @click="filter = 'OFFERED'">拟录取</button>
      </div>
      <span class="small muted">名额 {{ quota }} 人 · 已用 {{ activeOffers }} 人</span>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载初选数据…</div>
    <div v-else-if="filtered.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>学生</th><th>专业</th><th>面试</th><th>当前结论</th><th>候补顺位</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="row in filtered" :key="row.intent.id">
            <td><strong>{{ row.intent.studentName }}</strong><div class="tiny muted">{{ row.intent.studentNo }}</div></td>
            <td>{{ row.intent.major }}<div class="tiny muted">{{ row.intent.className }}</div></td>
            <td>
              <template v-if="row.interview">
                <StatusPill :value="row.interview.status" :map="interviewStatus" />
                <div class="tiny muted">{{ row.interview.score != null ? `${row.interview.score} 分` : '未评分' }}</div>
              </template>
              <span v-else class="tiny muted">未面试</span>
            </td>
            <td><StatusPill :value="row.intent.status" :map="intentStatus" /></td>
            <td>{{ row.intent.waitlistRank ?? '—' }}</td>
            <td class="right">
              <button class="btn btn-soft btn-sm" type="button" :disabled="!canEvaluate" @click="openDecision(row)">
                {{ row.intent.status === 'PENDING' ? '提交结论' : '调整结论' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无初选数据" desc="学生提交意向后会出现在这里。" icon="check" />
  </div>

  <div v-if="!canEvaluate" class="notice notice--info">
    <Icon name="eye" :size="16" /><span>当前不在「面试与初选」阶段，初选结论暂不可修改。</span>
  </div>

  <ModalDialog :open="modal.open" title="提交初选结论" :subtitle="modal.row ? `${modal.row.intent.studentName} · ${modal.row.intent.studentNo}` : ''" @close="modal.open = false">
    <div v-if="modal.row" class="stack">
      <div class="offer-card" style="padding: 13px">
        <div class="flex" style="justify-content: space-between">
          <div>
            <strong>{{ modal.row.intent.studentName }}</strong>
            <span class="muted tiny"> · {{ modal.row.intent.major }}</span>
          </div>
          <StatusPill :value="modal.row.intent.status" :map="intentStatus" />
        </div>
        <div class="tiny muted">
          面试：{{ modal.row.interview ? `${modal.row.interview.status === 'COMPLETED' ? '已完成' : '待完成'}，${modal.row.interview.score ?? '—'} 分，${resolveMeta(interviewConclusion, modal.row.interview.conclusion).label}` : '尚未安排' }}
        </div>
      </div>

      <div v-if="!modal.row.interview || modal.row.interview.status !== 'COMPLETED'" class="notice notice--warn">
        <Icon name="alert" :size="15" /><span>该学生尚未完成面试，无法提交拟录取/候补建议。请先在报名学生页录入面试记录。</span>
      </div>

      <div class="radio-cards">
        <label class="radio-card" :class="{ active: form.decision === 'OFFER' }">
          <input v-model="form.decision" type="radio" value="OFFER" hidden />
          <strong>拟录取</strong><span>占用 1 个名额</span>
        </label>
        <label class="radio-card" :class="{ active: form.decision === 'WAITLIST' }">
          <input v-model="form.decision" type="radio" value="WAITLIST" hidden />
          <strong>候补</strong><span>按顺位递补</span>
        </label>
        <label class="radio-card" :class="{ active: form.decision === 'REJECT' }">
          <input v-model="form.decision" type="radio" value="REJECT" hidden />
          <strong>不录取</strong><span>释放该意向</span>
        </label>
      </div>

      <div v-if="form.decision === 'WAITLIST'" class="field">
        <label class="label">候补顺位</label>
        <input v-model.number="form.waitlistRank" class="input" type="number" min="1" max="99" style="max-width: 160px" />
      </div>
      <div class="field">
        <label class="label">意见说明</label>
        <textarea v-model="form.comment" class="textarea" placeholder="推荐理由、候补说明或不录取原因。" />
      </div>
      <div v-if="form.decision === 'OFFER'" class="notice notice--warn">
        <Icon name="alert" :size="15" /><span>当前已用名额 {{ activeOffers }} / {{ quota }}。若名额已满，提交将被拒绝。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button
        class="btn btn-primary"
        type="button"
        :disabled="saving || !modal.row?.interview || modal.row?.interview?.status !== 'COMPLETED'"
        @click="submit"
      >
        {{ saving ? '提交中…' : '确认提交' }}
      </button>
    </template>
  </ModalDialog>
</template>
