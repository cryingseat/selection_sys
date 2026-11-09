<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, publicityStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, fromInput, toInput } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const actionLoading = ref(false)
const dashboard = ref(null)
const publicities = ref([])
const formModal = reactive({ open: false })
const detailModal = reactive({ open: false, loading: false, data: null })
const revokeModal = reactive({ open: false, publicity: null, reason: '公示内容需要更正' })
const form = reactive({ title: '', content: '', startAt: '', endAt: '' })

const batch = computed(() => dashboard.value?.batch || {})
const canManage = computed(() => batch.value.status === 'PUBLICITY')

async function load() {
  loading.value = true
  try {
    const [dash, list] = await Promise.all([adminApi.dashboard(), adminApi.publicities()])
    dashboard.value = dash
    publicities.value = list
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate() {
  form.title = `${batch.value.name}拟录取名单公示`
  form.content = '经集中宣讲、学生填报、统一面试、导师初选、学生确认、分轮补录和统筹调剂，现将拟录取名单予以公示。公示期间如有异议，请向学院教务办公室实名反映。'
  form.startAt = toInput(batch.value.publicityStart)
  form.endAt = toInput(batch.value.publicityEnd)
  formModal.open = true
}

async function save() {
  actionLoading.value = true
  try {
    await adminApi.savePublicity({
      batchId: batch.value.id,
      title: form.title,
      content: form.content,
      startAt: fromInput(form.startAt),
      endAt: fromInput(form.endAt)
    })
    notify('公示草稿已保存', 'success')
    formModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function publish(publicity) {
  actionLoading.value = true
  try {
    await adminApi.publishPublicity(publicity.id)
    notify('公示已发布，结果进入公示状态', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function close(publicity) {
  if (!window.confirm('确认结束公示并锁定最终录取结果？')) return
  actionLoading.value = true
  try {
    await adminApi.closePublicity(publicity.id)
    notify('公示已结束，最终结果已锁定', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function revoke() {
  actionLoading.value = true
  try {
    await adminApi.revokePublicity(revokeModal.publicity.id, revokeModal.reason)
    notify('公示已撤回，结果解锁并回到调剂阶段', 'success')
    revokeModal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    actionLoading.value = false
  }
}

async function openDetail(publicity) {
  detailModal.open = true
  detailModal.loading = true
  detailModal.data = null
  try {
    detailModal.data = await adminApi.publicity(publicity.id)
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    detailModal.loading = false
  }
}
</script>

<template>
  <PageHeader title="结果公示" desc="生成最终录取名单公示；公示发布后结果锁定，修改需先撤回公示。">
    <template #actions>
      <StatusPill :value="batch.status" :map="batchStatus" />
      <button class="btn btn-primary" type="button" :disabled="!canManage" @click="openCreate"><Icon name="plus" :size="15" /> 生成公示草稿</button>
      <router-link to="/" class="btn btn-ghost">查看公开页</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载公示数据…</div></div>

  <template v-else>
    <div v-if="!canManage" class="notice notice--info">
      <Icon name="eye" :size="16" /><span>当前批次不在「结果公示」阶段，公示操作为只读。</span>
    </div>
    <div class="notice notice--warn">
      <Icon name="shield" :size="16" />
      <span>发布公示后，所有录取结果进入公示状态并禁止改动；结束公示后结果锁定、批次完成。如需更正，请撤回公示。</span>
    </div>

    <div v-if="publicities.length" class="stack">
      <article v-for="publicity in publicities" :key="publicity.id" class="card card-pad">
        <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
          <div>
            <div class="flex">
              <StatusPill :value="publicity.status" :map="publicityStatus" />
              <span class="pill pill--gray">共 {{ publicity.admittedCount }} 人</span>
            </div>
            <h3 class="mt-8">{{ publicity.title }}</h3>
            <p class="small muted mt-4">{{ fmt(publicity.startAt) }} — {{ fmt(publicity.endAt) }}</p>
          </div>
          <div class="button-row">
            <button class="btn btn-ghost btn-sm" type="button" @click="openDetail(publicity)">查看名单</button>
            <button v-if="publicity.status === 'DRAFT'" class="btn btn-primary btn-sm" type="button" :disabled="actionLoading || !canManage" @click="publish(publicity)">发布公示</button>
            <button v-if="publicity.status === 'PUBLISHED'" class="btn btn-orange btn-sm" type="button" :disabled="actionLoading || !canManage" @click="close(publicity)">结束并锁定</button>
            <button
              v-if="['PUBLISHED', 'CLOSED'].includes(publicity.status)"
              class="btn btn-danger btn-sm"
              type="button"
              :disabled="actionLoading"
              @click="revokeModal.publicity = publicity; revokeModal.open = true"
            >
              撤回公示
            </button>
          </div>
        </div>
        <p class="small muted mt-12" style="white-space: pre-wrap">{{ publicity.content }}</p>
      </article>
    </div>
    <EmptyState v-else title="暂无公示" desc="完成录取后，在此生成最终名单公示草稿。" icon="award">
      <button class="btn btn-primary btn-sm" type="button" :disabled="!canManage" @click="openCreate">生成公示草稿</button>
    </EmptyState>
  </template>

  <ModalDialog :open="formModal.open" title="生成 / 更新公示草稿" wide @close="formModal.open = false">
    <div class="stack">
      <div class="field">
        <label class="label">公示标题</label>
        <input v-model="form.title" class="input" />
      </div>
      <div class="form-grid">
        <div class="field">
          <label class="label">开始时间</label>
          <input v-model="form.startAt" class="input" type="datetime-local" />
        </div>
        <div class="field">
          <label class="label">结束时间</label>
          <input v-model="form.endAt" class="input" type="datetime-local" />
        </div>
      </div>
      <div class="field">
        <label class="label">公示内容</label>
        <textarea v-model="form.content" class="textarea" style="min-height: 150px" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="formModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="actionLoading" @click="save">保存草稿</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="detailModal.open" title="公示名单" wide @close="detailModal.open = false">
    <div v-if="detailModal.loading" class="loading-block"><div class="spinner" />加载中…</div>
    <div v-else-if="detailModal.data" class="stack">
      <div class="flex" style="justify-content: space-between">
        <h3>{{ detailModal.data.publicity.title }}</h3>
        <StatusPill :value="detailModal.data.publicity.status" :map="publicityStatus" />
      </div>
      <div class="table-wrap">
        <table class="table">
          <thead><tr><th>学生</th><th>专业</th><th>实训室</th><th>导师</th><th>录取方式</th></tr></thead>
          <tbody>
            <tr v-for="row in detailModal.data.assignments" :key="row.id">
              <td><strong>{{ row.studentName }}</strong><div class="tiny muted">{{ row.studentNo }}</div></td>
              <td>{{ row.major }}</td>
              <td>{{ row.labName }}</td>
              <td>{{ row.mentorName }}</td>
              <td><span class="pill pill--blue">{{ resolveMeta(assignmentSource, row.source).label }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <template #footer><button class="btn btn-ghost" type="button" @click="detailModal.open = false">关闭</button></template>
  </ModalDialog>

  <ModalDialog :open="revokeModal.open" title="撤回公示" @close="revokeModal.open = false">
    <div class="stack">
      <div class="notice notice--danger">
        <Icon name="alert" :size="16" />
        <span>撤回后公示结果不再公开，录取结果解锁并回到调剂阶段。修改后需重新发布公示。</span>
      </div>
      <div class="field">
        <label class="label">撤回原因</label>
        <textarea v-model="revokeModal.reason" class="textarea" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="revokeModal.open = false">取消</button>
      <button class="btn btn-danger" type="button" :disabled="actionLoading" @click="revoke">确认撤回</button>
    </template>
  </ModalDialog>
</template>
