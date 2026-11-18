<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { studentApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  applicationStatus,
  assignmentSource,
  batchStatus,
  intentStatus,
  interviewConclusion,
  interviewStatus,
  offerSource,
  offerStatus,
  supplementStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt, relativeDays } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StepFlow from '../../components/StepFlow.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'
import ActionGuide from '../../components/ActionGuide.vue'

const router = useRouter()
const loading = ref(true)
const data = ref(null)
const actionLoading = ref(false)
const decision = reactive({ open: false, mode: 'confirm', offer: null })

const batch = computed(() => data.value?.batch || {})
const application = computed(() => data.value?.application || null)
const offers = computed(() => data.value?.offers || [])
const interviews = computed(() => data.value?.interviews || [])
const assignment = computed(() => data.value?.assignment || null)
const publicity = computed(() => data.value?.publicity || null)
const adjustments = computed(() => data.value?.adjustments || [])
const supplementRound = computed(() => data.value?.openSupplementRound || null)
const activeOffers = computed(() => offers.value.filter((offer) => offer.status === 'OFFERED'))
const confirmedOffer = computed(() => offers.value.find((offer) => offer.status === 'CONFIRMED'))
const stages = computed(() => data.value?.stages || [])
const batchMeta = computed(() => resolveMeta(batchStatus, batch.value.status))
const pendingInterviews = computed(() => interviews.value.filter((item) => item.status !== 'COMPLETED'))
const completedInterviews = computed(() => interviews.value.filter((item) => item.status === 'COMPLETED'))

