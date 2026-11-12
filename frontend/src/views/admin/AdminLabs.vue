<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import { quotaPercent } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatCard from '../../components/StatCard.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const labs = ref([])
const modal = reactive({ open: false, id: null })
const saving = ref(false)

const form = reactive({
  name: '', code: '', intro: '', direction: '', trainingPlan: '', requirements: '',
  contact: '', location: '', coverImage: '', totalQuota: 4, status: 'ACTIVE'
})

const totalQuota = computed(() => labs.value.reduce((sum, lab) => sum + (lab.totalQuota || 0), 0))
const activeCount = computed(() => labs.value.filter((lab) => lab.status === 'ACTIVE').length)

async function load() {
  loading.value = true
  try {
    labs.value = await adminApi.labs()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function openCreate() {
  modal.id = null
  Object.assign(form, { name: '', code: '', intro: '', direction: '', trainingPlan: '', requirements: '', contact: '', location: '', coverImage: '', totalQuota: 4, status: 'ACTIVE' })
  modal.open = true
}

function openEdit(lab) {
  modal.id = lab.id
  Object.assign(form, {
    name: lab.name || '', code: lab.code || '', intro: lab.intro || '', direction: lab.direction || '',
    trainingPlan: lab.trainingPlan || '', requirements: lab.requirements || '', contact: lab.contact || '',
    location: lab.location || '', coverImage: lab.coverImage || '',
    totalQuota: lab.totalQuota ?? 0, status: lab.status || 'ACTIVE'
  })
  modal.open = true
}

async function save() {
  if (!form.name || !form.code) {
    notify('请填写实训室名称与编码', 'warn')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, totalQuota: Number(form.totalQuota) }
    if (modal.id) await adminApi.updateLab(modal.id, payload)
    else await adminApi.createLab(payload)
    notify(modal.id ? '实训室已更新' : '实训室已创建', 'success')
    modal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function toggle(lab) {
  const enabled = lab.status !== 'ACTIVE'
  if (!window.confirm(`确认${enabled ? '启用' : '停用'}「${lab.name}」？`)) return
  try {
    await adminApi.toggleLab(lab.id, enabled)
    notify('状态已更新', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  }
}
</script>

<template>
  <PageHeader title="实训室管理" desc="维护实训室简介、研究方向、培养方式、名额与启用状态。">
    <template #actions>
      <button class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" /> 新建实训室</button>
    </template>
  </PageHeader>

  <div class="stat-grid">
    <StatCard label="实训室" :value="labs.length" :hint="`启用 ${activeCount} 个`" tint="#eaf2fc" />
    <StatCard label="总名额" :value="totalQuota" hint="全部实训室" tint="#f3f8fe" />
    <StatCard label="导师总数" :value="labs.reduce((sum, lab) => sum + (lab.mentorCount || 0), 0)" hint="各实训室合计" tint="#fff4e2" />
    <StatCard label="已录取" :value="labs.reduce((sum, lab) => sum + (lab.confirmedCount || 0), 0)" hint="当前批次" tint="#e8f7f1" />
  </div>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载实训室…</div></div>

  <div v-else-if="labs.length" class="stack">
    <article v-for="lab in labs" :key="lab.id" class="card card-pad">
      <div class="flex flex-wrap" style="justify-content: space-between; gap: 14px">
        <div class="flex" style="gap: 14px; align-items: flex-start">
          <img v-if="lab.coverImage" :src="lab.coverImage" :alt="lab.name" style="width: 138px; height: 88px; object-fit: cover; border-radius: 12px; flex: 0 0 auto" />
          <div>
            <div class="flex">
              <span class="pill" :class="lab.status === 'ACTIVE' ? 'pill--green' : 'pill--gray'">{{ lab.status === 'ACTIVE' ? '启用中' : '已停用' }}</span>
              <span class="pill pill--gray">{{ lab.code }}</span>
              <span v-if="lab.collegeName" class="pill pill--gray">{{ lab.collegeName }}</span>
            </div>
            <h3 class="mt-8">{{ lab.name }}</h3>
            <p class="small muted mt-4">{{ lab.direction }}</p>
            <p class="tiny muted mt-4">{{ lab.location || '未设置空间位置' }}</p>
          </div>
        </div>
        <div class="button-row">
          <button class="btn btn-ghost btn-sm" type="button" @click="toggle(lab)">{{ lab.status === 'ACTIVE' ? '停用' : '启用' }}</button>
          <button class="btn btn-soft btn-sm" type="button" @click="openEdit(lab)"><Icon name="edit" :size="14" /> 编辑</button>
        </div>
      </div>
      <div class="grid-4 mt-16">
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">总名额</div><div class="strong">{{ lab.totalQuota }}</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">导师</div><div class="strong">{{ lab.mentorCount || 0 }} 位</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">报名</div><div class="strong">{{ lab.applicantCount || 0 }} 人</div></div>
        <div class="offer-card" style="padding: 12px"><div class="tiny muted">剩余名额</div><div class="strong">{{ lab.remainingQuota ?? '—' }}</div></div>
      </div>
      <div class="quota-bar mt-12"><span :style="{ width: `${quotaPercent(lab.confirmedCount || 0, lab.totalQuota)}%` }" /></div>
      <div class="tiny muted mt-4">已录取 {{ lab.confirmedCount || 0 }} / {{ lab.totalQuota }}</div>
    </article>
  </div>
  <EmptyState v-else title="暂无实训室" desc="创建实训室后即可配置导师与名额。" icon="flask">
    <button class="btn btn-primary btn-sm" type="button" @click="openCreate">新建实训室</button>
  </EmptyState>

  <ModalDialog :open="modal.open" :title="modal.id ? '编辑实训室' : '新建实训室'" wide @close="modal.open = false">
    <div class="form-grid">
      <div class="field">
        <label class="label">实训室名称 <span class="req">*</span></label>
        <input v-model="form.name" class="input" />
      </div>
      <div class="field">
        <label class="label">编码 <span class="req">*</span></label>
        <input v-model="form.code" class="input" placeholder="例如 LAB-AIOT" />
      </div>
      <div class="field">
        <label class="label">总名额</label>
        <input v-model.number="form.totalQuota" class="input" type="number" min="0" />
      </div>
      <div class="field">
        <label class="label">状态</label>
        <select v-model="form.status" class="select">
          <option value="ACTIVE">启用</option><option value="DISABLED">停用</option>
        </select>
      </div>
      <div class="field span-2">
        <label class="label">研究方向</label>
        <input v-model="form.direction" class="input" placeholder="例如：边缘智能、嵌入式视觉、工业物联网" />
      </div>
      <div class="field span-2">
        <label class="label">简介</label>
        <textarea v-model="form.intro" class="textarea" />
      </div>
      <div class="field span-2">
        <label class="label">培养安排</label>
        <textarea v-model="form.trainingPlan" class="textarea" />
      </div>
      <div class="field span-2">
        <label class="label">选拔要求</label>
        <textarea v-model="form.requirements" class="textarea" />
      </div>
      <div class="field">
        <label class="label">空间位置</label>
        <input v-model="form.location" class="input" placeholder="例如：信息楼 A301" />
      </div>
      <div class="field">
        <label class="label">封面图</label>
        <select v-model="form.coverImage" class="select">
          <option value="">使用默认渐变封面</option>
          <option v-for="index in 6" :key="index" :value="`/lab-covers/lab-${index}.jpg`">空间实景 {{ index }}</option>
        </select>
      </div>
      <div class="field span-2">
        <label class="label">联系方式</label>
        <input v-model="form.contact" class="input" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </template>
  </ModalDialog>
</template>
