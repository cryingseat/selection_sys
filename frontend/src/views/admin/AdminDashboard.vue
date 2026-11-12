<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, reviewLevel, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StepFlow from '../../components/StepFlow.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const data = ref(null)
const stageModal = reactive({ open: false, status: '', reason: '' })
const saving = ref(false)

const batch = computed(() => data.value?.batch || {})
const stats = computed(() => data.value?.stats || {})
const review = computed(() => data.value?.review || {})
const issues = computed(() => (review.value.issues || []).slice(0, 6))
const statusOptions = [
  'DRAFT', 'ANNOUNCED', 'INTENT_OPEN', 'INTENT_CLOSED', 'INTERVIEW',
  'CONFIRM', 'SUPPLEMENT', 'ADJUSTMENT', 'PUBLICITY', 'FINISHED'
]
const nextStage = {
  DRAFT: 'ANNOUNCED', ANNOUNCED: 'INTENT_OPEN', INTENT_OPEN: 'INTENT_CLOSED',
  INTENT_CLOSED: 'INTERVIEW', INTERVIEW: 'CONFIRM', CONFIRM: 'SUPPLEMENT',
  SUPPLEMENT: 'ADJUSTMENT', ADJUSTMENT: 'PUBLICITY', PUBLICITY: 'FINISHED'
}

