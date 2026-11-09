<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, fromInput, toInput } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import StepFlow from '../../components/StepFlow.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const batches = ref([])
const formModal = reactive({ open: false, id: null })
const detailModal = reactive({ open: false, loading: false, data: null })
const saving = ref(false)

const form = reactive({
  name: '', gradeScope: '2023', majorScope: '', description: '', rules: '', status: 'DRAFT',
  lectureAt: '', openDayStart: '', openDayEnd: '', intentStart: '', intentEnd: '',
  interviewStart: '', interviewEnd: '', confirmStart: '', confirmEnd: '',
  supplementStart: '', supplementEnd: '', adjustStart: '', adjustEnd: '',
  publicityStart: '', publicityEnd: ''
})

const dateFields = [
  'lectureAt', 'openDayStart', 'openDayEnd', 'intentStart', 'intentEnd',
  'interviewStart', 'interviewEnd', 'confirmStart', 'confirmEnd',
  'supplementStart', 'supplementEnd', 'adjustStart', 'adjustEnd',
  'publicityStart', 'publicityEnd'
]

const timeline = [
  { label: '实训室开放日', start: 'openDayStart', end: 'openDayEnd' },
  { label: '意向填报', start: 'intentStart', end: 'intentEnd' },
  { label: '导师初选与面试', start: 'interviewStart', end: 'interviewEnd' },
  { label: '拟录取确认', start: 'confirmStart', end: 'confirmEnd' },
  { label: '分轮补录', start: 'supplementStart', end: 'supplementEnd' },
  { label: '统筹调剂', start: 'adjustStart', end: 'adjustEnd' },
  { label: '结果公示', start: 'publicityStart', end: 'publicityEnd' }
]

const canSave = computed(() => form.name.trim().length > 0)

