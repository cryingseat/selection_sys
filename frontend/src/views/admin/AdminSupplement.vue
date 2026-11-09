<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, supplementStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, fromInput, toInput } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const actionLoading = ref(false)
const dashboard = ref(null)
const rounds = ref([])
const waitlist = ref(null)
const createModal = reactive({ open: false })
const confirmModal = reactive({ open: false, action: '', round: null })

const form = reactive({ roundNo: 1, title: '', startAt: '', endAt: '' })

const batch = computed(() => dashboard.value?.batch || {})
const groups = computed(() => waitlist.value?.groups || [])
const canManage = computed(() => batch.value.status === 'SUPPLEMENT')

async function load() {
  loading.value = true
  try {
    const [dash, roundList, wait] = await Promise.all([
      adminApi.dashboard(),
      adminApi.rounds(),
      adminApi.waitlist()
    ])
    dashboard.value = dash
    rounds.value = roundList
    waitlist.value = wait
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate() {
  form.roundNo = rounds.value.length + 1
  form.title = `第${form.roundNo}轮补录`
  form.startAt = toInput(batch.value.supplementStart)
  form.endAt = toInput(batch.value.supplementEnd)
  createModal.open = true
}

async function createRound() {
  actionLoading.value = true
  try {
    await adminApi.createRound({
      roundNo: Number(form.roundNo),
      title: form.title,
      startAt: fromInput(form.startAt),
      endAt: fromInput(form.endAt)
    })
    notify('补录轮次已创建', 'success')
    createModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

function ask(action, round) {
  confirmModal.action = action
  confirmModal.round = round
  confirmModal.open = true
}

async function execute() {
  actionLoading.value = true
  try {
    if (confirmModal.action === 'open') {
      await adminApi.openRound(confirmModal.round.id)
      notify('补录轮次已开放', 'success')
    } else if (confirmModal.action === 'close') {
      const result = await adminApi.closeRound(confirmModal.round.id)
      notify(`轮次已结束，逾期拟录取 ${result.expiredOffers} 条`, 'success')
    } else if (confirmModal.action === 'promote') {
      const result = await adminApi.promote(confirmModal.round.id)
      notify(`候补递补完成，共 ${result.promotedCount} 人转为拟录取`, 'success')
    }
    confirmModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <PageHeader title="分轮补录" desc="公布剩余名额，按候补顺位递补，并组织未录取学生重新填报。最多支持三轮。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <button class="btn btn-primary" type="button" :disabled="!canManage" @click="openCreate"><Icon name="plus" :size="15" /> 新建补录轮次</button>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载补录数据…</div></div>

  <template v-else>
    <div v-if="!canManage" class="notice notice--info">
      <Icon name="eye" :size="16" /><span>当前批次不在「分轮补录」阶段，补录轮次为只读。</span>
    </div>

    <div class="notice notice--warn">
      <Icon name="alert" :size="16" />
      <span>建议顺序：开放轮次 → 执行候补递补（自动按顺位转拟录取）→ 学生确认/补录填报 → 结束轮次（逾期自动释放）。</span>
    </div>

    <section class="card card-pad">
      <div class="section-head"><div class="section-title">补录轮次</div><span class="pill pill--gray">{{ rounds.length }} 轮</span></div>
      <div v-if="rounds.length" class="stack">
        <div v-for="round in rounds" :key="round.id" class="offer-card">
          <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
            <div>
              <div class="flex">
                <span class="pill pill--violet">第 {{ round.roundNo }} 轮</span>
                <StatusPill :value="round.status" :map="supplementStatus" />
              </div>
              <h3 class="mt-8">{{ round.title }}</h3>
              <div class="small muted">{{ fmt(round.startAt) }} — {{ fmt(round.endAt) }}</div>
            </div>
            <div class="button-row">
              <button v-if="round.status === 'SCHEDULED'" class="btn btn-primary btn-sm" type="button" :disabled="!canManage || actionLoading" @click="ask('open', round)">开放轮次</button>
              <button v-if="round.status === 'OPEN'" class="btn btn-orange btn-sm" type="button" :disabled="!canManage || actionLoading" @click="ask('promote', round)">执行候补递补</button>
              <button v-if="round.status === 'OPEN'" class="btn btn-ghost btn-sm" type="button" :disabled="!canManage || actionLoading" @click="ask('close', round)">结束轮次</button>
            </div>
          </div>
          <div class="grid-3 mt-12">
            <div class="tiny muted">补录意向：<strong style="color: var(--ink)">{{ round.intentCount }} 条</strong></div>
            <div class="tiny muted">补录拟录取：<strong style="color: var(--ink)">{{ round.offerCount }} 条</strong></div>
            <div class="tiny muted">剩余名额：<strong style="color: var(--ink)">{{ waitlist?.totalWaitlist ?? 0 }} 人候补</strong></div>
          </div>
        </div>
      </div>
      <EmptyState v-else title="暂无补录轮次" desc="进入补录阶段后系统会自动创建第一轮，也可以手动创建。" icon="refresh" />
    </section>

    <section class="card card-pad">
      <div class="section-head"><div class="section-title">候补顺位</div><span class="pill pill--amber">{{ waitlist?.totalWaitlist || 0 }} 人</span></div>
      <div v-if="groups.length" class="stack">
        <div v-for="group in groups" :key="group.mentor.id" class="offer-card">
          <div class="flex" style="justify-content: space-between">
            <div><strong>{{ group.mentor.name }}</strong><span class="muted tiny"> · {{ group.mentor.labName }}</span></div>
            <span class="pill" :class="group.remainingQuota > 0 ? 'pill--green' : 'pill--gray'">剩余 {{ group.remainingQuota }}</span>
          </div>
          <div class="table-wrap mt-8">
            <table class="table">
              <thead><tr><th>顺位</th><th>学生</th><th>学号</th><th>专业</th><th>面试意见</th></tr></thead>
              <tbody>
                <tr v-for="intent in group.waitlist" :key="intent.id">
                  <td><span class="badge-no">{{ intent.waitlistRank }}</span></td>
                  <td><strong>{{ intent.studentName }}</strong></td>
                  <td>{{ intent.studentNo }}</td>
                  <td>{{ intent.major }}</td>
                  <td><span class="tiny muted">{{ intent.comment || '—' }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <EmptyState v-else title="暂无候补学生" desc="导师设置候补顺位后会出现在这里。" icon="users" />
    </section>
  </template>

  <ModalDialog :open="createModal.open" title="新建补录轮次" @close="createModal.open = false">
    <div class="form-grid">
      <div class="field">
        <label class="label">轮次</label>
        <input v-model.number="form.roundNo" class="input" type="number" min="1" max="3" />
      </div>
      <div class="field">
        <label class="label">标题</label>
        <input v-model="form.title" class="input" />
      </div>
      <div class="field">
        <label class="label">开始时间</label>
        <input v-model="form.startAt" class="input" type="datetime-local" />
      </div>
      <div class="field">
        <label class="label">结束时间</label>
        <input v-model="form.endAt" class="input" type="datetime-local" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="createModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading" @click="createRound">创建</button>
    </template>
  </ModalDialog>

  <ModalDialog
    :open="confirmModal.open"
    :title="confirmModal.action === 'open' ? '开放补录轮次？' : confirmModal.action === 'close' ? '结束补录轮次？' : '执行候补顺位递补？'"
    @close="confirmModal.open = false"
  >
    <div class="stack">
      <div v-if="confirmModal.round" class="offer-card" style="padding: 12px">
        <strong>{{ confirmModal.round.title }}</strong>
        <span class="tiny muted">{{ resolveMeta(supplementStatus, confirmModal.round.status).label }}</span>
      </div>
      <div class="notice" :class="confirmModal.action === 'close' ? 'notice--danger' : 'notice--info'">
        <Icon name="alert" :size="16" />
        <span v-if="confirmModal.action === 'open'">开放后学生可填报补录意向，导师可提交补录结论。</span>
        <span v-else-if="confirmModal.action === 'close'">结束后本轮待确认拟录取将自动标记为逾期，名额释放。</span>
        <span v-else>按候补顺位自动为学生发放拟录取，名额不足时跳过并记录原因。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="confirmModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading" @click="execute">{{ actionLoading ? '处理中…' : '确认' }}</button>
    </template>
  </ModalDialog>
</template>
