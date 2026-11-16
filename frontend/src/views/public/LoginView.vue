<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import Icon from '../../components/Icon.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ account: '', password: '' })
const error = ref('')
const submitting = ref(false)
const showPassword = ref(false)

const demoAccounts = [
  { label: '信息学院学生', account: 'S2023001', desc: '信息学院学生 · 林晓' },
  { label: '信息学院导师', account: 't1001', desc: '信息学院导师 · 张明' },
  { label: '学院管理员', account: 'admin', desc: '信息学院管理员 · 学院教务' },
  { label: '数学学院学生', account: 'S2024001', desc: '数学学院学生 · 赵思远' }
]

function fill(item) {
  form.account = item.account
  form.password = '123456'
  error.value = ''
  notify(`已填充：${item.desc}`, 'success')
}

async function submit() {
  error.value = ''
  if (!form.account || !form.password) {
    error.value = '请输入账号和密码'
    return
  }
  submitting.value = true
  try {
    const user = await auth.login(form.account.trim(), form.password)
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
    <div class="login-bg" aria-hidden="true"></div>
    <div class="login-glow login-glow--a" aria-hidden="true"></div>
    <div class="login-glow login-glow--b" aria-hidden="true"></div>
    <div class="login-beam" aria-hidden="true"></div>
    <div class="petals" aria-hidden="true">
      <i style="left: 8%; animation-duration: 13s; animation-delay: -2s; --px: 60px"></i>
      <i style="left: 22%; animation-duration: 17s; animation-delay: -9s; --px: -40px"></i>
      <i style="left: 38%; animation-duration: 14s; animation-delay: -5s; --px: 34px"></i>
      <i style="left: 55%; animation-duration: 19s; animation-delay: -12s; --px: -56px"></i>
      <i style="left: 70%; animation-duration: 15s; animation-delay: -3s; --px: 48px"></i>
      <i style="left: 84%; animation-duration: 12s; animation-delay: -7s; --px: -30px"></i>
      <i style="left: 93%; animation-duration: 16s; animation-delay: -11s; --px: 40px"></i>
    </div>
    <div class="login-brand">
      <strong class="calligraphy calligraphy-light">河北师范大学</strong>
      <i class="calligraphy-rule" aria-hidden="true"></i>
      <span>实训室选拔系统</span>
      <em class="brand-note brand-note--light">丙午 · 秋</em>
    </div>

    <div class="login-visual">
      <div class="login-visual-head">
        <div class="login-visual-title">
          <div class="login-kicker">实训室进组选拔</div>
          <h1>让每一次双向选择都有据可循</h1>
        </div>
        <p class="login-visual-desc">从集中宣讲、平行意向、统一面试，到拟录取确认、分轮补录、统筹调剂与结果公示，全流程线上留痕。</p>
      </div>
      <div class="login-demo-row">
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
    </div>

    <section class="login-card">
      <form class="login-panel" @submit.prevent="submit">
        <h2>登录系统</h2>
        <p class="sub">使用学号 / 工号登录，系统将自动识别你所属的学院。</p>

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