async function load() {
  loading.value = true
  try {
    data.value = await adminApi.dashboard()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openStage(status = nextStage[batch.value.status] || 'DRAFT') {
  stageModal.status = status
  stageModal.reason = ''
  stageModal.open = true
}

async function changeStage() {
  saving.value = true
  try {
    await adminApi.changeStage(batch.value.id, { status: stageModal.status, reason: stageModal.reason })
    notify('阶段已切换', 'success')
    stageModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="学院总览" desc="掌握本批次报名、面试、名额与待办，按流程推进各阶段。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <button class="btn btn-primary" type="button" @click="openStage()">
        <Icon name="arrow" :size="15" /> 推进到「{{ resolveMeta(batchStatus, nextStage[batch.status] || 'DRAFT').label }}」
      </button>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载学院总览…</div></div>

  <template v-else-if="data">
    <section class="card card-pad" style="background: linear-gradient(120deg, #fcfbf9, #fff 55%, #f7f5f0)">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 18px">
        <div>
          <div class="flex flex-wrap">
            <span class="pill pill--blue">{{ batch.name }}</span>
            <span class="pill pill--gray">参与年级 {{ batch.gradeScope || '不限' }}</span>
            <span v-if="data.college" class="pill pill--gray">{{ data.college }}</span>
            <span class="pill pill--gray">共 {{ data.batches?.length || 0 }} 个批次</span>
          </div>
          <h2 class="mt-12">{{ resolveMeta(batchStatus, batch.status).label }}</h2>
          <p class="muted small mt-4">当前阶段时间：{{ fmt(batch.intentStart) }} — {{ fmt(batch.publicityEnd) }}</p>
        </div>
        <div class="button-row">
          <button class="btn btn-soft" type="button" @click="openStage()">切换阶段</button>
          <router-link to="/admin/review" class="btn btn-ghost">初选审核</router-link>
        </div>
      </div>
    </section>

    <div class="stat-grid">
      <StatCard label="参与学生" :value="stats.studentCount" hint="学生库总人数" tint="#f2efe9" />
      <StatCard label="报名人数" :value="stats.applicationCount" hint="已提交报名" tint="#f7f5f0" />
      <StatCard label="待初选" :value="stats.pendingDecisions" hint="导师尚未给出结论" tint="#fff0e6" />
      <StatCard label="最终录取" :value="stats.admittedCount" :hint="`待确认拟录取 ${stats.pendingOffers} 条`" tint="#e7f5f1" />
    </div>

    <div class="split">
      <div class="stack">
        <section class="card card-pad">
          <div class="section-head">
            <div class="section-title">流程阶段</div>
            <span class="small muted">点击右上角可切换阶段</span>
          </div>
          <StepFlow :stages="data.stages" horizontal />
        </section>

        <section class="card card-pad">
          <div class="section-head">
            <div class="section-title">审核提醒</div>
            <router-link to="/admin/review" class="link-btn small">查看全部</router-link>
          </div>
          <div v-if="issues.length" class="stack" style="gap: 8px">
            <div v-for="(issue, index) in issues" :key="index" class="notice" :class="{
              'notice--danger': issue.level === 'ERROR',
              'notice--warn': issue.level === 'WARN',
              'notice--info': issue.level === 'INFO'
            }">
              <Icon :name="issue.level === 'ERROR' ? 'alert' : issue.level === 'WARN' ? 'clock' : 'spark'" :size="15" />
              <span>{{ issue.message }}</span>
              <StatusPill :value="issue.level" :map="reviewLevel" style="margin-left: auto" />
            </div>
          </div>
          <EmptyState v-else title="暂无审核问题" desc="当前数据未发现超额、缺面试等异常。" icon="shield" />
        </section>

        <section class="card card-pad">
          <div class="section-head"><div class="section-title">各实训室名额</div></div>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>实训室</th><th>总名额</th><th>已录取</th><th>报名</th></tr></thead>
              <tbody>
                <tr v-for="lab in data.labs" :key="lab.id">
                  <td><strong>{{ lab.name }}</strong><div class="tiny muted">{{ lab.direction }}</div></td>
                  <td>{{ lab.totalQuota }}</td>
                  <td>{{ lab.confirmedCount || 0 }}</td>
                  <td>{{ lab.applicantCount || 0 }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="card card-pad">
          <div class="section-head"><div class="section-title">近 8 条操作</div><router-link to="/admin/audit" class="link-btn small">审计日志</router-link></div>
          <div v-if="data.recentAudits?.length" class="timeline-log">
            <div v-for="log in data.recentAudits" :key="log.id" class="log-item">
              <span class="log-dot" />
              <div>
                <div class="log-title">{{ log.actorName }} · {{ log.action }}</div>
                <div class="log-meta">{{ fmt(log.createdAt) }}</div>
                <div class="tiny muted">{{ log.detail }}</div>
              </div>
            </div>
          </div>
          <EmptyState v-else title="暂无操作记录" desc="关键操作会自动写入审计日志。" icon="chart" />
        </section>

        <section class="card card-pad">
          <div class="section-title">批次列表</div>
          <div class="stack mt-12" style="gap: 8px">
            <router-link v-for="item in data.batches" :key="item.id" to="/admin/batches" class="offer-card" style="padding: 12px; color: inherit">
              <div class="flex" style="justify-content: space-between">
                <strong class="small">{{ item.name }}</strong>
                <StatusPill :value="item.status" :map="batchStatus" />
              </div>
              <div class="tiny muted">{{ item.applicationCount }} 人报名 · {{ item.admittedCount }} 人录取</div>
            </router-link>
          </div>
        </section>
      </div>
    </div>
  </template>

  <ModalDialog :open="stageModal.open" title="切换选拔阶段" subtitle="阶段切换会影响所有学生与导师的操作权限，请谨慎操作" @close="stageModal.open = false">
    <div class="stack">
      <div class="field">
        <label class="label">目标阶段</label>
        <select v-model="stageModal.status" class="select">
          <option v-for="status in statusOptions" :key="status" :value="status">{{ resolveMeta(batchStatus, status).label }}</option>
        </select>
      </div>
      <div class="field">
        <label class="label">操作原因</label>
        <textarea v-model="stageModal.reason" class="textarea" placeholder="记录本次阶段推进/回退的原因，写入审计日志。" />
      </div>
      <div class="notice notice--warn">
        <Icon name="alert" :size="16" />
        <span>进入「拟录取确认」阶段会自动确认唯一拟录取；进入「分轮补录」会自动准备第一轮；进入「结果公示」会尝试生成公示草稿。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="stageModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="changeStage">{{ saving ? '处理中…' : '确认切换' }}</button>
    </template>
  </ModalDialog>
</template>
