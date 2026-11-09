<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { studentApi } from '../../api'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, offerSource, offerStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, relativeDays } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const offers = ref([])
const dashboard = ref(null)
const actionLoading = ref(false)
const decision = reactive({ open: false, mode: 'confirm', offer: null })

const batch = computed(() => dashboard.value?.batch || {})
const assignment = computed(() => dashboard.value?.assignment || null)
const active = computed(() => offers.value.filter((offer) => offer.status === 'OFFERED'))
const history = computed(() => offers.value.filter((offer) => offer.status !== 'OFFERED'))
const canAct = computed(() => ['CONFIRM', 'SUPPLEMENT'].includes(batch.value.status))

async function load() {
  loading.value = true
  try {
    offers.value = await studentApi.offers()
    dashboard.value = await studentApi.dashboard()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openDecision(offer, mode) {
  decision.offer = offer
  decision.mode = mode
  decision.open = true
}

async function submitDecision() {
  actionLoading.value = true
  try {
    if (decision.mode === 'confirm') {
      await studentApi.confirmOffer(decision.offer.id, '学生自主选择该拟录取意向')
      notify('已确认拟录取，其他意向已释放', 'success')
    } else {
      await studentApi.declineOffer(decision.offer.id, '学生主动放弃该拟录取意向')
      notify('已放弃该拟录取意向', 'success')
    }
    decision.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <PageHeader title="拟录取确认" desc="只收到 1 个拟录取意向时系统自动确认；收到多个时须自主选择唯一去向。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/student/result" class="btn btn-ghost">查看最终结果</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载拟录取意向…</div></div>

  <template v-else>
    <div v-if="batch.status === 'CONFIRM'" class="notice notice--warn">
      <Icon name="clock" :size="16" />
      <span>拟录取确认截止 {{ fmt(batch.confirmEnd) }}（{{ relativeDays(batch.confirmEnd) }}）。逾期未确认视为放弃，名额将进入补录。</span>
    </div>
    <div v-else-if="batch.status === 'SUPPLEMENT'" class="notice notice--info">
      <Icon name="refresh" :size="16" />
      <span>当前为补录阶段，补录拟录取同样需要你确认。请关注截止时间。</span>
    </div>
    <div v-else class="notice notice--info">
      <Icon name="eye" :size="16" />
      <span>当前处于「{{ resolveMeta(batchStatus, batch.status).label }}」阶段。拟录取确认将在确认阶段开放。</span>
    </div>

    <div v-if="assignment" class="card card-pad" style="background: linear-gradient(120deg, #effbf6, #fff)">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 14px">
        <div>
          <div class="pill pill--green"><span class="dot" />已完成确认</div>
          <h2 class="mt-12">{{ assignment.labName }}</h2>
          <p class="muted small mt-4">导师 {{ assignment.mentorName }} · {{ resolveMeta(assignmentSource, assignment.source).label }}</p>
        </div>
        <router-link to="/student/result" class="btn btn-primary">查看结果与入组说明</router-link>
      </div>
    </div>

    <section class="card card-pad">
      <div class="section-head">
        <div class="section-title">待确认意向</div>
        <span class="pill" :class="active.length ? 'pill--orange' : 'pill--gray'">{{ active.length }} 个</span>
      </div>

      <div v-if="active.length" class="stack">
        <div v-if="active.length > 1" class="notice notice--warn">
          <Icon name="alert" :size="16" />
          <span>你有 {{ active.length }} 个拟录取意向，只能确认其中 1 个。确认后其他意向自动释放，请慎重选择。</span>
        </div>
        <div v-for="offer in active" :key="offer.id" class="offer-card highlight">
          <div class="flex" style="justify-content: space-between">
            <span class="pill pill--blue">{{ resolveMeta(offerSource, offer.source).label }}</span>
            <StatusPill :value="offer.status" :map="offerStatus" />
          </div>
          <div>
            <div style="font-size: 18px; font-weight: 800">{{ offer.mentorName }}</div>
            <div class="small muted">{{ offer.labName }} · {{ offer.major }}</div>
          </div>
          <div class="tiny muted">发放时间 {{ fmt(offer.createdAt) }}</div>
          <div class="button-row">
            <button class="btn btn-primary" type="button" :disabled="!canAct" @click="openDecision(offer, 'confirm')">
              <Icon name="check" :size="15" /> 确认这个意向
            </button>
            <button class="btn btn-ghost" type="button" :disabled="!canAct" @click="openDecision(offer, 'decline')">放弃</button>
          </div>
        </div>
      </div>
      <EmptyState v-else-if="!assignment" title="暂无待确认的拟录取意向" desc="导师完成初选后，拟录取意向会出现在这里。" icon="award" />

      <div v-if="!canAct && active.length" class="notice notice--info mt-12">
        <Icon name="clock" :size="15" /><span>当前阶段暂不可操作，请在拟录取确认或补录阶段进行确认。</span>
      </div>
    </section>

    <section v-if="history.length" class="card card-pad">
      <div class="section-head"><div class="section-title">历史意向记录</div></div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>导师</th><th>实训室</th><th>来源</th><th>状态</th><th>发放时间</th><th>确认时间</th></tr></thead>
          <tbody>
            <tr v-for="offer in history" :key="offer.id">
              <td>{{ offer.mentorName }}</td>
              <td>{{ offer.labName }}</td>
              <td>{{ resolveMeta(offerSource, offer.source).label }}</td>
              <td><StatusPill :value="offer.status" :map="offerStatus" /></td>
              <td>{{ fmt(offer.createdAt) }}</td>
              <td>{{ fmt(offer.confirmedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </template>

  <ModalDialog :open="decision.open" :title="decision.mode === 'confirm' ? '确认这个拟录取意向？' : '放弃这个拟录取意向？'" @close="decision.open = false">
    <div v-if="decision.offer" class="stack">
      <div class="offer-card highlight">
        <strong style="font-size: 17px">{{ decision.offer.mentorName }}</strong>
        <span class="small muted">{{ decision.offer.labName }}</span>
      </div>
      <div class="notice" :class="decision.mode === 'confirm' ? 'notice--warn' : 'notice--danger'">
        <Icon name="alert" :size="16" />
        <span v-if="decision.mode === 'confirm'">确认后其他拟录取意向将自动释放，操作不可撤销。</span>
        <span v-else>放弃后名额进入候补递补，操作不可撤销。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="decision.open = false">取消</button>
      <button class="btn" :class="decision.mode === 'confirm' ? 'btn-primary' : 'btn-danger'" type="button" :disabled="actionLoading" @click="submitDecision">
        {{ actionLoading ? '处理中…' : decision.mode === 'confirm' ? '确认选择' : '确认放弃' }}
      </button>
    </template>
  </ModalDialog>
</template>
