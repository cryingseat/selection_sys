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
  { account: 'S2023001', role: '信息学院学生', desc: '林晓 · 已有报名', collegeId: 1 },
  { account: 't1001', role: '信息学院导师', desc: '张明 · 智能感知', collegeId: 1 },
  { account: 'admin', role: '信息学院管理员', desc: '学院教务', collegeId: 1 },
  { account: 'S2024001', role: '数学学院学生', desc: '赵思远 · 只看到数学实验室', collegeId: 2 }
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
    <section class="login-visual">
      <router-link to="/" class="brand">
        <span class="brand-mark">选</span>
        <span class="brand-text">
          <strong>实训室进组选拔系统</strong>
          <span>OPEN STUDIO</span>
        </span>
      </router-link>

      <div>
        <div class="hero-badge"><Icon name="spark" :size="14" /> 全校实训室与导师空间画廊</div>
        <h1>让每一次<br />双向选择都有据可循</h1>
        <p>从集中宣讲、平行意向、统一面试，到拟录取确认、分轮补录、统筹调剂与结果公示，全流程线上留痕。</p>
        <div class="login-points">
          <div class="login-point"><span class="tick">1</span><span>最多 3 个平行导师意向，不设志愿先后，学生自主表达真实兴趣。</span></div>
          <div class="login-point"><span class="tick">2</span><span>导师独立评价、名额硬约束，学生只确认一个拟录取结果。</span></div>
          <div class="login-point"><span class="tick">3</span><span>不同学院独立查看本学院实训室；游客可浏览全校空间。</span></div>
        </div>
      </div>

      <div class="tiny" style="color: rgba(255,255,255,0.5)">
        Vue 3 · Spring Boot 3 · MyBatis · MySQL 8
      </div>
    </section>

    <section class="login-panel">
      <div class="login-card">
        <h2>登录系统</h2>
        <p class="sub">先选择学院，再使用学号 / 工号登录。登录后只展示你所在学院的实训室。</p>

        <form @submit.prevent="submit" class="stack">
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
            <input v-model="form.account" class="input" placeholder="学号 / 工号 / admin" autocomplete="username" />
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
                style="position: absolute; right: 12px; top: 9px; font-size: 12px"
                @click="showPassword = !showPassword"
              >
                {{ showPassword ? '隐藏' : '显示' }}
              </button>
            </div>
          </div>

          <div v-if="error" class="notice notice--danger"><Icon name="alert" :size="15" />{{ error }}</div>

          <button class="btn btn-primary btn-lg btn-block" type="submit" :disabled="submitting">
            {{ submitting ? '登录中…' : '登录' }}
            <Icon v-if="!submitting" name="arrow" :size="16" />
          </button>
        </form>

        <div class="mt-24">
          <div class="tiny muted">演示账号（密码均为 123456，点击自动填充对应学院）</div>
          <div class="demo-accounts mt-8">
            <button v-for="item in demoAccounts" :key="item.account" class="demo-account" type="button" @click="fill(item)">
              <strong>{{ item.role }}</strong>
              <span>{{ item.desc }}</span>
            </button>
          </div>
        </div>

        <div class="login-foot">
          未登录也可以 <router-link to="/">浏览全校实训室与选拔流程</router-link>
        </div>
      </div>
    </section>
  </div>
</template>
