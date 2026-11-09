<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { announcementStatus, announcementType, meta as resolveMeta } from '../../utils/labels'
import { fmt, fromInput, toInput } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const saving = ref(false)
const announcements = ref([])
const batches = ref([])
const filterBatch = ref('')
const modal = reactive({ open: false, id: null })

const form = reactive({
  batchId: null, title: '', type: 'GENERAL', content: '', status: 'DRAFT', publishStart: '', publishEnd: ''
})

const types = ['GENERAL', 'LECTURE', 'OPEN_DAY', 'INTERVIEW', 'SUPPLEMENT', 'PUBLICITY']

async function load() {
  loading.value = true
  try {
    const [list, batchList] = await Promise.all([
      adminApi.announcements(filterBatch.value || undefined),
      adminApi.batches()
    ])
    announcements.value = list
    batches.value = batchList
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate() {
  modal.id = null
  Object.assign(form, {
    batchId: batches.value.find((item) => !['DRAFT', 'FINISHED'].includes(item.status))?.id || batches.value[0]?.id || null,
    title: '', type: 'GENERAL', content: '', status: 'DRAFT', publishStart: '', publishEnd: ''
  })
  modal.open = true
}

function openEdit(item) {
  modal.id = item.id
  Object.assign(form, {
    batchId: item.batchId, title: item.title || '', type: item.type || 'GENERAL', content: item.content || '',
    status: item.status || 'DRAFT', publishStart: toInput(item.publishStart), publishEnd: toInput(item.publishEnd)
  })
  modal.open = true
}

async function save() {
  if (!form.title.trim()) {
    notify('请填写公告标题', 'warn')
    return
  }
  saving.value = true
  try {
    const payload = {
      batchId: form.batchId,
      title: form.title,
      type: form.type,
      content: form.content,
      status: form.status,
      publishStart: fromInput(form.publishStart),
      publishEnd: fromInput(form.publishEnd)
    }
    if (modal.id) await adminApi.updateAnnouncement(modal.id, payload)
    else await adminApi.createAnnouncement(payload)
    notify(modal.id ? '公告已更新' : '公告已保存', 'success')
    modal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function publish(item) {
  try {
    await adminApi.publishAnnouncement(item.id)
    notify('公告已发布', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  }
}

async function remove(item) {
  if (!window.confirm(`确认删除公告「${item.title}」？`)) return
  try {
    await adminApi.deleteAnnouncement(item.id)
    notify('公告已删除', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  }
}
</script>

<template>
  <PageHeader title="公告管理" desc="发布集中宣讲、开放日、面试安排、补录与公示通知。">
    <template #actions>
      <select v-model="filterBatch" class="select" style="width: 220px" @change="load">
        <option value="">全部批次</option>
        <option v-for="batch in batches" :key="batch.id" :value="batch.id">{{ batch.name }}</option>
      </select>
      <button class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" /> 新建公告</button>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载公告…</div></div>

  <div v-else-if="announcements.length" class="stack">
    <article v-for="item in announcements" :key="item.id" class="card card-pad">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 12px">
        <div style="min-width: 0">
          <div class="flex">
            <StatusPill :value="item.status" :map="announcementStatus" />
            <StatusPill :value="item.type" :map="announcementType" />
            <span class="pill pill--gray">{{ item.batchName || '全局' }}</span>
          </div>
          <h3 class="mt-8">{{ item.title }}</h3>
          <p class="small muted mt-4" style="display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden">{{ item.content }}</p>
          <div class="tiny muted mt-8">展示时间 {{ fmt(item.publishStart) }} — {{ fmt(item.publishEnd) }}</div>
        </div>
        <div class="button-row">
          <button v-if="item.status !== 'PUBLISHED'" class="btn btn-primary btn-sm" type="button" @click="publish(item)">发布</button>
          <button class="btn btn-soft btn-sm" type="button" @click="openEdit(item)">编辑</button>
          <button class="btn btn-danger btn-sm" type="button" @click="remove(item)">删除</button>
        </div>
      </div>
    </article>
  </div>
  <EmptyState v-else title="暂无公告" desc="发布公告后，学生会在公开首页与工作台看到。" icon="bell">
    <button class="btn btn-primary btn-sm" type="button" @click="openCreate">新建公告</button>
  </EmptyState>

  <ModalDialog :open="modal.open" :title="modal.id ? '编辑公告' : '新建公告'" wide @close="modal.open = false">
    <div class="form-grid">
      <div class="field">
        <label class="label">所属批次</label>
        <select v-model="form.batchId" class="select">
          <option v-for="batch in batches" :key="batch.id" :value="batch.id">{{ batch.name }}</option>
        </select>
      </div>
      <div class="field">
        <label class="label">公告类型</label>
        <select v-model="form.type" class="select">
          <option v-for="type in types" :key="type" :value="type">{{ resolveMeta(announcementType, type).label }}</option>
        </select>
      </div>
      <div class="field span-2">
        <label class="label">标题 <span class="req">*</span></label>
        <input v-model="form.title" class="input" />
      </div>
      <div class="field">
        <label class="label">状态</label>
        <select v-model="form.status" class="select">
          <option value="DRAFT">草稿</option><option value="PUBLISHED">发布</option><option value="CLOSED">关闭</option>
        </select>
      </div>
      <div class="field">
        <label class="label">展示开始</label>
        <input v-model="form.publishStart" class="input" type="datetime-local" />
      </div>
      <div class="field">
        <label class="label">展示结束</label>
        <input v-model="form.publishEnd" class="input" type="datetime-local" />
      </div>
      <div class="field span-2">
        <label class="label">内容</label>
        <textarea v-model="form.content" class="textarea" style="min-height: 160px" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </template>
  </ModalDialog>
</template>
