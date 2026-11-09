<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { publicApi, studentApi } from '../../api'
import { notify } from '../../composables/useToast'
import {
  batchStatus,
  offerSource,
  offerStatus,
  supplementIntentStatus,
  supplementStatus,
  meta as resolveMeta
} from '../../utils/labels'
import { fmt, relativeDays } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import MentorPicker from '../../components/MentorPicker.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const saving = ref(false)
const data = ref(null)
const labs = ref([])
const selected = ref([])
const decision = reactive({ open: false, mode: 'confirm', offer: null })

const batch = computed(() => data.value?.batch || {})
const round = computed(() => data.value?.round || null)
const intents = computed(() => data.value?.intents || [])
const offers = computed(() => data.value?.offers || [])
const finalAssignment = computed(() => data.value?.finalAssignment || null)
const canApply = computed(() =>
  batch.value.status === 'SUPPLEMENT' && round.value?.status === 'OPEN' && !finalAssignment.value
)
const openOffers = computed(() => offers.value.filter((offer) => offer.status === 'OFFERED'))

async function load() {
  loading.value = true
  try {
    const [state, labData] = await Promise.all([studentApi.supplement(), publicApi.labs()])
    data.value = state
    labs.value = labData
    selected.value = (state.intents || []).map((intent) => intent.mentorId)
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function submit() {
  if (!selected.value.length) {
    notify('请至少选择 1 个补录意向', 'warn')
    return
  }
  saving.value = true
  try {
    data.value = await studentApi.applySupplement({ roundId: round.value.id, mentorIds: selected.value })
    notify('补录意向已提交，请等待实训室考核', 'success')
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

function openDecision(offer, mode) {
  decision.offer = offer
  decision.mode = mode
  decision.open = true
}

async function submitDecision() {
  saving.value = true
  try {
    if (decision.mode === 'confirm') {
      await studentApi.confirmOffer(decision.offer.id, '学生确认补录拟录取意向')
      notify('已确认补录拟录取', 'success')
    } else {
      await studentApi.declineOffer(decision.offer.id, '学生放弃补录拟录取意向')
      notify('已放弃该补录意向', 'success')
    }
    decision.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="补录填报" desc="首轮未录取的同学可在补录轮次重新填报最多 3 个意向，已录取同学无需参加。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <router-link to="/student/dashboard" class="btn btn-ghost">我的选拔</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载补录信息…</div></div>

  <template v-else>
    <div v-if="finalAssignment" class="notice notice--success">
      <Icon name="check" :size="16" />
      <span>你已完成录取（{{ finalAssignment.labName }} · {{ finalAssignment.mentorName }}），无需参加补录。</span>
    </div>
    <div v-else-if="!round" class="card card-pad">
      <EmptyState title="当前没有补录轮次" desc="学院将在补录阶段公布剩余名额与轮次安排。" icon="refresh" />
    </div>

    <template v-else>
      <section class="card card-pad">
        <div class="flex flex-wrap" style="justify-content: space-between; gap: 14px">
          <div>
            <div class="flex">
              <span class="pill pill--violet">第 {{ round.roundNo }} 轮</span>
              <StatusPill :value="round.status" :map="supplementStatus" />
            </div>
            <h2 class="mt-8">{{ round.title }}</h2>
            <p class="muted small mt-4">{{ fmt(round.startAt) }} — {{ fmt(round.endAt) }}（{{ relativeDays(round.endAt) }}）</p>
          </div>
          <div v-if="canApply" class="pill pill--green"><span class="dot" />可填报</div>
          <div v-else class="pill pill--gray">{{ round.status === 'CLOSED' ? '本轮已结束' : '暂不可填报' }}</div>
        </div>
      </section>

      <div v-if="openOffers.length" class="stack">
        <div class="card card-pad">
          <div class="section-head"><div class="section-title">补录拟录取</div><span class="pill pill--orange">{{ openOffers.length }} 个待确认</span></div>
          <div class="stack">
            <div v-for="offer in openOffers" :key="offer.id" class="offer-card highlight">
              <div class="flex" style="justify-content: space-between">
                <span class="pill pill--blue">{{ resolveMeta(offerSource, offer.source).label }}</span>
                <StatusPill :value="offer.status" :map="offerStatus" />
              </div>
              <div><strong style="font-size: 17px">{{ offer.mentorName }}</strong><div class="small muted">{{ offer.labName }}</div></div>
              <div class="button-row">
                <button class="btn btn-primary btn-sm" type="button" @click="openDecision(offer, 'confirm')">确认选择</button>
                <button class="btn btn-ghost btn-sm" type="button" @click="openDecision(offer, 'decline')">放弃</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="intents.length" class="card card-pad">
        <div class="section-head"><div class="section-title">已提交的补录意向</div></div>
        <div class="stack">
          <div v-for="(intent, index) in intents" :key="intent.id" class="offer-card" style="padding: 12px 14px">
            <div class="flex" style="justify-content: space-between">
              <div class="flex">
                <span class="badge-no">{{ index + 1 }}</span>
                <div><strong>{{ intent.mentorName }}</strong><span class="muted tiny"> · {{ intent.labName }}</span></div>
              </div>
              <StatusPill :value="intent.status" :map="supplementIntentStatus" />
            </div>
            <div v-if="intent.comment" class="tiny muted">{{ intent.comment }}</div>
          </div>
        </div>
      </div>

      <div v-if="canApply" class="split">
        <MentorPicker v-model="selected" :labs="labs" :max="3" :disabled="saving" />
        <div class="stack">
          <div class="card card-pad">
            <div class="section-title">提交补录意向</div>
            <p class="small muted mt-8">补录阶段最多填报 3 个平行意向。学院将结合剩余名额、面试情况与候补顺位统筹安排。</p>
            <button class="btn btn-primary btn-block mt-12" type="button" :disabled="saving" @click="submit">
              <Icon name="send" :size="15" /> {{ saving ? '提交中…' : '提交补录意向' }}
            </button>
          </div>
          <div class="notice notice--warn">
            <Icon name="alert" :size="15" />
            <span>补录不代表必然录取；请保持联系方式畅通，关注面试与确认通知。</span>
          </div>
        </div>
      </div>
      <div v-else-if="!finalAssignment" class="notice notice--info">
        <Icon name="eye" :size="16" />
        <span>{{ round.status === 'CLOSED' ? '本轮补录已结束，请等待下一轮或统筹调剂。' : '当前不在补录填报时间内。' }}</span>
      </div>
    </template>
  </template>

  <ModalDialog :open="decision.open" :title="decision.mode === 'confirm' ? '确认补录拟录取？' : '放弃补录拟录取？'" @close="decision.open = false">
    <div v-if="decision.offer" class="stack">
      <div class="offer-card highlight"><strong>{{ decision.offer.mentorName }}</strong><span class="small muted">{{ decision.offer.labName }}</span></div>
      <div class="notice" :class="decision.mode === 'confirm' ? 'notice--warn' : 'notice--danger'">
        <Icon name="alert" :size="16" />
        <span>{{ decision.mode === 'confirm' ? '确认后其他拟录取意向将释放，不能撤销。' : '放弃后名额将释放，不能撤销。' }}</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="decision.open = false">取消</button>
      <button class="btn" :class="decision.mode === 'confirm' ? 'btn-primary' : 'btn-danger'" type="button" :disabled="saving" @click="submitDecision">
        {{ saving ? '处理中…' : decision.mode === 'confirm' ? '确认选择' : '确认放弃' }}
      </button>
    </template>
  </ModalDialog>
</template>
