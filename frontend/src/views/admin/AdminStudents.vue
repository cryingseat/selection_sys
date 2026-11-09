<script setup>
import { onMounted, reactive, ref } from 'vue'
import { adminApi } from '../../api'
import { notify } from '../../composables/useToast'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import StatusPill from '../../components/StatusPill.vue'
import EmptyState from '../../components/EmptyState.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const loading = ref(true)
const records = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const grade = ref('')
const modal = reactive({ open: false, id: null })
const passwordModal = reactive({ open: false, id: null, name: '', password: '123456' })
const saving = ref(false)

const form = reactive({
  account: '', password: '', studentNo: '', name: '', gender: '男', major: '', className: '', grade: '2023', phone: '', email: ''
})

async function load() {
  loading.value = true
  try {
    const data = await adminApi.students({ keyword: keyword.value || undefined, grade: grade.value || undefined, page: page.value, size: size.value })
    records.value = data.records
    total.value = data.total
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
  Object.assign(form, { account: '', password: '', studentNo: '', name: '', gender: '男', major: '', className: '', grade: '2023', phone: '', email: '' })
  modal.open = true
}

function openEdit(student) {
  modal.id = student.id
  Object.assign(form, {
    account: student.account || '', password: '', studentNo: student.studentNo || '', name: student.name || '',
    gender: student.gender || '男', major: student.major || '', className: student.className || '',
    grade: student.grade || '', phone: student.phone || '', email: student.email || ''
  })
  modal.open = true
}

async function save() {
  if (!form.name || !form.studentNo) {
    notify('请填写姓名与学号', 'warn')
    return
  }
  saving.value = true
  try {
    const payload = {
      account: form.account || null,
      password: form.password || null,
      studentNo: form.studentNo,
      name: form.name,
      gender: form.gender,
      major: form.major,
      className: form.className,
      grade: form.grade,
      phone: form.phone,
      email: form.email
    }
    if (modal.id) await adminApi.updateStudent(modal.id, payload)
    else await adminApi.createStudent(payload)
    notify(modal.id ? '学生信息已更新' : '学生已创建', 'success')
    modal.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function toggle(student) {
  try {
    await adminApi.toggleStudent(student.id, !student.enabled)
    notify('学生账号状态已更新', 'success')
    await load()
  } catch (err) {
    notify(err.message, 'error')
  }
}

function openPassword(student) {
  passwordModal.id = student.id
  passwordModal.name = student.name
  passwordModal.password = '123456'
  passwordModal.open = true
}

async function resetPassword() {
  saving.value = true
  try {
    await adminApi.resetStudentPassword(passwordModal.id, passwordModal.password)
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
  <PageHeader title="学生管理" desc="维护参与选拔的学生名单、学号、专业、班级与账号状态。">
    <template #actions>
      <button class="btn btn-primary" type="button" @click="openCreate"><Icon name="plus" :size="15" /> 新增学生</button>
    </template>
  </PageHeader>

  <div class="card card-pad" style="padding: 13px 18px">
    <div class="flex flex-wrap" style="justify-content: space-between; gap: 10px">
      <div class="flex" style="gap: 8px">
        <div style="position: relative">
          <Icon name="search" :size="15" style="position: absolute; left: 11px; top: 9px; color: var(--muted-2)" />
          <input v-model="keyword" class="input" style="padding-left: 32px; width: 220px" placeholder="姓名 / 学号 / 专业" @keyup.enter="search" />
        </div>
        <input v-model="grade" class="input" style="width: 120px" placeholder="年级，如 2023" @keyup.enter="search" />
        <button class="btn btn-soft btn-sm" @click="search">查询</button>
      </div>
      <span class="small muted">共 {{ total }} 名学生</span>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="loading-block"><div class="spinner" />正在加载学生…</div>
    <div v-else-if="records.length" class="table-wrap" style="border: 0">
      <table class="table">
        <thead><tr><th>学生</th><th>专业 / 班级</th><th>年级</th><th>联系方式</th><th>账号</th><th>状态</th><th class="right">操作</th></tr></thead>
        <tbody>
          <tr v-for="student in records" :key="student.id">
            <td><strong>{{ student.name }}</strong><div class="tiny muted">{{ student.studentNo }}</div></td>
            <td>{{ student.major }}<div class="tiny muted">{{ student.className }}</div></td>
            <td>{{ student.grade }}</td>
            <td><span class="tiny">{{ student.phone || '—' }}</span><div class="tiny muted">{{ student.email || '' }}</div></td>
            <td>{{ student.account }}</td>
            <td><StatusPill :value="student.enabled ? 'ACTIVE' : 'DISABLED'" :map="{ ACTIVE: { label: '启用', tone: 'green' }, DISABLED: { label: '停用', tone: 'gray' } }" /></td>
            <td class="right">
              <div class="actions">
                <button class="btn btn-ghost btn-sm" type="button" @click="openPassword(student)">重置密码</button>
                <button class="btn btn-ghost btn-sm" type="button" @click="toggle(student)">{{ student.enabled ? '停用' : '启用' }}</button>
                <button class="btn btn-soft btn-sm" type="button" @click="openEdit(student)">编辑</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <EmptyState v-else title="暂无学生" desc="新增或导入学生后即可参与选拔。" icon="book">
      <button class="btn btn-primary btn-sm" type="button" @click="openCreate">新增学生</button>
    </EmptyState>
    <div v-if="total > size" class="flex" style="justify-content: space-between; padding: 12px 16px; border-top: 1px solid var(--line)">
      <span class="small muted">第 {{ page }} 页</span>
      <div class="button-row">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <button class="btn btn-ghost btn-sm" :disabled="page * size >= total" @click="page++; load()">下一页</button>
      </div>
    </div>
  </div>

  <ModalDialog :open="modal.open" :title="modal.id ? '编辑学生' : '新增学生'" wide @close="modal.open = false">
    <div class="form-grid">
      <div class="field">
        <label class="label">姓名 <span class="req">*</span></label>
        <input v-model="form.name" class="input" />
      </div>
      <div class="field">
        <label class="label">学号 <span class="req">*</span></label>
        <input v-model="form.studentNo" class="input" :disabled="Boolean(modal.id)" />
      </div>
      <div class="field">
        <label class="label">登录账号</label>
        <input v-model="form.account" class="input" :disabled="Boolean(modal.id)" placeholder="留空使用学号" />
      </div>
      <div v-if="!modal.id" class="field">
        <label class="label">初始密码</label>
        <input v-model="form.password" class="input" placeholder="留空默认 123456" />
      </div>
      <div class="field">
        <label class="label">性别</label>
        <select v-model="form.gender" class="select"><option>男</option><option>女</option></select>
      </div>
      <div class="field">
        <label class="label">年级</label>
        <input v-model="form.grade" class="input" placeholder="2023" />
      </div>
      <div class="field">
        <label class="label">专业</label>
        <input v-model="form.major" class="input" />
      </div>
      <div class="field">
        <label class="label">班级</label>
        <input v-model="form.className" class="input" />
      </div>
      <div class="field">
        <label class="label">手机</label>
        <input v-model="form.phone" class="input" />
      </div>
      <div class="field">
        <label class="label">邮箱</label>
        <input v-model="form.email" class="input" />
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="modal.open = false">取消</button>
      <button class="btn btn-primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存' }}</button>
    </template>
  </ModalDialog>

  <ModalDialog :open="passwordModal.open" title="重置学生密码" :subtitle="passwordModal.name" @close="passwordModal.open = false">
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
