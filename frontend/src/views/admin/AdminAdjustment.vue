<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const actionLoading = ref(false)
const workspace = ref(null)
const mentors = ref([])
const autoModal = reactive({ open: false })
const manualModal = reactive({ open: false, student: null })
const talkModal = reactive({ open: false, student: null, reason: '学生不接受调剂，安排谈话安置' })
const manualForm = reactive({ toMentorId: null, reason: '' })

const batch = computed(() => workspace.value?.batch || {})
const unadmitted = computed(() => workspace.value?.unadmitted || [])
const records = computed(() => workspace.value?.records || [])
const canAdjust = computed(() => batch.value.status === 'ADJUSTMENT')
const internalCount = computed(() => records.value.filter((item) => item.type === 'INTERNAL_ADJUST').length)
const crossCount = computed(() => records.value.filter((item) => item.type === 'CROSS_ADJUST').length)

const mentorOptions = computed(() => mentors.value.map((mentor) => {
  const used = Math.max(mentor.offeredCount || 0, mentor.confirmedCount || 0)
  return { ...mentor, used, remaining: Math.max(0, (mentor.quota || 0) - used) }
}))

async function load() {
  loading.value = true
  try {
    const [space, mentorPage] = await Promise.all([
      adminApi.adjustmentWorkspace(),
      adminApi.mentors({ page: 1, size: 100 })
    ])
    workspace.value = space
    mentors.value = mentorPage.records
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openManual(row) {
  manualModal.student = row
  manualForm.toMentorId = mentorOptions.value.find((mentor) => mentor.remaining > 0)?.id || null
  manualForm.reason = '按学生调剂授权与剩余名额统筹分配'
  manualModal.open = true
}

async function autoAdjust() {
  actionLoading.value = true
  try {
    const result = await adminApi.autoAdjust()
    notify(`统筹调剂完成：成功 ${result.assignedCount} 人，谈话安置 ${result.talkedCount} 人`, 'success')
    autoModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function manualAdjust() {
  actionLoading.value = true
  try {
    await adminApi.manualAdjust({
      studentId: manualModal.student.application.studentId,
      toMentorId: Number(manualForm.toMentorId),
      reason: manualForm.reason
    })
    notify('调剂已办理', 'success')
    manualModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function talk() {
  actionLoading.value = true
  try {
    await adminApi.talk(talkModal.student.application.studentId, talkModal.reason)
    notify('已标记谈话安置', 'success')
    talkModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <PageHeader title="统筹调剂" desc="按“实训室内部 → 跨实训室 → 谈话安置”的优先级，在学生授权范围内完成兜底分配。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <button class="btn btn-primary" type="button" :disabled="!canAdjust" @click="autoModal.open = true">
        <Icon name="target" :size="15" /> 一键统筹调剂
      </button>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载调剂数据…</div></div>

  <template v-else-if="workspace">
    <div v-if="!canAdjust" class="notice notice--info">
      <Icon name="eye" :size="16" /><span>当前批次不在「统筹调剂」阶段，调剂操作为只读。</span>
    </div>

    <div class="stat-grid">
      <StatCard label="待调剂学生" :value="unadmitted.length" hint="已提交报名且未录取" tint="#f6f2cf" />
      <StatCard label="内部调剂" :value="internalCount" hint="同实训室调整导师" tint="#fbe3ea" />
      <StatCard label="跨室调剂" :value="crossCount" hint="需学生明确授权" tint="#e6f2d8" />
      <StatCard label="调剂记录" :value="records.length" hint="含谈话安置" tint="#fbe9ee" />
    </div>

    <div class="notice notice--warn">
      <Icon name="shield" :size="16" />
      <span>系统会校验学生调剂授权、明确拒绝的实训室、导师名额与实训室总名额。调剂成功后写入最终录取与操作审计。</span>
    </div>

    <section class="card card-pad">
      <div class="section-head"><div class="section-title">待调剂学生</div><span class="pill pill--gray">{{ unadmitted.length }} 人</span></div>
      <div v-if="unadmitted.length" class="stack">
        <div v-for="row in unadmitted" :key="row.application.id" class="offer-card">
          <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
            <div>
              <strong>{{ row.application.studentName }}</strong>
              <span class="muted tiny"> · {{ row.application.studentNo }} · {{ row.application.major }}</span>
              <div class="tiny muted">
                调剂意愿：{{ { INTERNAL: '仅内部', CROSS: '接受跨室', NONE: '不接受' }[row.application.adjustmentPreference] }}
                <span v-if="row.excludedLabIds.length"> · 拒绝 {{ row.excludedLabIds.length }} 个实训室</span>
              </div>
            </div>
            <div class="button-row">
              <button class="btn btn-soft btn-sm" type="button" :disabled="!canAdjust" @click="openManual(row)">手动调剂</button>
              <button class="btn btn-ghost btn-sm" type="button" :disabled="!canAdjust" @click="talkModal.student = row; talkModal.open = true">谈话安置</button>
            </div>
          </div>
          <div class="flex flex-wrap" style="gap: 6px">
            <span v-for="intent in row.intents" :key="intent.id" class="chip">{{ intent.mentorName }} · {{ intent.labName }}</span>
          </div>
        </div>
      </div>
      <EmptyState v-else title="没有待调剂学生" desc="所有报名学生均已录取，或尚未完成补录流程。" icon="check" />
    </section>

    <section class="card card-pad">
      <div class="section-head"><div class="section-title">调剂记录</div><span class="pill pill--gray">{{ records.length }} 条</span></div>
      <div v-if="records.length" class="table-wrap">
        <table class="table">
          <thead><tr><th>学生</th><th>类型</th><th>原实训室</th><th>去向</th><th>原因</th><th>时间</th></tr></thead>
          <tbody>
            <tr v-for="record in records" :key="record.id">
              <td><strong>{{ record.studentName }}</strong><div class="tiny muted">{{ record.studentNo }}</div></td>
              <td><span class="pill" :class="record.type === 'TALK' ? 'pill--gray' : 'pill--teal'">{{ resolveMeta(assignmentSource, record.type).label }}</span></td>
              <td>{{ record.fromLabName || '—' }}</td>
              <td>{{ record.toLabName ? `${record.toLabName} · ${record.toMentorName}` : '谈话安置' }}</td>
              <td><span class="tiny muted">{{ record.reason }}</span></td>
              <td>{{ fmt(record.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <EmptyState v-else title="暂无调剂记录" desc="执行调剂后记录会出现在这里。" icon="target" />
    </section>
  </template>

  <ModalDialog :open="autoModal.open" title="执行一键统筹调剂？" subtitle="按优先级自动为未录取学生分配剩余名额" @close="autoModal.open = false">
    <div class="stack">
      <div class="notice notice--warn">
        <Icon name="alert" :size="16" />
        <span>系统会依次尝试实训室内部调剂、跨实训室调剂；仍无法分配的学生将生成谈话安置记录。操作写入审计日志。</span>
      </div>
      <div class="kv">
        <dt>待处理</dt><dd>{{ unadmitted.length }} 人</dd>
        <dt>目标批次</dt><dd>{{ batch.name }}</dd>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="autoModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading" @click="autoAdjust">{{ actionLoading ? '执行中…' : '确认执行' }}</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="manualModal.open" title="手动调剂" :subtitle="manualModal.student?.application?.studentName" @close="manualModal.open = false">
    <div v-if="manualModal.student" class="stack">
      <div class="field">
        <label class="label">目标导师</label>
        <select v-model="manualForm.toMentorId" class="select">
          <option v-for="mentor in mentorOptions" :key="mentor.id" :value="mentor.id" :disabled="mentor.remaining <= 0">
            {{ mentor.name }} · {{ mentor.labName }}（剩余 {{ mentor.remaining }}）
          </option>
        </select>
        <div class="hint">只显示当前批次名额情况；名额已满的导师不可选。</div>
      </div>
      <div class="field">
        <label class="label">调剂原因</label>
        <textarea v-model="manualForm.reason" class="textarea" />
      </div>
      <div class="notice notice--info">
        <Icon name="shield" :size="15" />
        <span>系统仍会校验学生授权与拒绝实训室；不符合条件会拒绝。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="manualModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading || !manualForm.toMentorId" @click="manualAdjust">确认调剂</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="talkModal.open" title="标记谈话安置" :subtitle="talkModal.student?.application?.studentName" @close="talkModal.open = false">
    <div class="field">
      <label class="label">原因</label>
      <textarea v-model="talkModal.reason" class="textarea" />
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="talkModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading" @click="talk">确认标记</button>
    </template>
  </ModalDialog>
</template>
