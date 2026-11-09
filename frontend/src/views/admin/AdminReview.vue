<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { batchStatus, offerStatus, reviewLevel, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const actionLoading = ref(false)
const batches = ref([])
const selectedId = ref(null)
const detail = ref(null)
const expireModal = reactive({ open: false, reason: '确认期限届满，按放弃处理' })

const batch = computed(() => detail.value?.batch || {})
const review = computed(() => detail.value?.review || {})
const issues = computed(() => review.value.issues || [])
const labs = computed(() => detail.value?.labs || [])
const mentors = computed(() => detail.value?.mentors || [])
const offers = computed(() => (detail.value?.offers || []).filter((offer) => ['OFFERED', 'CONFIRMED'].includes(offer.status)))
const canAutoConfirm = computed(() => batch.value.status === 'CONFIRM')
const canExpire = computed(() => ['CONFIRM', 'SUPPLEMENT'].includes(batch.value.status))

async function boot() {
  loading.value = true
  try {
    batches.value = await adminApi.batches()
    const current = batches.value.find((item) => !['DRAFT', 'FINISHED'].includes(item.status)) || batches.value[0]
    selectedId.value = current?.id || null
    if (selectedId.value) await loadDetail()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

async function loadDetail() {
  if (!selectedId.value) return
  detail.value = await adminApi.batch(selectedId.value)
}

onMounted(boot)

async function onSelect() {
  loading.value = true
  try {
    await loadDetail()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

async function autoConfirm() {
  if (!selectedId.value) return
  actionLoading.value = true
  try {
    const result = await adminApi.autoConfirm(selectedId.value)
    notify(`自动确认完成，共 ${result.count} 人`, 'success')
    await loadDetail()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function expireOffers() {
  actionLoading.value = true
  try {
    const result = await adminApi.expireOffers(selectedId.value, expireModal.reason)
    notify(`已将 ${result.count} 条逾期拟录取按放弃处理`, 'success')
    expireModal.open = false
    await loadDetail()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}
</script>

<template>
  <PageHeader title="初选审核" desc="检查各实训室面试、拟录取与名额情况，处理超额、缺面试与逾期确认。">
    <template #actions>
      <select v-model="selectedId" class="select" style="width: 260px" @change="onSelect">
        <option v-for="item in batches" :key="item.id" :value="item.id">{{ item.name }}</option>
      </select>
      <StatusPill v-if="batch.id" :value="batch.status" :map="batchStatus" />
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载审核数据…</div></div>

  <template v-else-if="detail">
    <div class="stat-grid">
      <StatCard label="待确认拟录取" :value="review.pendingOffers || 0" hint="学生尚未确认" tint="#fff0e6" />
      <StatCard label="缺面试记录" :value="review.offerWithoutInterview || 0" hint="拟录取必须有面试" tint="#ffeaec" />
      <StatCard label="审核问题" :value="review.issueCount || 0" :hint="`必须处理 ${review.errorCount || 0} 项`" tint="#fff5db" />
      <StatCard label="最终录取" :value="review.admittedCount || 0" hint="已确认学生" tint="#e5f8f4" />
    </div>

    <div class="notice notice--info">
      <Icon name="shield" :size="16" />
      <span>系统会校验导师与实训室名额、面试完成情况、一人一组等规则。发现问题时请先处理，再推进到确认/公示阶段。</span>
    </div>

    <div class="split">
      <div class="stack">
        <section class="card card-pad">
          <div class="section-head">
            <div class="section-title">审核问题</div>
            <span class="pill" :class="issues.length ? 'pill--red' : 'pill--green'">{{ issues.length ? `${issues.length} 项` : '全部正常' }}</span>
          </div>
          <div v-if="issues.length" class="stack" style="gap: 8px">
            <div v-for="(issue, index) in issues" :key="index" class="notice" :class="{
              'notice--danger': issue.level === 'ERROR',
              'notice--warn': issue.level === 'WARN',
              'notice--info': issue.level === 'INFO'
            }">
              <Icon :name="issue.level === 'ERROR' ? 'alert' : 'clock'" :size="15" />
              <span>{{ issue.message }}</span>
              <StatusPill :value="issue.level" :map="reviewLevel" style="margin-left: auto" />
            </div>
          </div>
          <EmptyState v-else title="没有发现异常" desc="所有拟录取均有面试记录，名额也未超额。" icon="check" />
        </section>

        <section class="card card-pad">
          <div class="section-head"><div class="section-title">导师名额与初选</div></div>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>导师</th><th>实训室</th><th>名额</th><th>拟录取/确认</th><th>待处理</th></tr></thead>
              <tbody>
                <tr v-for="mentor in mentors" :key="mentor.id">
                  <td><strong>{{ mentor.name }}</strong><div class="tiny muted">{{ mentor.title }}</div></td>
                  <td>{{ mentor.labName }}</td>
                  <td>{{ mentor.quota }}</td>
                  <td>{{ mentor.offeredCount || 0 }} / {{ mentor.confirmedCount || 0 }}</td>
                  <td>{{ mentor.applicantCount || 0 }} 人报名</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div class="stack">
        <section class="card card-pad">
          <div class="section-title">审核操作</div>
          <div class="stack mt-12" style="gap: 8px">
            <button class="btn btn-soft btn-block" type="button" :disabled="actionLoading || !canAutoConfirm" @click="autoConfirm">
              <Icon name="check" :size="15" /> 自动确认唯一拟录取
            </button>
            <button class="btn btn-orange btn-block" type="button" :disabled="actionLoading || !canExpire" @click="expireModal.open = true">
              <Icon name="clock" :size="15" /> 处理逾期未确认
            </button>
            <div class="tiny muted">自动确认仅在“拟录取确认”阶段可用；逾期处理在确认/补录阶段可用。</div>
            <button class="btn btn-ghost btn-block" type="button" :disabled="actionLoading" @click="loadDetail">刷新数据</button>
          </div>
        </section>

        <section class="card card-pad">
          <div class="section-head"><div class="section-title">拟录取记录</div><span class="pill pill--gray">{{ offers.length }} 条</span></div>
          <div v-if="offers.length" class="stack" style="gap: 7px">
            <div v-for="offer in offers" :key="offer.id" class="offer-card" style="padding: 10px 12px">
              <div class="flex" style="justify-content: space-between">
                <strong class="small">{{ offer.studentName }}</strong>
                <StatusPill :value="offer.status" :map="offerStatus" />
              </div>
              <div class="tiny muted">{{ offer.mentorName }} · {{ offer.labName }} · {{ fmt(offer.createdAt) }}</div>
            </div>
          </div>
          <EmptyState v-else title="暂无拟录取记录" desc="导师完成初选后会出现在这里。" icon="award" />
        </section>
      </div>
    </div>
  </template>

  <ModalDialog :open="expireModal.open" title="处理逾期未确认拟录取" subtitle="仅处理确认期限届满仍处于待确认状态的记录" @close="expireModal.open = false">
    <div class="stack">
      <div class="notice notice--danger">
        <Icon name="alert" :size="16" />
        <span>逾期记录将标记为“已逾期”，对应意向改为“已放弃”，名额进入补录。该操作会写入审计日志。</span>
      </div>
      <div class="field">
        <label class="label">处理原因</label>
        <textarea v-model="expireModal.reason" class="textarea" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="expireModal.open = false">取消</button>
      <button class="btn btn-orange" type="button" :disabled="actionLoading" @click="expireOffers">{{ actionLoading ? '处理中…' : '确认处理' }}</button>
    </template>
  </ModalDialog>
</template>
