<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  applicationStatus,
  assignmentSource,
  batchStatus,
  interviewConclusion,
  interviewStatus,
  intentStatus,
  offerStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const keyword = ref('')
const statusFilter = ref('')
const batches = ref([])
const batchId = ref(null)
const detail = reactive({ open: false, loading: false, data: null })

async function loadBatches() {
  batches.value = await adminApi.batches()
  const current = batches.value.find((item) => !['DRAFT', 'FINISHED'].includes(item.status)) || batches.value[0]
  batchId.value = current?.id || null
}

async function load() {
  loading.value = true
  try {
    if (!batches.value.length) await loadBatches()
    const data = await adminApi.applications({
      batchId: batchId.value || undefined,
      status: statusFilter.value || undefined,
      keyword: keyword.value || undefined,
      page: page.value,
      size: size.value
    })
    records.value = data.records
    total.value = data.total
    if (data.batch) batchId.value = data.batch.id
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  page.value = 1
  load()
}

function changeBatch() {
  page.value = 1
  load()
}

async function openDetail(application) {
  detail.open = true
  detail.loading = true
  detail.data = null
  try {
    detail.data = await adminApi.application(application.id)
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    detail.loading = false
  }
}
</script>

<template>
  <PageHeader title="报名数据" desc="查看学生报名、平行意向、调剂授权、面试与录取记录。">
    <template #actions>
      <select v-model="batchId" class="select" style="width: 260px" @change="changeBatch">
        <option v-for="batch in batches" :key="batch.id" :value="batch.id">{{ batch.name }}</option>
      </select>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 13px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
      <div class="flex" style="gap: 8px">
        <div style="position: relative">
          <Icon name="search" :size="15" style="position: absolute; left: 11px; top: 9px; color: var(--muted-2)" />
          <input v-model="keyword" class="input" style="padding-left: 32px; width: 230px" placeholder="姓名 / 学号 / 专业" @keyup.enter="search" />
        </div>
        <select v-model="statusFilter" class="select" style="width: 140px" @change="search">
          <option value="">全部状态</option>
          <option value="DRAFT">草稿</option>
          <option value="SUBMITTED">已提交</option>
          <option value="WITHDRAWN">已撤回</option>
          <option value="LOCKED">已锁定</option>
        </select>
        <button class="btn btn-soft btn-sm" @click="search">查询</button>
      </div>
      <span class="small muted">共 {{ total }} 条</span>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载报名数据…</div>
    <div v-else-if="records.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>学生</th><th>专业 / 班级</th><th>意向</th><th>调剂意愿</th><th>状态</th><th>提交时间</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="application in records" :key="application.id">
            <td><strong>{{ application.studentName }}</strong><div class="tiny muted">{{ application.studentNo }}</div></td>
            <td>{{ application.major }}<div class="tiny muted">{{ application.className }}</div></td>
            <td><span class="pill pill--gray">{{ (application.intents || []).length }} 个</span></td>
            <td><span class="tiny">{{ { INTERNAL: '仅内部', CROSS: '接受跨室', NONE: '不接受' }[application.adjustmentPreference] }}</span></td>
            <td><StatusPill :value="application.status" :map="applicationStatus" /></td>
            <td>{{ fmt(application.submittedAt) }}</td>
            <td class="right"><button class="btn btn-soft btn-sm" type="button" @click="openDetail(application)">详情</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无报名数据" desc="学生提交意向后会出现在这里。" icon="form" />
    <div v-if="total > size" class="flex" style="justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line)">
      <span class="small muted">第 {{ page }} 页</span>
      <div class="button-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <button class="btn btn-ghost btn-sm" :disabled="page * size >= total" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>

  <ModalDialog :open="detail.open" title="报名详情" wide @close="detail.open = false">
    <div v-if="detail.loading" class="loading-block"><div class="spinner" />加载中…</div>
    <div v-else-if="detail.data" class="stack">
      <section class="card card-pad" style="box-shadow: none; background: var(--surface-2)">
        <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
          <div>
            <h3>{{ detail.data.application.studentName }}</h3>
            <div class="small muted">{{ detail.data.application.studentNo }} · {{ detail.data.application.major }} · {{ detail.data.application.className }}</div>
          </div>
          <div class="flex">
            <StatusPill :value="detail.data.application.status" :map="applicationStatus" />
            <StatusPill :value="detail.data.application.batchStatus" :map="batchStatus" />
          </div>
        </div>
        <div class="kv mt-12">
          <dt>调剂意愿</dt><dd>{{ { INTERNAL: '接受实训室内调剂', CROSS: '接受跨实训室调剂', NONE: '不接受调剂' }[detail.data.application.adjustmentPreference] }}</dd>
          <dt>拒绝实训室</dt><dd>{{ detail.data.application.excludedLabIdList?.length ? `${detail.data.application.excludedLabIdList.length} 个` : '无' }}</dd>
          <dt>提交时间</dt><dd>{{ fmt(detail.data.application.submittedAt) }}</dd>
        </div>
      </section>

      <div class="grid-2">
        <div>
          <div class="label">平行意向</div>
          <div class="stack mt-8" style="gap: 7px">
            <div v-for="intent in detail.data.application.intents" :key="intent.id" class="offer-card" style="padding: 11px 12px">
              <div class="flex" style="justify-content: space-between">
                <strong class="small">{{ intent.sortNo }}. {{ intent.mentorName }}</strong>
                <StatusPill :value="intent.status" :map="intentStatus" />
              </div>
              <div class="tiny muted">{{ intent.labName }} · {{ intent.researchDirection }}</div>
              <div v-if="intent.comment" class="tiny muted">意见：{{ intent.comment }}</div>
            </div>
          </div>
        </div>
        <div>
          <div class="label">面试记录</div>
          <div v-if="detail.data.interviews?.length" class="stack mt-8" style="gap: 7px">
            <div v-for="item in detail.data.interviews" :key="item.id" class="offer-card" style="padding: 11px 12px">
              <div class="flex" style="justify-content: space-between">
                <strong class="small">{{ item.labName }}</strong>
                <StatusPill :value="item.status" :map="interviewStatus" />
              </div>
              <div class="tiny muted">{{ fmt(item.scheduledAt) }} · {{ item.location || '—' }} · {{ item.score ?? '—' }} 分</div>
              <div class="tiny muted">{{ resolveMeta(interviewConclusion, item.conclusion).label }} {{ item.comment || '' }}</div>
            </div>
          </div>
          <div v-else class="small muted mt-8">暂无面试记录</div>
          <div class="label mt-16">调剂记录 / 最终结果</div>
          <div v-if="detail.data.assignment" class="offer-card mt-8" style="padding: 12px">
            <div class="strong">{{ detail.data.assignment.labName }} · {{ detail.data.assignment.mentorName }}</div>
            <div class="tiny muted">{{ resolveMeta(assignmentSource, detail.data.assignment.source).label }} · {{ fmt(detail.data.assignment.confirmedAt) }}</div>
          </div>
          <div v-else class="small muted mt-8">尚未产生最终录取结果</div>
        </div>
      </div>

      <div>
        <div class="label">报名材料</div>
        <div class="grid-3 mt-8">
          <div class="offer-card" style="padding: 12px"><div class="tiny muted">个人简介</div><p class="small mt-4">{{ detail.data.application.intro || '—' }}</p></div>
          <div class="offer-card" style="padding: 12px"><div class="tiny muted">技术基础</div><p class="small mt-4">{{ detail.data.application.skills || '—' }}</p></div>
          <div class="offer-card" style="padding: 12px"><div class="tiny muted">项目经历</div><p class="small mt-4">{{ detail.data.application.projectExperience || '—' }}</p></div>
        </div>
      </div>
    </div>
    <template #footer><button class="btn btn-ghost" type="button" @click="detail.open = false">关闭</button></template>
  </ModalDialog>
</template>