async function load() {
  loading.value = true
  try {
    data.value = await studentApi.dashboard()
    maybeShowGuide()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

const guideOpen = ref(false)
const guide = computed(() => {
  if (!data.value) return null
  const b = batch.value.status
  const app = application.value
  const hasActive = activeOffers.value.length > 0
  const sr = supplementRound.value

  // 只在存在未完成动作时给出引导：已确定最终去向 / 公示阶段不弹窗
  if (b === 'INTENT_OPEN' && (!app || app.status === 'WITHDRAWN' || app.status === 'DRAFT')) {
    return { key: 'intent', stage: '意向填报', title: '当前为意向填报阶段', desc: '选择最多 3 位导师，表达你的真实兴趣，提交后等待面试安排。', actionLabel: '去填报意向', target: '/student/application' }
  }
  if (b === 'CONFIRM' && hasActive) {
    return { key: 'confirm', stage: '拟录取确认', title: `你有 ${activeOffers.value.length} 个拟录取待确认`, desc: '只能确认一个去向，请尽快做出选择，逾期视为放弃。', actionLabel: '去确认拟录取', target: '/student/offers' }
  }
  if (sr && sr.status === 'OPEN') {
    return { key: 'supplement', stage: '分轮补录', title: '补录填报进行中', desc: `截止 ${fmt(sr.endAt)}，可重新填报最多 3 个补录意向。`, actionLabel: '去补录填报', target: '/student/supplement' }
  }
  return null
})

function maybeShowGuide() {
  // 每个登录会话只检测一次：登录后首次进入工作台时，存在未完成动作才弹窗，之后点回「我的选拔」不再自动弹
  const FLAG = 'selection_guide_checked'
  if (sessionStorage.getItem(FLAG)) return
  sessionStorage.setItem(FLAG, '1')
  if (guide.value) guideOpen.value = true
}

function onGuideGo() {
  guideOpen.value = false
  if (guide.value?.target) router.push(guide.value.target)
}

function openDecision(offer, mode) {
  decision.offer = offer
  decision.mode = mode
  decision.open = true
}

async function submitDecision() {
  if (!decision.offer) return
  actionLoading.value = true
  try {
    if (decision.mode === 'confirm') {
      await studentApi.confirmOffer(decision.offer.id, '学生自主选择该拟录取意向')
      notify('已确认拟录取，其他拟录取意向已自动释放', 'success')
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

async function declineAll() {
  if (!window.confirm('确认放弃全部拟录取意向？放弃后名额将释放给候补同学。')) return
  actionLoading.value = true
  try {
    await studentApi.declineAll('学生主动放弃全部拟录取意向')
    notify('已放弃全部拟录取意向', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <PageHeader title="我的选拔" desc="查看当前阶段、报名状态、面试安排与拟录取结果。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link v-if="!application || application.status === 'WITHDRAWN'" to="/student/application" class="btn btn-primary">
        <Icon name="form" :size="15" /> 填报意向
      </router-link>
      <router-link v-else to="/student/application" class="btn btn-ghost"><Icon name="edit" :size="15" /> 查看报名</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载你的选拔进度…</div></div>

  <template v-else-if="data">
    <section class="card card-pad" style="background: linear-gradient(120deg, #f8fbf0, #fff 55%, #fbe9ee)">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 18px">
        <div style="min-width: 0">
          <div class="flex flex-wrap">
            <span class="pill pill--blue">{{ batch.name }}</span>
            <span class="pill pill--gray">意向填报 {{ fmt(batch.intentStart) }} — {{ fmt(batch.intentEnd) }}</span>
            <span v-if="data.student?.collegeName" class="pill pill--gray">{{ data.student.collegeName }}</span>
          </div>
          <h2 class="mt-12">{{ data.student?.name }}，{{ batchMeta.label }}</h2>
          <p class="muted small mt-4">
            {{ batch.status === 'CONFIRM' ? `拟录取确认截止 ${fmt(batch.confirmEnd)}，逾期未确认视为放弃。` : '按流程完成每一步，关键节点会在这里提醒你。' }}
          </p>
        </div>
        <div class="stack" style="align-items: flex-end; gap: 8px">
          <div class="pill" :class="assignment ? 'pill--green' : activeOffers.length ? 'pill--orange' : 'pill--blue'">
            <span class="dot" />
            {{ assignment ? '已确定最终去向' : activeOffers.length ? `${activeOffers.length} 个拟录取待确认` : '流程进行中' }}
          </div>
          <router-link v-if="assignment" to="/student/result" class="btn btn-soft btn-sm">查看录取结果 <Icon name="arrow" :size="13" /></router-link>
        </div>
      </div>
    </section>

    <div class="stat-grid">
      <StatCard label="报名状态" :value="resolveMeta(applicationStatus, application?.status || 'DRAFT').label" :hint="application ? `${(application.intents || []).length} 个平行意向` : '尚未填报'" tint="#fbe3ea" />
      <StatCard label="面试考核" :value="completedInterviews.length ? `${completedInterviews.length} 场已完成` : pendingInterviews.length ? `${pendingInterviews.length} 场待参加` : '待安排'" hint="以实训室通知为准" tint="#fbe9ee" />
      <StatCard label="拟录取" :value="offers.length" hint="可同时获得多个意向，只能确认一个" tint="#f6f2cf" />
      <StatCard label="最终去向" :value="assignment ? assignment.labName : '待定'" :hint="assignment ? `${assignment.mentorName} · ${resolveMeta(assignmentSource, assignment.source).label}` : '公示后确定'" tint="#e6f2d8" />
    </div>

    <div class="split">
      <div class="stack">
        <section class="card card-pad">
          <div class="section-head">
            <div class="section-title">选拔进度</div>
            <span class="small muted">共 8 个阶段</span>
          </div>
          <StepFlow :stages="stages" />
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="form" :size="17" /> 我的报名</div>
              <div class="card-sub">{{ application ? `提交于 ${fmt(application.submittedAt)}` : '尚未提交报名' }}</div>
            </div>
            <StatusPill v-if="application" :value="application.status" :map="applicationStatus" />
          </div>

          <template v-if="application">
            <div class="stack" style="gap: 10px">
              <div v-for="(intent, index) in application.intents || []" :key="intent.id" class="offer-card" style="padding: 12px 14px">
                <div class="flex" style="justify-content: space-between">
                  <div class="flex">
                    <span class="badge-no">{{ index + 1 }}</span>
                    <div>
                      <strong>{{ intent.mentorName }}</strong>
                      <span class="muted tiny"> · {{ intent.mentorTitle }} · {{ intent.labName }}</span>
                      <div class="tiny muted">{{ intent.researchDirection }}</div>
                    </div>
                  </div>
                  <StatusPill :value="intent.status" :map="intentStatus" />
                </div>
              </div>
            </div>
            <div class="kv mt-16">
              <dt>调剂意愿</dt>
              <dd>{{ { INTERNAL: '接受实训室内调剂', CROSS: '接受跨实训室调剂', NONE: '不接受调剂' }[application.adjustmentPreference] }}</dd>
              <dt>拒绝实训室</dt>
              <dd>{{ application.excludedLabIdList?.length ? `${application.excludedLabIdList.length} 个` : '无' }}</dd>
            </div>
            <div v-if="batch.status === 'INTENT_OPEN'" class="notice notice--info mt-12">
              <Icon name="clock" :size="15" />
              <span>填报截止前可修改。截止时间 {{ fmt(batch.intentEnd) }}。</span>
            </div>
          </template>
          <EmptyState v-else title="还没有提交报名" desc="选择最多 3 位导师，表达你的真实兴趣。" icon="form">
            <router-link to="/student/application" class="btn btn-primary btn-sm">去填报</router-link>
          </EmptyState>
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="calendar" :size="17" /> 面试安排与结果</div>
              <div class="card-sub">面试按实训室统一组织，请留意时间与地点。</div>
            </div>
            <span class="pill pill--gray">{{ interviews.length }} 场</span>
          </div>
          <div v-if="interviews.length" class="stack" style="gap: 10px">
            <div v-for="item in interviews" :key="item.id" class="offer-card" style="padding: 13px 15px">
              <div class="flex" style="justify-content: space-between; gap: 10px">
                <div>
                  <strong>{{ item.labName }}</strong>
                  <div class="tiny muted">{{ fmt(item.scheduledAt) }} · {{ item.location || '地点待定' }}</div>
                </div>
                <div class="flex">
                  <StatusPill :value="item.status" :map="interviewStatus" />
                  <StatusPill v-if="item.conclusion" :value="item.conclusion" :map="interviewConclusion" />
                </div>
              </div>
              <div v-if="item.score != null" class="flex" style="justify-content: space-between">
                <span class="small muted">面试评分</span>
                <strong style="font-size: 18px">{{ item.score }}<span class="tiny muted"> / 100</span></strong>
              </div>
              <p v-if="item.comment" class="tiny muted">{{ item.comment }}</p>
            </div>
          </div>
          <EmptyState v-else title="暂无面试安排" desc="意向填报截止后，实训室会统一安排面试。" icon="calendar" />
        </section>
      </div>

      <div class="stack">
        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="award" :size="17" /> 拟录取意向</div>
              <div class="card-sub">可以同时收到多个，但只能确认一个。</div>
            </div>
            <span v-if="activeOffers.length" class="pill pill--orange">{{ activeOffers.length }} 待确认</span>
          </div>

          <div v-if="activeOffers.length" class="stack">
            <div v-if="activeOffers.length > 1" class="notice notice--warn notice--strong">
              <Icon name="alert" :size="18" />
              <span>你收到多个拟录取意向，请于 {{ fmt(batch.confirmEnd) }} 前选择唯一去向；逾期未确认视为全部放弃。</span>
            </div>
            <div v-for="offer in activeOffers" :key="offer.id" class="offer-card highlight">
              <div class="flex" style="justify-content: space-between">
                <span class="pill pill--blue">{{ resolveMeta(offerSource, offer.source).label }}</span>
                <span class="tiny muted">{{ relativeDays(batch.confirmEnd) }}</span>
              </div>
              <div>
                <div class="strong" style="font-size: 16px">{{ offer.mentorName }}</div>
                <div class="small muted">{{ offer.labName }}</div>
              </div>
              <div class="button-row">
                <button class="btn btn-primary btn-sm" type="button" @click="openDecision(offer, 'confirm')">
                  <Icon name="check" :size="14" /> 确认选择
                </button>
                <button class="btn btn-ghost btn-sm" type="button" @click="openDecision(offer, 'decline')">放弃该意向</button>
              </div>
            </div>
            <button class="btn btn-danger btn-block btn-sm" type="button" :disabled="actionLoading" @click="declineAll">全部放弃</button>
          </div>

          <div v-else-if="confirmedOffer" class="notice notice--success">
            <Icon name="check" :size="16" />
            <span>你已确认 <strong>{{ confirmedOffer.mentorName }}</strong>（{{ confirmedOffer.labName }}）的拟录取意向。</span>
          </div>
          <div v-else class="notice notice--info">
            <Icon name="clock" :size="16" />
            <span>暂无可确认的拟录取意向，请关注面试与初选进度。</span>
          </div>
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="target" :size="17" /> 最终结果</div>
              <div class="card-sub">公示后锁定最终去向。</div>
            </div>
            <StatusPill v-if="assignment" value="CONFIRMED" :map="{ CONFIRMED: { label: '已确认', tone: 'green' } }" />
          </div>
          <div v-if="assignment" class="offer-card highlight">
            <div class="tiny muted">{{ resolveMeta(assignmentSource, assignment.source).label }}</div>
            <div style="font-size: 18px; font-weight: 800">{{ assignment.labName }}</div>
            <div class="small">导师：<strong>{{ assignment.mentorName }}</strong> {{ assignment.mentorTitle }}</div>
            <div class="tiny muted">确认时间 {{ fmt(assignment.confirmedAt) }}</div>
            <router-link to="/student/result" class="btn btn-soft btn-sm btn-block">查看入组说明 <Icon name="arrow" :size="13" /></router-link>
          </div>
          <div v-else class="notice notice--info">
            <Icon name="clock" :size="16" />
            <span>{{ batch.status === 'PUBLICITY' ? '最终名单正在公示，请查看公示结果。' : '尚未产生最终录取结果。' }}</span>
          </div>
          <div v-if="adjustments.length" class="mt-12">
            <div class="label">调剂记录</div>
            <div v-for="item in adjustments" :key="item.id" class="tiny muted mt-4">
              {{ fmt(item.createdAt) }} · {{ resolveMeta(assignmentSource, item.type).label }} · {{ item.reason }}
            </div>
          </div>
        </section>

        <section v-if="supplementRound" class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="refresh" :size="17" /> 补录轮次</div>
              <div class="card-sub">{{ supplementRound.title }}</div>
            </div>
            <StatusPill :value="supplementRound.status" :map="supplementStatus" />
          </div>
          <p class="small muted">截止 {{ fmt(supplementRound.endAt) }}。未录取学生可重新填报最多 3 个补录意向。</p>
          <router-link to="/student/supplement" class="btn btn-primary btn-sm btn-block mt-12">进入补录填报</router-link>
        </section>

        <section v-if="publicity" class="card card-pad">
          <div class="section-title">结果公示</div>
          <p class="small muted mt-8">{{ publicity.title }}</p>
          <router-link to="/student/result" class="btn btn-ghost btn-sm btn-block mt-12">查看公示与结果</router-link>
        </section>
      </div>
    </div>
  </template>

  <ModalDialog
    :open="decision.open"
    :title="decision.mode === 'confirm' ? '确认拟录取意向' : '放弃拟录取意向'"
    @close="decision.open = false"
  >
    <div v-if="decision.offer" class="stack">
      <div class="offer-card highlight">
        <div class="strong" style="font-size: 17px">{{ decision.offer.mentorName }}</div>
        <div class="small muted">{{ decision.offer.labName }}</div>
      </div>
      <div v-if="decision.mode === 'confirm'" class="notice notice--warn">
        <Icon name="alert" :size="16" />
        <span>确认后，其他拟录取意向将自动释放，且不能撤销。最终结果将在公示后锁定。</span>
      </div>
      <div v-else class="notice notice--danger">
        <Icon name="alert" :size="16" />
        <span>放弃后该名额将进入候补递补，不能撤销。若仍有其他拟录取意向可继续确认。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="decision.open = false">取消</button>
      <button class="btn" :class="decision.mode === 'confirm' ? 'btn-primary' : 'btn-danger'" type="button" :disabled="actionLoading" @click="submitDecision">
        {{ actionLoading ? '处理中…' : decision.mode === 'confirm' ? '确认选择' : '确认放弃' }}
      </button>
    </template>
  </ModalDialog>

  <ActionGuide
    v-if="guide"
    :open="guideOpen"
    :stage="guide.stage"
    :title="guide.title"
    :desc="guide.desc"
    :action-label="guide.actionLabel"
    :target="guide.target"
    @close="guideOpen = false"
    @go="onGuideGo"
  />
</template>
