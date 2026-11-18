<script setup>
import { computed, onMounted, ref } from 'vue'
import { studentApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  applicationStatus,
  assignmentSource,
  assignmentStatus,
  batchStatus,
  publicityStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'

const loading = ref(true)
const result = ref(null)
const history = ref([])

const batch = computed(() => result.value?.batch || {})
const assignment = computed(() => result.value?.assignment || null)
const publicity = computed(() => result.value?.publicity || null)
const adjustments = computed(() => result.value?.adjustments || [])

async function load() {
  loading.value = true
  try {
    const [resultData, historyData] = await Promise.all([studentApi.result(), studentApi.history()])
    result.value = resultData
    history.value = historyData
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHeader title="我的结果" desc="查看最终录取的实训室、导师、录取方式与入组说明。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/" class="btn btn-ghost">公开结果</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载结果…</div></div>

  <template v-else-if="result">
    <section v-if="assignment" class="card card-pad" style="background: linear-gradient(120deg, #edf5dd, #fff 60%, #fbe9ee)">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 18px">
        <div>
          <div class="pill pill--green"><span class="dot" />正式录取</div>
          <h1 class="mt-12">{{ assignment.labName }}</h1>
          <p class="muted mt-4">导师：<strong>{{ assignment.mentorName }}</strong>（{{ assignment.mentorTitle }}）· 录取方式：{{ resolveMeta(assignmentSource, assignment.source).label }}</p>
        </div>
        <div class="stack" style="align-items: flex-end">
          <StatusPill :value="assignment.status" :map="assignmentStatus" />
          <span class="tiny muted">确认时间 {{ fmt(assignment.confirmedAt) }}</span>
        </div>
      </div>
      <div class="grid-3 mt-24">
        <div class="offer-card">
          <div class="tiny muted">录取批次</div>
          <div class="strong">{{ batch.name }}</div>
        </div>
        <div class="offer-card">
          <div class="tiny muted">录取方式</div>
          <div class="strong">{{ resolveMeta(assignmentSource, assignment.source).label }}</div>
        </div>
        <div class="offer-card">
          <div class="tiny muted">结果状态</div>
          <div class="strong">{{ resolveMeta(assignmentStatus, assignment.status).label }}</div>
        </div>
      </div>
      <div class="grid-2 mt-12">
        <div class="offer-card">
          <div class="tiny muted">培养方向</div>
          <div class="strong">{{ assignment.labDirection || '以实训室通知为准' }}</div>
          <div v-if="assignment.mentorDirection" class="tiny muted">导师方向：{{ assignment.mentorDirection }}</div>
        </div>
        <div class="offer-card">
          <div class="tiny muted">培养安排</div>
          <p class="small mt-4">{{ assignment.labTraining || '入组后由导师与实训室统一安排。' }}</p>
        </div>
      </div>
    </section>

    <section v-else class="card card-pad">
      <EmptyState
        title="暂未产生录取结果"
        :desc="batch.status === 'PUBLICITY' ? '最终名单正在公示中，请查看公示结果。' : '请关注后续补录、调剂与公示通知。'"
        icon="award"
      />
      <div class="center">
        <router-link to="/student/dashboard" class="btn btn-ghost btn-sm">返回我的选拔</router-link>
      </div>
    </section>

    <div class="split">
      <div class="stack">
        <section v-if="assignment" class="card card-pad">
          <div class="section-head"><div class="section-title">入组说明</div></div>
          <div class="stack" style="gap: 10px">
            <div class="notice notice--info">
              <Icon name="calendar" :size="16" />
              <span>请于公示结束后关注实训室通知，按导师要求参加首次组会与基础训练。入组后需保证每周投入时间。</span>
            </div>
            <div class="notice notice--success">
              <Icon name="book" :size="16" />
              <span>培养方式以 AI+PBL 项目制为主：基础训练 → 项目实践 → 阶段答辩。具体安排由导师与实训室统一发布。</span>
            </div>
            <div class="notice notice--warn">
              <Icon name="alert" :size="16" />
              <span>最终结果公示后锁定。如确有特殊情况需要变更，请联系学院教务办公室，按“撤回公示—修改—重新公示”流程处理。</span>
            </div>
          </div>
        </section>

        <section v-if="publicity" class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="award" :size="17" /> 结果公示</div>
              <div class="card-sub">{{ publicity.title }}</div>
            </div>
            <StatusPill :value="publicity.status" :map="publicityStatus" />
          </div>
          <p class="small" style="white-space: pre-wrap; line-height: 1.9">{{ publicity.content || '公示期内如有异议，请向学院教务办公室实名反映。' }}</p>
          <div class="kv mt-16">
            <dt>公示时间</dt><dd>{{ fmt(publicity.startAt) }} — {{ fmt(publicity.endAt) }}</dd>
            <dt>发布时间</dt><dd>{{ fmt(publicity.publishedAt) }}</dd>
          </div>
        </section>

        <section v-if="adjustments.length" class="card card-pad">
          <div class="section-head"><div class="section-title">调剂记录</div></div>
          <div class="timeline-log">
            <div v-for="item in adjustments" :key="item.id" class="log-item">
              <span class="log-dot" />
              <div>
                <div class="log-title">{{ resolveMeta(assignmentSource, item.type).label }}</div>
                <div class="log-meta">{{ fmt(item.createdAt) }} · {{ item.fromLabName || '原意向' }} → {{ item.toLabName || '谈话安置' }}</div>
                <div class="tiny muted">{{ item.reason }}</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="card card-pad">
          <div class="section-head"><div class="section-title">选拔历史</div></div>
          <div v-if="history.length" class="stack" style="gap: 9px">
            <div v-for="row in history" :key="row.application.id" class="offer-card" style="padding: 13px">
              <div class="flex" style="justify-content: space-between; gap: 8px">
                <strong class="small">{{ row.application.batchName }}</strong>
                <StatusPill :value="row.application.batchStatus" :map="batchStatus" />
              </div>
              <div class="flex" style="justify-content: space-between">
                <span class="tiny muted">报名状态</span>
                <StatusPill :value="row.application.status" :map="applicationStatus" />
              </div>
              <div class="tiny muted" v-if="row.assignment">去向：{{ row.assignment.labName }} · {{ row.assignment.mentorName }}</div>
              <div class="tiny muted" v-else>未产生最终录取结果</div>
            </div>
          </div>
          <EmptyState v-else title="暂无历史批次" desc="你还没有参加过其他批次的选拔。" icon="book" />
        </section>

        <section class="card card-pad">
          <div class="section-title">温馨提示</div>
          <ul class="small muted mt-8" style="line-height: 2">
            <li>最终公示结束后，录取结果将被锁定。</li>
            <li>请勿同时加入多个实训室，系统会进行唯一性校验。</li>
            <li>如需放弃录取，请及时联系学院，以便名额递补。</li>
          </ul>
        </section>
      </div>
    </div>
  </template>
</template>
