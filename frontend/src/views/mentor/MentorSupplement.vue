<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { mentorApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, supplementIntentStatus, supplementStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const data = ref(null)
const dashboard = ref(null)
const modal = reactive({ open: false, intent: null })
const form = reactive({ decision: 'OFFER', waitlistRank: 1, comment: '' })
const saving = ref(false)

const batch = computed(() => data.value?.batch || dashboard.value?.batch || {})
const round = computed(() => data.value?.round || null)
const intents = computed(() => data.value?.intents || [])
const canEvaluate = computed(() => batch.value.status === 'SUPPLEMENT' && round.value?.status === 'OPEN')

async function load() {
  loading.value = true
  try {
    const [state, dash] = await Promise.all([mentorApi.supplement(), mentorApi.dashboard()])
    data.value = state
    dashboard.value = dash
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openDecision(intent) {
  modal.intent = intent
  form.decision = 'OFFER'
  form.waitlistRank = 1
  form.comment = ''
  modal.open = true
}

async function submit() {
  saving.value = true
  try {
    await mentorApi.decideSupplement({
      supplementIntentId: modal.intent.id,
      decision: form.decision,
      waitlistRank: form.decision === 'WAITLIST' ? Number(form.waitlistRank) : null,
      comment: form.comment,
      reason: '补录阶段导师考核意见'
    })
    notify('补录结论已提交', 'success')
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
  <PageHeader title="补录管理" desc="查看补录阶段填报你的学生，提交拟录取、候补或不录取结论。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/mentor/applicants" class="btn btn-ghost">首轮报名学生</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载补录数据…</div></div>

  <template v-else>
    <div v-if="!round" class="card card-pad">
      <EmptyState title="暂无补录轮次" desc="学院开放补录轮次后，填报你的学生将出现在这里。" icon="refresh" />
    </div>
    <template v-else>
      <section class="card card-pad">
        <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
          <div>
            <div class="flex">
              <span class="pill pill--violet">第 {{ round.roundNo }} 轮</span>
              <StatusPill :value="round.status" :map="supplementStatus" />
            </div>
            <h2 class="mt-8">{{ round.title }}</h2>
            <p class="small muted mt-4">{{ fmt(round.startAt) }} — {{ fmt(round.endAt) }}</p>
          </div>
          <span v-if="canEvaluate" class="pill pill--green"><span class="dot" />可提交结论</span>
        </div>
      </section>

      <div v-if="!canEvaluate" class="notice notice--info">
        <Icon name="eye" :size="16" /><span>当前不在补录阶段或本轮未开放，暂不可提交补录结论。</span>
      </div>

      <div class="card">
        <div v-if="intents.length" class="table-wrap" style="border: 0">
          <table class="table">
            <thead><tr><th>学生</th><th>专业</th><th>补录意向</th><th>当前状态</th><th>意见</th><th class="right">操作</th></tr></thead>
            <tbody>
              <tr v-for="intent in intents" :key="intent.id">
                <td><strong>{{ intent.studentName }}</strong><div class="tiny muted">{{ intent.studentNo }}</div></td>
                <td>{{ intent.major }}<div class="tiny muted">{{ intent.className }}</div></td>
                <td>第 {{ intent.sortNo }} 志愿<div class="tiny muted">{{ intent.labName }}</div></td>
                <td><StatusPill :value="intent.status" :map="supplementIntentStatus" /></td>
                <td><span class="tiny muted">{{ intent.comment || '—' }}</span></td>
                <td class="right">
                  <button class="btn btn-soft btn-sm" type="button" :disabled="!canEvaluate" @click="openDecision(intent)">提交结论</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <EmptyState v-else title="暂无补录意向" desc="本轮还没有学生填报你。" icon="users" />
      </div>
    </template>
  </template>

  <ModalDialog :open="modal.open" title="提交补录结论" :subtitle="modal.intent ? `${modal.intent.studentName} · ${modal.intent.studentNo}` : ''" @close="modal.open = false">
    <div v-if="modal.intent" class="stack">
      <div class="offer-card" style="padding: 12px">
        <div class="flex" style="justify-content: space-between">
          <strong>{{ modal.intent.studentName }}</strong>
          <StatusPill :value="modal.intent.status" :map="supplementIntentStatus" />
        </div>
        <div class="tiny muted">补录第 {{ modal.intent.sortNo }} 意向 · {{ modal.intent.labName }}</div>
      </div>
      <div class="radio-cards">
        <label class="radio-card" :class="{ active: form.decision === 'OFFER' }">
          <input v-model="form.decision" type="radio" value="OFFER" hidden /><strong>拟录取</strong><span>占用补录名额</span>
        </label>
        <label class="radio-card" :class="{ active: form.decision === 'WAITLIST' }">
          <input v-model="form.decision" type="radio" value="WAITLIST" hidden /><strong>候补</strong><span>按顺位递补</span>
        </label>
        <label class="radio-card" :class="{ active: form.decision === 'REJECT' }">
          <input v-model="form.decision" type="radio" value="REJECT" hidden /><strong>不录取</strong><span>释放意向</span>
        </label>
      </div>
      <div v-if="form.decision === 'WAITLIST'" class="field">
        <label class="label">候补顺位</label>
        <input v-model.number="form.waitlistRank" class="input" type="number" min="1" max="99" style="max-width: 160px" />
      </div>
      <div class="field">
        <label class="label">意见说明</label>
        <textarea v-model="form.comment" class="textarea" placeholder="补录考核意见。" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="submit">{{ saving ? '提交中…' : '确认提交' }}</button>
    </template>
  </ModalDialog>
</template>
