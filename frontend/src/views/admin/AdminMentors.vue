<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const records = ref([])
const labs = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const labFilter = ref('')

const modal = reactive({ open: false, id: null })
const passwordModal = reactive({ open: false, id: null, name: '', password: '123456' })
const saving = ref(false)

const form = reactive({
  account: '', password: '', name: '', mentorNo: '', title: '', labId: null,
  researchDirection: '', profile: '', quota: 2, enabled: true
})

const labOptions = computed(() => labs.value.map((lab) => ({ id: lab.id, name: lab.name })))

async function load() {
  loading.value = true
  try {
    const [data, labData] = await Promise.all([
      adminApi.mentors({ keyword: keyword.value || undefined, labId: labFilter.value || undefined, page: page.value, size: size.value }),
      labs.value.length ? Promise.resolve(labs.value) : adminApi.labs()
    ])
    records.value = data.records
    total.value = data.total
    labs.value = labData
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function search() {
  page.value = 1
  load()
}

function openCreate() {
  modal.id = null
  Object.assign(form, {
    account: '', password: '', name: '', mentorNo: '', title: '', labId: labs.value[0]?.id || null,
    researchDirection: '', profile: '', quota: 2, enabled: true
  })
  modal.open = true
}

function openEdit(mentor) {
  modal.id = mentor.id
  Object.assign(form, {
    account: mentor.account || '', password: '', name: mentor.name || '', mentorNo: mentor.mentorNo || '',
    title: mentor.title || '', labId: mentor.labId, researchDirection: mentor.researchDirection || '',
    profile: mentor.profile || '', quota: mentor.quota ?? 0, enabled: mentor.enabled !== false
  })
  modal.open = true
}

async function save() {
  if (!form.name || !form.labId) {
    notify('请填写导师姓名并选择实训室', 'warn')
    return
  }
  saving.value = true
  try {
    const payload = {
      account: form.account || null,
      password: form.password || null,
      name: form.name,
      mentorNo: form.mentorNo || null,
      title: form.title,
      labId: Number(form.labId),
      researchDirection: form.researchDirection,
      profile: form.profile,
      quota: Number(form.quota),
      enabled: form.enabled
    }
    if (modal.id) await adminApi.updateMentor(modal.id, payload)
    else await adminApi.createMentor(payload)
    notify(modal.id ? '导师已更新' : '导师已创建', 'success')
    modal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function toggle(mentor) {
  try {
    await adminApi.toggleMentor(mentor.id, !mentor.enabled)
    notify('导师状态已更新', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  }
}

function openPassword(mentor) {
  passwordModal.id = mentor.id
  passwordModal.name = mentor.name
  passwordModal.password = '123456'
  passwordModal.open = true
}

async function resetPassword() {
  saving.value = true
  try {
    await adminApi.resetMentorPassword(passwordModal.id, passwordModal.password)
    notify('密码已重置', 'success')
    passwordModal.open = false
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <PageHeader title="导师管理" desc="维护导师账号、所属实训室、研究方向、招生名额与启用状态。">
    <template #actions>
      <button class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" /> 新增导师</button>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 13px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
      <div class="flex" style="gap: 8px">
        <div style="position: relative">
          <Icon name="search" :size="15" style="position: absolute; left: 11px; top: 9px; color: var(--muted-2)" />
          <input v-model="keyword" class="input" style="padding-left: 32px; width: 220px" placeholder="姓名 / 工号 / 方向" @keyup.enter="search" />
        </div>
        <select v-model="labFilter" class="select" style="width: 190px" @change="search">
          <option value="">全部实训室</option>
          <option v-for="lab in labOptions" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
        </select>
        <button class="btn btn-soft btn-sm" @click="search">查询</button>
      </div>
      <span class="small muted">共 {{ total }} 位导师</span>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载导师…</div>
    <div v-else-if="records.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>导师</th><th>实训室</th><th>研究方向</th><th>名额</th><th>报名/拟录取</th><th>状态</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="mentor in records" :key="mentor.id">
            <td><strong>{{ mentor.name }}</strong><div class="tiny muted">{{ mentor.mentorNo }} · {{ mentor.title }}</div></td>
            <td>{{ mentor.labName }}</td>
            <td><span class="tiny muted">{{ mentor.researchDirection }}</span></td>
            <td><strong>{{ mentor.quota }}</strong></td>
            <td>{{ mentor.applicantCount || 0 }} / {{ mentor.offeredCount || 0 }}</td>
            <td><StatusPill :value="mentor.enabled ? 'ACTIVE' : 'DISABLED'" :map="{ ACTIVE: { label: '启用', tone: 'green' }, DISABLED: { label: '停用', tone: 'gray' } }" /></td>
            <td class="right">
              <div class="actions">
                <button class="btn btn-ghost btn-sm" type="button" @click="openPassword(mentor)">重置密码</button>
                <button class="btn btn-ghost btn-sm" type="button" @click="toggle(mentor)">{{ mentor.enabled ? '停用' : '启用' }}</button>
                <button class="btn btn-soft btn-sm" type="button" @click="openEdit(mentor)">编辑</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无导师" desc="新增导师账号后即可参与选拔。" icon="users">
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate">新增导师</button>
    </EmptyState>
    <div v-if="total > size" class="flex" style="justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line)">
      <span class="small muted">第 {{ page }} 页</span>
      <div class="button-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <button class="btn btn-ghost btn-sm" :disabled="page * size >= total" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>

  <ModalDialog :open="modal.open" :title="modal.id ? '编辑导师' : '新增导师'" wide @close="modal.open = false">
    <div class="form-grid">
      <div class="field">
        <label class="label">姓名 <span class="req">*</span></label>
        <input v-model="form.name" class="input" />
      </div>
      <div class="field">
        <label class="label">所属实训室 <span class="req">*</span></label>
        <select v-model="form.labId" class="select" :disabled="Boolean(modal.id)">
          <option v-for="lab in labOptions" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
        </select>
      </div>
      <div class="field">
        <label class="label">登录账号</label>
        <input v-model="form.account" class="input" :disabled="Boolean(modal.id)" :placeholder="modal.id ? '' : '留空自动生成，如 t1007'" />
      </div>
      <div v-if="!modal.id" class="field">
        <label class="label">初始密码</label>
        <input v-model="form.password" class="input" placeholder="留空默认 123456" />
      </div>
      <div class="field">
        <label class="label">工号</label>
        <input v-model="form.mentorNo" class="input" :disabled="Boolean(modal.id)" placeholder="留空自动生成" />
      </div>
      <div class="field">
        <label class="label">职称</label>
        <input v-model="form.title" class="input" placeholder="教授 / 副教授 / 讲师" />
      </div>
      <div class="field">
        <label class="label">招生名额</label>
        <input v-model.number="form.quota" class="input" type="number" min="0" />
      </div>
      <div class="field">
        <label class="label">状态</label>
        <select v-model="form.enabled" class="select">
          <option :value="true">启用</option><option :value="false">停用</option>
        </select>
      </div>
      <div class="field span-2">
        <label class="label">研究方向</label>
        <input v-model="form.researchDirection" class="input" />
      </div>
      <div class="field span-2">
        <label class="label">导师简介</label>
        <textarea v-model="form.profile" class="textarea" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="passwordModal.open" title="重置导师密码" :subtitle="passwordModal.name" @close="passwordModal.open = false">
    <div class="field">
      <label class="label">新密码</label>
      <input v-model="passwordModal.password" class="input" placeholder="至少 6 位" />
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="passwordModal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="resetPassword">确认重置</button>
    </template>
  </ModalDialog>
</template>
