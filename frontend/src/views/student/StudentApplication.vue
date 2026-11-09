<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi, studentApi } from '../../api'
import { notify } from '../../composables/useToast'
import { applicationStatus, batchStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import PageHeader from '../../components/PageHeader.vue'
import MentorPicker from '../../components/MentorPicker.vue'
import StatusPill from '../../components/StatusPill.vue'
import ModalDialog from '../../components/ModalDialog.vue'

const router = useRouter()
const loading = ref(true)
const saving = ref(false)
const dashboard = ref(null)
const labs = ref([])

const form = reactive({
  mentorIds: [],
  intro: '',
  skills: '',
  projectExperience: '',
  adjustmentPreference: 'CROSS',
  excludedLabIds: [],
  version: 0
})

const confirmState = reactive({ open: false, action: 'submit' })
const batch = computed(() => dashboard.value?.batch || {})
const application = computed(() => dashboard.value?.application || null)
const status = computed(() => application.value?.status || '')
const eligible = computed(() => dashboard.value?.eligible !== false)
const intentOpen = computed(() => Boolean(dashboard.value?.intentOpen))
const editable = computed(() => intentOpen.value && eligible.value && status.value !== 'LOCKED')
const batchMeta = computed(() => resolveMeta(batchStatus, batch.value.status))
const selectedLabs = computed(() => {
  const map = new Map()
  labs.value.forEach((lab) => (lab.mentors || []).forEach((mentor) => map.set(mentor.id, lab.id)))
  return form.mentorIds.map((id) => map.get(id)).filter(Boolean)
})

async function load() {
  loading.value = true
  try {
    const [dash, labData] = await Promise.all([studentApi.dashboard(), publicApi.labs()])
    dashboard.value = dash
    labs.value = labData
    if (dash.application) {
      form.mentorIds = (dash.application.intents || []).map((intent) => intent.mentorId)
      form.intro = dash.application.intro || ''
      form.skills = dash.application.skills || ''
      form.projectExperience = dash.application.projectExperience || ''
      form.adjustmentPreference = dash.application.adjustmentPreference || 'CROSS'
      form.excludedLabIds = dash.application.excludedLabIdList || []
      form.version = dash.application.version || 0
    }
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function toggleExcluded(labId, checked) {
  if (checked) {
    form.excludedLabIds = [...form.excludedLabIds, labId]
    form.mentorIds = form.mentorIds.filter((id) => {
      const lab = labs.value.find((item) => (item.mentors || []).some((mentor) => mentor.id === id))
      return lab?.id !== labId
    })
  } else {
    form.excludedLabIds = form.excludedLabIds.filter((id) => id !== labId)
  }
}

function ask(action) {
  if (!editable.value) return
  if (action === 'submit' && form.mentorIds.length === 0) {
    notify('请至少选择 1 个导师意向', 'warn')
    return
  }
  if (!form.adjustmentPreference) {
    notify('请选择调剂意愿', 'warn')
    return
  }
  confirmState.action = action
  confirmState.open = true
}

async function doSave() {
  saving.value = true
  try {
    const payload = {
      mentorIds: form.mentorIds,
      intro: form.intro,
      skills: form.skills,
      projectExperience: form.projectExperience,
      adjustmentPreference: form.adjustmentPreference,
      excludedLabIds: form.excludedLabIds,
      version: form.version
    }
    if (confirmState.action === 'submit') {
      await studentApi.submit(payload)
      notify('报名已提交，等待实训室面试安排', 'success')
    } else {
      await studentApi.saveDraft(payload)
      notify('草稿已保存', 'success')
    }
    confirmState.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function doWithdraw() {
  saving.value = true
  try {
    await studentApi.withdraw()
    notify('报名已撤回，可在开放期内重新提交', 'success')
    confirmState.open = false
    await load()
  } catch (err) {
    notify(err.message, 'error')
  } finally {
    saving.value = false
  }
}

async function onConfirm() {
  if (confirmState.action === 'withdraw') await doWithdraw()
  else await doSave()
}
</script>

<template>
  <PageHeader title="意向填报" desc="最多选择 3 位导师，平行意向不设先后。请在开放截止前完成提交。">
    <template #actions>
      <router-link to="/student/labs" class="btn btn-ghost"><Icon name="flask" :size="15" /> 查看实训室</router-link>
      <router-link to="/student" class="btn btn-ghost">我的选拔</router-link>
    </template>
  </PageHeader>

  <div v-if="loading" class="card card-pad"><div class="loading-block"><div class="spinner" />正在加载报名信息…</div></div>

  <template v-else>
    <div v-if="!eligible" class="notice notice--danger">
      <Icon name="alert" :size="16" />
      <span>你不符合本批次（{{ batch.gradeScope || '不限' }} 级）的报名资格，无法提交意向。如有疑问请联系学院教务办公室。</span>
    </div>
    <div v-else-if="!intentOpen" class="notice notice--warn">
      <Icon name="clock" :size="16" />
      <span>当前处于「{{ batchMeta.label }}」阶段，意向填报{{ batch.status === 'DRAFT' || batch.status === 'ANNOUNCED' ? '尚未开放' : '已截止' }}，页面为只读。开放时间为 {{ fmt(batch.intentStart) }} — {{ fmt(batch.intentEnd) }}。</span>
    </div>
    <div v-else-if="application && application.status === 'SUBMITTED'" class="notice notice--success">
      <Icon name="check" :size="16" />
      <span>报名已于 {{ fmt(application.submittedAt) }} 提交。在填报截止前你仍可以修改并重新提交；截止后系统将自动锁定。</span>
    </div>

    <div class="split">
      <div class="stack">
        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="form" :size="17" /> 报名信息</div>
              <div class="card-sub">让导师更全面地了解你，内容将仅对报名导师及学院管理员可见。</div>
            </div>
            <StatusPill v-if="application" :value="application.status" :map="applicationStatus" />
          </div>

          <div class="stack">
            <div class="field">
              <label class="label">个人简介 <span class="req">*</span></label>
              <textarea v-model="form.intro" class="textarea" maxlength="1000" :disabled="!editable"
                placeholder="简要介绍你的专业背景、兴趣方向、学习状态与投入时间。" />
              <div class="hint" style="text-align: right">{{ (form.intro || '').length }} / 1000</div>
            </div>
            <div class="field">
              <label class="label">技术基础</label>
              <textarea v-model="form.skills" class="textarea" maxlength="1000" :disabled="!editable"
                placeholder="课程、编程语言、工具、竞赛或科研训练等。" />
            </div>
            <div class="field">
              <label class="label">项目 / 实践经历</label>
              <textarea v-model="form.projectExperience" class="textarea" maxlength="1000" :disabled="!editable"
                placeholder="课程项目、竞赛、开源项目、实习等，说明你承担的工作与收获。" />
            </div>
          </div>
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="refresh" :size="17" /> 调剂意愿</div>
              <div class="card-sub">调剂只会在你授权的范围内进行，且不会分配到明确拒绝的实训室。</div>
            </div>
          </div>
          <div class="radio-cards">
            <label class="radio-card" :class="{ active: form.adjustmentPreference === 'INTERNAL' }">
              <input v-model="form.adjustmentPreference" type="radio" value="INTERNAL" :disabled="!editable" hidden />
              <strong>仅接受实训室内调剂</strong>
              <span>可在你填报的实训室内部调整导师</span>
            </label>
            <label class="radio-card" :class="{ active: form.adjustmentPreference === 'CROSS' }">
              <input v-model="form.adjustmentPreference" type="radio" value="CROSS" :disabled="!editable" hidden />
              <strong>接受跨实训室调剂</strong>
              <span>内部优先，仍有空缺时接受其他实训室</span>
            </label>
            <label class="radio-card" :class="{ active: form.adjustmentPreference === 'NONE' }">
              <input v-model="form.adjustmentPreference" type="radio" value="NONE" :disabled="!editable" hidden />
              <strong>不接受调剂</strong>
              <span>未被录取时不再参与统筹分配</span>
            </label>
          </div>

          <div class="field mt-16">
            <label class="label">明确拒绝的实训室（可选）</label>
            <div class="form-row">
              <label v-for="lab in labs" :key="lab.id" class="checkbox-line">
                <input
                  type="checkbox"
                  :checked="form.excludedLabIds.includes(lab.id)"
                  :disabled="!editable"
                  @change="toggleExcluded(lab.id, $event.target.checked)"
                />
                {{ lab.name }}
              </label>
            </div>
            <div class="hint">勾选后，系统不会将你调剂到该实训室，同时会移除已选择的该室导师。</div>
          </div>
        </section>

        <section v-if="application" class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title"><Icon name="clock" :size="17" /> 提交记录</div>
              <div class="card-sub">当前版本 v{{ application.version }}，最近更新 {{ fmt(application.updatedAt) }}</div>
            </div>
          </div>
          <div class="kv">
            <dt>报名状态</dt><dd>{{ resolveMeta(applicationStatus, application.status).label }}</dd>
            <dt>提交时间</dt><dd>{{ fmt(application.submittedAt) }}</dd>
            <dt>意向数量</dt><dd>{{ form.mentorIds.length }} 个</dd>
            <dt>调剂意愿</dt><dd>{{ { INTERNAL: '接受实训室内调剂', CROSS: '接受跨实训室调剂', NONE: '不接受调剂' }[form.adjustmentPreference] }}</dd>
          </div>
        </section>
      </div>

      <div class="stack">
        <MentorPicker
          v-model="form.mentorIds"
          :labs="labs"
          :disabled="!editable"
          :disabled-labs="form.excludedLabIds"
          :max="3"
        />

        <div v-if="editable" class="card card-pad">
          <div class="section-head"><div class="section-title">提交报名</div></div>
          <p class="small muted">提交前请确认意向与调剂意愿。截止后不可再修改。</p>
          <div class="stack mt-12">
            <button class="btn btn-primary btn-block" type="button" :disabled="saving" @click="ask('submit')">
              <Icon name="send" :size="15" /> {{ application?.status === 'SUBMITTED' ? '保存并重新提交' : '提交报名' }}
            </button>
            <button class="btn btn-ghost btn-block" type="button" :disabled="saving" @click="ask('draft')">保存草稿</button>
            <button
              v-if="application && application.status !== 'WITHDRAWN'"
              class="btn btn-danger btn-block"
              type="button"
              :disabled="saving"
              @click="confirmState.action = 'withdraw'; confirmState.open = true"
            >
              撤回报名
            </button>
          </div>
        </div>
        <div v-else class="card card-pad">
          <div class="notice notice--info"><Icon name="eye" :size="16" /> 当前页面为只读视图，无法修改报名。</div>
          <router-link to="/student" class="btn btn-ghost btn-block mt-12">返回我的选拔</router-link>
        </div>
      </div>
    </div>
  </template>

  <ModalDialog
    :open="confirmState.open"
    :title="confirmState.action === 'withdraw' ? '确认撤回报名？' : confirmState.action === 'submit' ? '确认提交报名？' : '保存草稿'"
    @close="confirmState.open = false"
  >
    <div v-if="confirmState.action === 'withdraw'" class="notice notice--danger">
      <Icon name="alert" :size="16" />
      <span>撤回后你的意向将全部释放，导师将不再看到你的报名。开放期内可以重新提交。</span>
    </div>
    <div v-else>
      <div class="kv">
        <dt>意向导师</dt><dd>{{ form.mentorIds.length }} 位</dd>
        <dt>调剂意愿</dt><dd>{{ { INTERNAL: '实训室内调剂', CROSS: '跨实训室调剂', NONE: '不接受调剂' }[form.adjustmentPreference] }}</dd>
        <dt>拒绝实训室</dt><dd>{{ form.excludedLabIds.length ? `${form.excludedLabIds.length} 个` : '无' }}</dd>
      </div>
      <div class="notice notice--info mt-16">
        <Icon name="shield" :size="16" />
        <span>提交后信息将进入实训室初选流程。在截止前仍可修改，系统会记录每次变更。</span>
      </div>
    </div>
    <template #footer>
      <button class="btn btn-ghost" type="button" @click="confirmState.open = false">取消</button>
      <button
        class="btn"
        :class="confirmState.action === 'withdraw' ? 'btn-danger' : 'btn-primary'"
        type="button"
        :disabled="saving"
        @click="onConfirm"
      >
        {{ saving ? '处理中…' : confirmState.action === 'withdraw' ? '确认撤回' : confirmState.action === 'submit' ? '确认提交' : '保存草稿' }}
      </button>
    </template>
  </ModalDialog>
</template>