function offset(days, hour = 9) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  date.setHours(hour, 0, 0, 0)
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:00`
}

function resetForm() {
  Object.assign(form, {
    name: '', gradeScope: '2023', majorScope: '', description: '', rules: '', status: 'DRAFT',
    lectureAt: offset(1, 19),
    openDayStart: offset(2), openDayEnd: offset(3, 17),
    intentStart: offset(4, 8), intentEnd: offset(11, 22),
    interviewStart: offset(12), interviewEnd: offset(13, 18),
    confirmStart: offset(14, 8), confirmEnd: offset(15, 12),
    supplementStart: offset(15, 14), supplementEnd: offset(17, 17),
    adjustStart: offset(18), adjustEnd: offset(19, 12),
    publicityStart: offset(20), publicityEnd: offset(21)
  })
}

async function load() {
  loading.value = true
  try {
    batches.value = await adminApi.batches()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate() {
  formModal.id = null
  resetForm()
  formModal.open = true
}

function openEdit(batch) {
  formModal.id = batch.id
  Object.assign(form, {
    name: batch.name || '',
    gradeScope: batch.gradeScope || '',
    majorScope: batch.majorScope || '',
    description: batch.description || '',
    rules: batch.rules || '',
    status: batch.status || 'DRAFT'
  })
  dateFields.forEach((key) => { form[key] = toInput(batch[key]) })
  formModal.open = true
}

async function save() {
  if (!canSave.value) return
  saving.value = true
  try {
    const payload = { ...form }
    dateFields.forEach((key) => { payload[key] = fromInput(form[key]) })
    if (formModal.id) {
      await adminApi.updateBatch(formModal.id, payload)
      notify('批次已更新', 'success')
    } else {
      await adminApi.createBatch(payload)
      notify('批次已创建', 'success')
    }
    formModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function openDetail(batch) {
  detailModal.open = true
  detailModal.loading = true
  detailModal.data = null
  try {
    detailModal.data = await adminApi.batch(batch.id)
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    detailModal.loading = false
  }
}
</script>

<template>
  <PageHeader title="选拔批次" desc="维护批次名称、参与对象、阶段时间与流程规则。同一时间只会有一个进行中的批次。">
    <template #actions>
      <button class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" /> 新建批次</button>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载批次…</div></div>

  <div v-else-if="batches.length" class="stack">
    <article v-for="batch in batches" :key="batch.id" class="card card-pad">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 14px">
        <div style="min-width: 0">
          <div class="flex flex-wrap">
            <StatusPill :value="batch.status" :map="batchStatus" />
            <span class="pill pill--gray">参与年级 {{ batch.gradeScope || '不限' }}</span>
            <span v-if="batch.majorScope" class="pill pill--gray">{{ batch.majorScope }}</span>
          </div>
          <h3 class="mt-8">{{ batch.name }}</h3>
          <p class="small muted mt-4">{{ batch.description || '暂无批次说明' }}</p>
        </div>
        <div class="button-row">
          <button class="btn btn-ghost btn-sm" type="button" @click="openDetail(batch)">批次详情</button>
          <button class="btn btn-soft btn-sm" type="button" @click="openEdit(batch)"><Icon name="edit" :size="14" /> 编辑</button>
        </div>
      </div>
      <div class="grid-4 mt-16">
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">报名</div><div class="strong">{{ batch.applicationCount }} 人</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">拟录取记录</div><div class="strong">{{ batch.offerCount }} 条</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">最终录取</div><div class="strong">{{ batch.admittedCount }} 人</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">填报时间</div><div class="small">{{ fmt(batch.intentStart) }}<br />{{ fmt(batch.intentEnd) }}</div></div>
      </div>
    </article>
  </div>
  <EmptyState v-else title="暂无选拔批次" desc="新建一个批次并配置阶段时间，开始本学期的选拔。" icon="calendar">
    <button class="btn btn-primary btn-sm" type="button" @click="openCreate">新建批次</button>
  </EmptyState>

  <ModalDialog :open="formModal.open" :title="formModal.id ? '编辑选拔批次' : '新建选拔批次'" subtitle="时间将用于阶段校验与页面展示" wide @close="formModal.open = false">
    <div class="stack">
      <div class="form-grid">
        <div class="field span-2">
          <label class="label">批次名称 <span class="req">*</span></label>
          <input v-model="form.name" class="input" placeholder="例如：2026年秋季实训室进组选拔" />
        </div>
        <div class="field">
          <label class="label">参与年级</label>
          <input v-model="form.gradeScope" class="input" placeholder="2023，多个年级用逗号分隔" />
        </div>
        <div class="field">
          <label class="label">参与专业（可选）</label>
          <input v-model="form.majorScope" class="input" placeholder="为空表示不限" />
        </div>
        <div class="field span-2">
          <label class="label">批次说明</label>
          <textarea v-model="form.description" class="textarea" />
        </div>
        <div class="field span-2">
          <label class="label">流程规则</label>
          <textarea v-model="form.rules" class="textarea" placeholder="平行意向、确认、补录、调剂等规则说明。" />
        </div>
      </div>

      <div class="section-title mt-8">阶段时间</div>
      <div class="form-grid">
        <div class="field">
          <label class="label">集中宣讲时间</label>
          <input v-model="form.lectureAt" class="input" type="datetime-local" />
        </div>
        <div class="field">
          <label class="label">初始状态</label>
          <select v-model="form.status" class="select" :disabled="Boolean(formModal.id)">
            <option v-for="value in ['DRAFT','ANNOUNCED','INTENT_OPEN','INTENT_CLOSED','INTERVIEW','CONFIRM','SUPPLEMENT','ADJUSTMENT','PUBLICITY','FINISHED']" :key="value" :value="value">
              {{ resolveMeta(batchStatus, value).label }}
            </option>
          </select>
        </div>
        <template v-for="item in timeline" :key="item.label">
          <div class="field">
            <label class="label">{{ item.label }} · 开始</label>
            <input v-model="form[item.start]" class="input" type="datetime-local" />
          </div>
          <div class="field">
            <label class="label">{{ item.label }} · 结束</label>
            <input v-model="form[item.end]" class="input" type="datetime-local" />
          </div>
        </template>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="formModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="!canSave || saving" @click="save">{{ saving ? '保存中…' : '保存批次' }}</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="detailModal.open" title="批次详情" wide @close="detailModal.open = false">
    <div v-if="detailModal.loading" class="loading-block"><div class="spinner" />加载中…</div>
    <div v-else-if="detailModal.data" class="stack">
      <div class="flex" style="justify-content: space-between">
        <h3>{{ detailModal.data.batch.name }}</h3>
        <StatusPill :value="detailModal.data.batch.status" :map="batchStatus" />
      </div>
      <StepFlow :stages="detailModal.data.stages" horizontal />
      <div class="grid-3">
        <div class="offer-card"><div class="tiny muted">报名</div><div class="strong">{{ detailModal.data.applications?.length || 0 }} 人</div></div>
        <div class="offer-card"><div class="tiny muted">拟录取记录</div><div class="strong">{{ detailModal.data.offers?.length || 0 }} 条</div></div>
        <div class="offer-card"><div class="tiny muted">最终录取</div><div class="strong">{{ detailModal.data.assignments?.length || 0 }} 人</div></div>
      </div>
      <div class="grid-2">
        <div>
          <div class="label">实训室名额</div>
          <div class="table-wrap mt-8">
            <table class="table">
              <thead><tr><th>实训室</th><th>名额</th><th>录取</th></tr></thead>
              <tbody>
                <tr v-for="lab in detailModal.data.labs" :key="lab.id"><td>{{ lab.name }}</td><td>{{ lab.totalQuota }}</td><td>{{ lab.confirmedCount || 0 }}</td></tr>
              </tbody>
            </table>
          </div>
        </div>
        <div>
          <div class="label">补录轮次</div>
          <div v-if="detailModal.data.rounds?.length" class="stack mt-8" style="gap: 6px">
            <div v-for="round in detailModal.data.rounds" :key="round.id" class="offer-card" style="padding: 10px">
              <strong class="small">{{ round.title }}</strong>
              <span class="tiny muted">{{ round.status }} · 意向 {{ round.intentCount }} 条</span>
            </div>
          </div>
          <div v-else class="small muted mt-8">暂无补录轮次</div>
        </div>
      </div>
    </div>
    <template #footer><button class="btn btn-ghost" type="button" @click="detailModal.open = false">关闭</button></template>
  </ModalDialog>
</template>
