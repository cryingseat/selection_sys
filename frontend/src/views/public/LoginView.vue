<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import Icon from '../../components/Icon.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ account: '', password: '', collegeId: null })
const colleges = ref([])
const error = ref('')
const submitting = ref(false)
const showPassword = ref(false)

const selectedCollege = computed(() => colleges.value.find((item) => item.id === form.collegeId))

const demoAccounts = [
  { label: '信息学院学生', account: 'S2023001', desc: '信息学院学生 · 林晓', collegeId: 1 },
  { label: '信息学院导师', account: 't1001', desc: '信息学院导师 · 张明', collegeId: 1 },
  { label: '学院管理员', account: 'admin', desc: '信息学院管理员 · 学院教务', collegeId: 1 },
  { label: '数学学院学生', account: 'S2024001', desc: '数学学院学生 · 赵思远', collegeId: 2 }
]

async function loadColleges() {
  try {
    colleges.value = await publicApi.colleges()
    if (!form.collegeId && colleges.value.length) {
      form.collegeId = colleges.value[0].id
    }
  } catch (err) {
    error.value = err.message
  }
}

onMounted(loadColleges)

function fill(item) {
  form.account = item.account
  form.password = '123456'
  form.collegeId = item.collegeId
  error.value = ''
  notify(`已填充：${item.desc}`, 'success')
}

async function submit() {
  error.value = ''
  if (!form.collegeId) {
    error.value = '请先选择学院'
    return
  }
  if (!form.account || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  try {
    const user = await auth.login(form.account.trim(), form.password, form.collegeId)
    notify(`欢迎回来，${user.displayName}`, 'success')
    const redirect = route.query.redirect
    if (redirect && typeof redirect === 'string') {
      router.replace(redirect)
    } else {
      router.replace(auth.homePath)
    }
  } catch (err) {
    error.value = err.message
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-brand">
      <strong>河北师范大学</strong>
      <span>实训室选拔系统</span>
    </div>

    <section class="login-card">
      <div class="login-visual">
        <div class="login-kicker">实训室进组选拔</div>
        <h1>让每一次<br />双向选择都有据可循</h1>
        <p>从集中宣讲、平行意向、统一面试，到拟录取确认、分轮补录、统筹调剂与结果公示，全流程线上留痕。</p>
        <div class="overlay-points">
          <div class="overlay-point"><span class="tick">1</span><span>最多 3 个平行导师意向，不设志愿先后。</span></div>
          <div class="overlay-point"><span class="tick">2</span><span>导师独立评价、名额硬约束，只确认一个结果。</span></div>
          <div class="overlay-point"><span class="tick">3</span><span>不同学院独立查看本院实训室，游客可浏览全校。</span></div>
        </div>
        <div class="login-demo-label">演示账号 · 点击填充（密码 123456）</div>
        <div class="demo-chips">
          <button
            v-for="item in demoAccounts"
            :key="item.account"
            class="demo-chip"
            type="button"
            :title="item.desc"
            @click="fill(item)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>

      <form class="login-panel" @submit.prevent="submit">
        <h2>登录系统</h2>
        <p class="sub">选择学院后，使用学号 / 工号登录；登录后仅展示你所在学院的实训室。</p>

        <div class="field login-college">
          <label class="label">选择学院 <span class="req">*</span></label>
          <select v-model.number="form.collegeId" class="select">
            <option v-for="college in colleges" :key="college.id" :value="college.id">{{ college.name }}</option>
          </select>
          <div v-if="selectedCollege" class="college-select-hint">
            {{ selectedCollege.intro }}
          </div>
        </div>

        <div class="field">
          <label class="label">账号</label>
          <input v-model="form.account" class="input" placeholder="学号 / 工号" autocomplete="username" />
        </div>

        <div class="field">
          <label class="label">密码</label>
          <div style="position: relative">
            <input
              v-model="form.password"
              class="input"
              :type="showPassword ? 'text' : 'password'"
              placeholder="请输入密码"
              autocomplete="current-password"
              style="padding-right: 44px"
            />
            <button
              type="button"
              class="link-btn"
              style="position: absolute; right: 12px; top: 10px; font-size: 12px"
              @click="showPassword = !showPassword"
            >
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div v-if="error" class="notice notice--danger"><Icon name="alert" :size="15" />{{ error }}</div>

        <button class="btn btn-primary btn-lg btn-block login-submit" type="submit" :disabled="submitting">
          {{ submitting ? '登录中…' : '登录' }}
          <Icon v-if="!submitting" name="arrow" :size="16" />
        </button>

        <div class="login-foot">
          未登录也可以 <router-link to="/">浏览全校实训室与选拔流程</router-link>
        </div>
      </form>
    </section>
  </div>
</template>
