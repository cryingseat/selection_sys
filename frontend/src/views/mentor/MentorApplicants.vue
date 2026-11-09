<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  applicationStatus,
  batchStatus,
  intentStatus,
  interviewConclusion,
  interviewStatus,
  offerStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt, fromInput, toInput } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const records = ref([])
const total = ref(0)
const batch = ref({})
const page = ref(1)
const size = ref(10)
const statusFilter = ref('')
const keyword = ref('')

const detail = reactive({ open: false, loading: false, data: null })
const interviewForm = reactive({ scheduledAt: '', location: '', score: null, comment: '', conclusion: 'PENDING' })
const decisionForm = reactive({ decision: 'OFFER', waitlistRank: 1, comment: '' })
const saving = ref(false)

const canEvaluate = computed(() => batch.value.status === 'INTERVIEW')
const interviewCompleted = computed(() => detail.data?.interview?.status === 'COMPLETED')
const activeOffer = computed(() => {
  const offer = detail.data?.offer
  return offer && ['OFFERED', 'CONFIRMED'].includes(offer.status) ? offer : null
})

async function load() {
  loading.value = true
  try {
    const data = await mentorApi.applicants({
      status: statusFilter.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    records.value = data.records
    total.value = data.total
    batch.value = data.batch
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function setFilter(value) {
  statusFilter.value = value
  page.value = 1
  load()
}

function search() {
  page.value = 1
  load()
}

function changePage(delta) {
  const next = page.value + delta
  if (next < 1 || (next - 1) * size.value >= total.value) return
  page.value = next
  load()
}

async function openDetail(applicationId) {
  detail.open = true
  detail.loading = true
  detail.data = null
  try {
    const data = await mentorApi.applicant(applicationId)
    detail.data = data
    const interview = data.interview
    interviewForm.scheduledAt = toInput(interview?.scheduledAt) || toInput(new Date().toISOString())
    interviewForm.location = interview?.location || ''
    interviewForm.score = interview?.score ?? null
    interviewForm.comment = interview?.comment || ''
    interviewForm.conclusion = interview?.conclusion || 'PENDING'
    decisionForm.decision = 'OFFER'
    decisionForm.waitlistRank = 1
    decisionForm.comment = data.intent?.comment || ''
  } catch (err) {
    notify(err.message, 'error')
    detail.open = false
  } finally {
    detail.loading = false
  }
}

async function saveInterview() {
  if (!detail.data) return
  saving.value = true
  try {
    await mentorApi.saveInterview({
      applicationId: detail.data.application.id,
      scheduledAt: fromInput(interviewForm.scheduledAt),
      location: interviewForm.location,
      score: interviewForm.score === '' || interviewForm.score == null ? null : Number(interviewForm.score),
      comment: interviewForm.comment,
      conclusion: interviewForm.conclusion
    })
    notify('面试记录已保存', 'success')
    await openDetail(detail.data.application.id)
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function submitDecision() {
  if (!detail.data) return
  saving.value = true
  try {
    await mentorApi.decide({
      intentId: detail.data.intent.id,
      decision: decisionForm.decision,
      waitlistRank: decisionForm.decision === 'WAITLIST' ? Number(decisionForm.waitlistRank) : null,
      comment: decisionForm.comment,
      reason: '导师完成面试后提交初选意见'
    })
    notify('初选结论已提交', 'success')
    await openDetail(detail.data.application.id)
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="报名学生" desc="查看报名你的学生信息、记录统一面试结果，并提交拟录取、候补或不录取建议。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/mentor/decisions" class="btn btn-ghost">初选建议总览</router-link>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 14px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
      <div class="button-row">
        <button class="btn btn-sm" :class="statusFilter === '' ? 'btn-primary' : 'btn-ghost'" @click="setFilter('')">全部</button>
        <button class="btn btn-sm" :class="statusFilter === 'PENDING' ? 'btn-primary' : 'btn-ghost'" @click="setFilter('PENDING')">待处理</button>
        <button class="btn btn-sm" :class="statusFilter === 'WAITLISTED' ? 'btn-primary' : 'btn-ghost'" @click="setFilter('WAITLISTED')">候补</button>
        <button class="btn btn-sm" :class="statusFilter === 'REJECTED' ? 'btn-primary' : 'btn-ghost'" @click="setFilter('REJECTED')">未录取</button>
      </div>
      <div class="flex" style="gap: 8px">
        <div style="position: relative">
          <Icon name="search" :size="15" style="position: absolute; left: 11px; top: 9px; color: var(--muted-2)" />
          <input v-model="keyword" class="input" style="padding-left: 32px; width: 230px" placeholder="姓名 / 学号 / 专业" @keyup.enter="search" />
        </div>
        <button class="btn btn-soft btn-sm" @click="search">搜索</button>
      </div>
    </div>
  </div>

  <div v-if="!canEvaluate" class="notice notice--info">
    <Icon name="eye" :size="16" />
    <span>当前处于「{{ resolveMeta(batchStatus, batch.status).label }}」阶段。面试记录与初选建议在「面试与初选」阶段开放。</span>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载报名学生…</div>
    <div v-else-if="records.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead>
          <tr><th>学生</th><th>专业 / 班级</th><th>意向状态</th><th>面试</th><th>其他意向</th><th class="right">操作</th></tr>
        </thead>
        <tbody>
          <tr v-for="row in records" :key="row.intent.id">
            <td>
              <strong>{{ row.intent.studentName }}</strong>
              <div class="tiny muted">{{ row.intent.studentNo }}</div>
            </td>
            <td>
              {{ row.intent.major }}
              <div class="tiny muted">{{ row.intent.className }}</div>
            </td>
            <td><StatusPill :value="row.intent.status" :map="intentStatus" /></td>
            <td>
              <template v-if="row.interview">
                <StatusPill :value="row.interview.status" :map="interviewStatus" />
                <div class="tiny muted">{{ row.interview.score != null ? `${row.interview.score} 分` : '未评分' }}</div>
              </template>
              <span v-else class="tiny muted">未安排</span>
            </td>
            <td><span class="pill pill--gray">另有 {{ row.otherIntentCount }} 个</span></td>
            <td class="right">
              <button class="btn btn-soft btn-sm" type="button" @click="openDetail(row.application.id)">查看 / 考核</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无报名学生" desc="学生提交意向后会出现在这里。" icon="users" />
    <div v-if="total > size" class="flex" style="justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line)">
      <span class="small muted">共 {{ total }} 条 · 第 {{ page }} 页</span>
      <div class="button-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="changePage(-1)">上一页</button>
        <button class="btn btn-ghost btn-sm" :disabled="page * size >= total" @click="changePage(1)">下一页</button>
      </div>
    </div>
  </div>

  <ModalDialog :open="detail.open" title="学生详情与考核" subtitle="导师端仅展示你本人的意向，不展示学生的其他平行导师信息" wide @close="detail.open = false">
    <div v-if="detail.loading" class="loading-block"><div class="spinner" />加载中…</div>
    <div v-else-if="detail.data" class="stack">
      <section class="card card-pad" style="box-shadow: none; background: var(--surface-2)">
        <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
          <div>
            <h3>{{ detail.data.application.studentName }}</h3>
            <div class="small muted">{{ detail.data.application.studentNo }} · {{ detail.data.application.major }} · {{ detail.data.application.className }} · {{ detail.data.application.grade }} 级</div>
          </div>
          <div class="flex">
            <StatusPill :value="detail.data.application.status" :map="applicationStatus" />
            <StatusPill :value="detail.data.intent.status" :map="intentStatus" />
          </div>
        </div>
        <div class="kv mt-12">
          <dt>联系电话</dt><dd>{{ detail.data.application.phone || '—' }}</dd>
          <dt>邮箱</dt><dd>{{ detail.data.application.email || '—' }}</dd>
          <dt>调剂意愿</dt><dd>{{ { INTERNAL: '接受实训室内调剂', CROSS: '接受跨实训室调剂', NONE: '不接受调剂' }[detail.data.application.adjustmentPreference] }}</dd>
          <dt>其他意向</dt><dd>另有 {{ detail.data.otherIntentCount }} 个平行意向（不展示具体导师）</dd>
        </div>
      </section>

      <section class="card card-pad" style="box-shadow: none">
        <div class="section-title">报名材料</div>
        <div class="stack mt-12" style="gap: 10px">
          <div><div class="label">个人简介</div><p class="small mt-4">{{ detail.data.application.intro || '—' }}</p></div>
          <div><div class="label">技术基础</div><p class="small mt-4">{{ detail.data.application.skills || '—' }}</p></div>
          <div><div class="label">项目经历</div><p class="small mt-4">{{ detail.data.application.projectExperience || '—' }}</p></div>
        </div>
      </section>

      <section class="card card-pad" style="box-shadow: none">
        <div class="card-head">
          <div>
            <div class="section-title">统一面试记录</div>
            <div class="card-sub">同一实训室共享面试记录；提交结论必须基于已完成的面试。</div>
          </div>
          <StatusPill v-if="detail.data.interview" :value="detail.data.interview.status" :map="interviewStatus" />
        </div>
        <div class="form-grid">
          <div class="field">
            <label class="label">面试时间</label>
            <input v-model="interviewForm.scheduledAt" class="input" type="datetime-local" :disabled="!canEvaluate" />
          </div>
          <div class="field">
            <label class="label">地点</label>
            <input v-model="interviewForm.location" class="input" placeholder="例如：信息楼 A301" :disabled="!canEvaluate" />
          </div>
          <div class="field">
            <label class="label">评分（0—100）</label>
            <input v-model.number="interviewForm.score" class="input" type="number" min="0" max="100" :disabled="!canEvaluate" />
          </div>
          <div class="field">
            <label class="label">考核结论</label>
            <select v-model="interviewForm.conclusion" class="select" :disabled="!canEvaluate">
              <option value="PENDING">待考核 / 仅安排</option>
              <option value="PASS">通过</option>
              <option value="FAIL">不通过</option>
            </select>
          </div>
          <div class="field span-2">
            <label class="label">面试评语</label>
            <textarea v-model="interviewForm.comment" class="textarea" :disabled="!canEvaluate" placeholder="记录专业基础、项目经历、学习态度与投入时间等。" />
          </div>
        </div>
        <div class="button-row mt-12">
          <button class="btn btn-primary btn-sm" type="button" :disabled="!canEvaluate || saving" @click="saveInterview">
            <Icon name="check" :size="14" /> 保存面试记录
          </button>
          <span v-if="!canEvaluate" class="tiny muted">当前阶段不可编辑</span>
          <span v-else-if="interviewForm.conclusion !== 'PENDING' && !interviewForm.score" class="tiny" style="color: var(--orange)">提交考核结论时必须填写评分</span>
        </div>
      </section>

      <section class="card card-pad" style="box-shadow: none">
        <div class="card-head">
          <div>
            <div class="section-title">初选结论</div>
            <div class="card-sub">拟录取受导师与实训室名额双重限制；候补需设置顺位。</div>
          </div>
          <span class="pill pill--gray">名额 {{ detail.data.intent.mentorQuota }} 人</span>
        </div>

        <div v-if="activeOffer" class="notice notice--success">
          <Icon name="check" :size="16" />
          <span>已发放拟录取（{{ resolveMeta(offerStatus, activeOffer.status).label }}），不能重复提交。等待学生确认。</span>
        </div>

        <template v-else>
          <div class="radio-cards">
            <label class="radio-card" :class="{ active: decisionForm.decision === 'OFFER' }">
              <input v-model="decisionForm.decision" type="radio" value="OFFER" hidden :disabled="!canEvaluate || !interviewCompleted" />
              <strong>拟录取</strong><span>占用 1 个招生名额</span>
            </label>
            <label class="radio-card" :class="{ active: decisionForm.decision === 'WAITLIST' }">
              <input v-model="decisionForm.decision" type="radio" value="WAITLIST" hidden :disabled="!canEvaluate || !interviewCompleted" />
              <strong>候补</strong><span>按顺位参与补录递补</span>
            </label>
            <label class="radio-card" :class="{ active: decisionForm.decision === 'REJECT' }">
              <input v-model="decisionForm.decision" type="radio" value="REJECT" hidden :disabled="!canEvaluate || !interviewCompleted" />
              <strong>不录取</strong><span>释放该意向</span>
            </label>
          </div>

          <div v-if="decisionForm.decision === 'WAITLIST'" class="field mt-12">
            <label class="label">候补顺位</label>
            <input v-model.number="decisionForm.waitlistRank" class="input" type="number" min="1" max="99" style="max-width: 160px" />
            <div class="hint">同一导师下顺位不能重复，数字越小越优先。</div>
          </div>
          <div class="field mt-12">
            <label class="label">初选意见</label>
            <textarea v-model="decisionForm.comment" class="textarea" placeholder="可填写推荐理由、候补说明或不录取原因。" :disabled="!canEvaluate || !interviewCompleted" />
          </div>
          <div v-if="!interviewCompleted" class="notice notice--warn mt-12">
            <Icon name="alert" :size="15" /><span>请先保存已完成的面试记录，再提交初选结论。</span>
          </div>
          <button class="btn btn-primary btn-block mt-12" type="button" :disabled="!canEvaluate || !interviewCompleted || saving" @click="submitDecision">
            <Icon name="send" :size="15" /> 提交初选结论
          </button>
        </template>
      </section>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="detail.open = false">关闭</button>
    </template>
  </ModalDialog>
</template>
