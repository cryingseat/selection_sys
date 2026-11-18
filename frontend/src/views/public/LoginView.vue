<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import { batchStatus, meta as resolveMeta } from '../../utils/labels'
import Icon from '../../components/Icon.vue'
import FloraDecor from '../../components/FloraDecor.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ account: '', password: '' })
const error = ref('')
const info = ref('')
const submitting = ref(false)
const showPassword = ref(false)
const remember = ref(true)

/* ---------- 角色切换（学生 / 导师 / 教务） ---------- */
const ROLE_CONFIG = {
  student: {
    key: 'student',
    label: '学号（学生统一身份认证账号）',
    placeholder: '例如：S2023001',
    demoId: 'S2023001',
    art: '/images/mentor_welcome.jpg',
    badge: '学生端 · 最多可填报 3 个平行志愿',
    title: '春风化雨，双向奔赴理想的学术山海',
    desc: '登录学生工作台，在线浏览全院实训室与导师课题方向、实时剩余名额，一键投递个人简历、预约开放日面谈并追踪预录取状态。'
  },
  mentor: {
    key: 'mentor',
    label: '教工号（导师统一身份认证账号）',
    placeholder: '例如：t1001',
    demoId: 't1001',
    art: '/images/lab_discussion.jpg',
    badge: '导师端 · 申请审阅与组会面谈管理',
    title: '窗明几净，寻觅志同道合的青年科研薪火',
    desc: '登录导师端工作台，集中查阅投递学生的个人简历与科研设想，在线发起组会面谈邀请、开展多维评分并管理本季招生名额。'
  },
  admin: {
    key: 'admin',
    label: '教务工号（院系管理员认证账号）',
    placeholder: '例如：admin',
    demoId: 'admin',
    art: '/images/notice_board.jpg',
    badge: '教务管理端 · 全院指标调配与阳光公示',
    title: '严谨透明，守护每一份学术选择的公平秩序',
    desc: '登录学院教务管理后台，统筹配置八阶段双选时间锁、实时监控各实训室招生指标水位、处理跨组调剂并生成院系公示名单。'
  }
}
const currentRole = ref('student')
const roleCfg = computed(() => ROLE_CONFIG[currentRole.value] || ROLE_CONFIG.student)

/* ---------- 验证方式（密码 / 短信演示） ---------- */
const authMode = ref('pwd')
const smsCode = ref('')
const smsSeconds = ref(0)
let smsTimer = null

function sendSms() {
  if (smsTimer) return
  smsCode.value = '829406'
  info.value = '验证码已发送至校园绑定手机（演示环境已自动填入：829406）'
  smsSeconds.value = 60
  smsTimer = setInterval(() => {
    smsSeconds.value -= 1
    if (smsSeconds.value <= 0) {
      clearInterval(smsTimer)
      smsTimer = null
    }
  }, 1000)
}

/* ---------- 当前批次阶段（顶部胶囊） ---------- */
const home = ref(null)
onMounted(async () => {
  try {
    home.value = await publicApi.home()
  } catch {
    /* 静默失败，仅隐藏阶段胶囊 */
  }
})
const batch = computed(() => home.value?.batch || {})
const statusMeta = computed(() => resolveMeta(batchStatus, batch.value.status))
const stageChips = computed(() => {
  const stages = (home.value?.stages || []).slice(0, 3)
  if (stages.length) return stages
  return [
    { time: '第一批次', title: '平行志愿投递' },
    { time: '第二批次', title: '实验室双选面谈' },
    { time: '第三批次', title: '师生双向确认' }
  ]
})

/* ---------- 飘落花瓣 ---------- */
const petals = Array.from({ length: 16 }, () => ({
  left: Math.round(Math.random() * 100),
  dx: Math.round(Math.random() * 160 - 80),
  dur: +(10 + Math.random() * 9).toFixed(2),
  delay: +(-Math.random() * 15).toFixed(2),
  scale: +(0.65 + Math.random() * 0.7).toFixed(2)
}))

/* ---------- 交互 ---------- */
function setRole(role) {
  if (!ROLE_CONFIG[role]) return
  currentRole.value = role
  error.value = ''
  info.value = ''
}

function setMode(mode) {
  authMode.value = mode
  error.value = ''
  info.value = ''
}

function fillDemo(roleKey) {
  const key = roleKey || currentRole.value
  if (ROLE_CONFIG[key]) setRole(key)
  const cfg = ROLE_CONFIG[key] || roleCfg.value
  form.account = cfg.demoId
  form.password = '123456'
  smsCode.value = '829406'
  error.value = ''
  info.value = `已自动填入【${cfg.badge.split(' ')[0]}】演示账号（密码 123456），点击登录即可进入。`
  notify(`已填充：${cfg.demoId}`, 'success')
}

function forgotHint() {
  info.value = '提示：演示环境统一初始密码为 123456；如需重置请联系学院教务或校园信息化中心。'
  error.value = ''
}

async function submit() {
  error.value = ''
  info.value = ''
  if (authMode.value === 'sms') {
    error.value = '演示环境暂不支持验证码登录，请切换至「账号密码登录」。'
    return
  }
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
  <div class="fpl-login">
    <!-- 飘落花瓣与摇曳花枝 -->
    <div class="fpl-petals" aria-hidden="true">
      <i
        v-for="(p, i) in petals"
        :key="i"
        class="fpl-petal"
        :style="{ left: p.left + '%', '--dx': p.dx + 'px', '--s': p.scale, animationDuration: p.dur + 's', animationDelay: p.delay + 's' }"
      />
    </div>
    <FloraDecor kind="sprig-tl" class="fpl-sprig fpl-sprig-tl fpl-sway-l" />
    <FloraDecor kind="sprig-tr" class="fpl-sprig fpl-sprig-tr fpl-sway-r" />
    <FloraDecor kind="side-l" class="fpl-side fpl-side-l fpl-sway-l" />
    <FloraDecor kind="side-r" class="fpl-side fpl-side-r fpl-sway-r" />

    <!-- 顶部导航 -->
    <header class="fpl-topbar">
      <router-link to="/" class="fpl-brand" aria-label="返回公开导览首页">
        <FloraDecor kind="logo" :size="26" />
        <span>河北师范大学 · 统一身份认证中心</span>
      </router-link>
      <div class="fpl-topbar-right">
        <span v-if="batch.name" class="fpl-status"><span class="fpl-pill-dot" />当前阶段：{{ statusMeta.label }} · {{ batch.name }}</span>
        <router-link to="/" class="fpl-back-btn">← 返回公开导览首页</router-link>
      </div>
    </header>

    <!-- 主分栏玻璃卡 -->
    <main class="fpl-main">
      <div class="fpl-card">
        <!-- 左：水彩插画与故事 -->
        <div class="fpl-showcase">
          <div class="fpl-watermark" aria-hidden="true">1902</div>

          <div>
            <div class="fpl-art">
              <img :key="roleCfg.art" :src="roleCfg.art" :alt="roleCfg.badge" />
              <span class="fpl-art-badge"><span class="fpl-pill-dot" />{{ roleCfg.badge }}</span>
            </div>

            <div class="fpl-quote">
              <h2>{{ roleCfg.title }}</h2>
              <p>{{ roleCfg.desc }}</p>
            </div>
          </div>

          <div class="fpl-timeline">
            <div v-for="chip in stageChips" :key="chip.time + chip.title" class="fpl-step-chip">
              <span>{{ chip.time }}</span>
              <strong>{{ chip.title }}</strong>
            </div>
          </div>
        </div>

        <!-- 右：登录表单 -->
        <div class="fpl-form-col">
          <div class="fpl-form-head">
            <span class="fpl-pill"><span class="fpl-pill-dot" />校园统一身份认证 · SSO</span>
            <h3>欢迎登录实训室进组选拔系统</h3>
            <p>请选择您的身份角色，使用学号或教工号进入对应工作台</p>
          </div>

          <!-- 三角色切换 -->
          <div class="fpl-role-tabs" role="tablist" aria-label="登录角色选择">
            <button
              v-for="(cfg, key) in ROLE_CONFIG"
              :key="key"
              class="fpl-role-tab"
              :class="{ 'is-active': currentRole === key }"
              type="button"
              role="tab"
              :aria-selected="currentRole === key"
              @click="setRole(key)"
            >
              {{ key === 'student' ? '学生端' : key === 'mentor' ? '导师端' : '教务管理端' }}
            </button>
          </div>

          <!-- 验证方式切换 -->
          <div class="fpl-method-bar">
            <div class="fpl-method-tabs" role="tablist" aria-label="验证方式">
              <button class="fpl-method-btn" :class="{ 'is-active': authMode === 'pwd' }" type="button" @click="setMode('pwd')">账号密码登录</button>
              <button class="fpl-method-btn" :class="{ 'is-active': authMode === 'sms' }" type="button" @click="setMode('sms')">手机验证码登录</button>
            </div>
            <span class="fpl-ssl">SSL 加密传输</span>
          </div>

          <form @submit.prevent="submit">
            <div class="fpl-form-group">
              <label for="fpl-account">{{ roleCfg.label }}</label>
              <input id="fpl-account" v-model="form.account" class="fpl-input" type="text" :placeholder="roleCfg.placeholder" autocomplete="username" />
            </div>

            <!-- 密码 -->
            <div v-if="authMode === 'pwd'" class="fpl-form-group">
              <label for="fpl-pwd">认证密码</label>
              <div class="fpl-input-wrap">
                <input
                  id="fpl-pwd"
                  v-model="form.password"
                  class="fpl-input"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入统一身份认证密码"
                  autocomplete="current-password"
                />
                <button class="fpl-eye-btn" type="button" aria-label="显示或隐藏密码" @click="showPassword = !showPassword">
                  {{ showPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <!-- 短信验证码（演示） -->
            <div v-else class="fpl-form-group">
              <label for="fpl-sms">校园绑定手机动态码</label>
              <div class="fpl-sms-row">
                <input id="fpl-sms" v-model="smsCode" class="fpl-input" type="text" placeholder="请输入 6 位短信验证码" />
                <button class="fpl-sms-btn" type="button" :disabled="smsSeconds > 0" @click="sendSms">
                  {{ smsSeconds > 0 ? `${smsSeconds}s 后重发` : smsCode ? '重新获取' : '获取验证码' }}
                </button>
              </div>
            </div>

            <div class="fpl-aux-row">
              <label class="fpl-check-label">
                <input v-model="remember" type="checkbox" checked />
                <span>7 天内保持登录状态</span>
              </label>
              <button type="button" class="fpl-forgot" @click="forgotHint">忘记密码 / 账号激活说明</button>
            </div>

            <button class="fpl-submit" type="submit" :disabled="submitting">
              {{ submitting ? '登录中…' : '立即登录' }}
              <Icon v-if="!submitting" name="arrow" :size="16" />
            </button>

            <div v-if="error" class="fpl-msg fpl-msg-error"><Icon name="alert" :size="14" />{{ error }}</div>
            <div v-else-if="info" class="fpl-msg">{{ info }}</div>
          </form>

          <!-- 演示账号一键填入 -->
          <div class="fpl-demos">
            <span>演示环境快捷填入：</span>
            <div class="fpl-demo-chips">
              <button class="fpl-demo-chip" type="button" @click="fillDemo('')">一键填入当前角色</button>
              <button class="fpl-demo-chip" type="button" @click="fillDemo('student')">新生示例</button>
              <button class="fpl-demo-chip" type="button" @click="fillDemo('mentor')">导师示例</button>
              <button class="fpl-demo-chip" type="button" @click="fillDemo('admin')">教务示例</button>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 底部水彩草地带 -->
    <FloraDecor kind="meadow" class="fpl-meadow" />
  </div>
</template>

<style>
/* ============================================================
   登录页 · 日系水彩风（参考花栖双选统一身份认证中心）
   命名前缀 fpl-，独立于全局与内页样式
   ============================================================ */
.fpl-login {
  --fpl-ink: #161914;
  --fpl-olive: #4a6714;
  --fpl-green: #6d9612;
  --fpl-lime: #b9d458;
  --fpl-blossom: #f5b8cb;
  --fpl-blossom-deep: #d96b8a;
  --fpl-pill: #e9f1d2;
  --fpl-line: #ecefe6;
  --fpl-cream: #fafbf7;
  position: relative;
  min-height: 100vh;
  background:
    radial-gradient(ellipse 62% 55% at 15% 18%, rgba(252, 228, 236, .72), transparent 70%),
    radial-gradient(ellipse 58% 52% at 88% 22%, rgba(233, 241, 210, .82), transparent 70%),
    radial-gradient(ellipse 65% 50% at 50% 95%, rgba(226, 238, 195, .68), transparent 75%),
    #fafbf7;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow-x: hidden;
  color: var(--fpl-ink);
  font-family: "Inter Tight", "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
}
@keyframes fplFall {
  to { transform: translate(var(--dx, 30px), 108vh) rotate(540deg) scale(var(--s, 1)); }
}
@keyframes fplSwaySlow {
  0%, 100% { transform: rotate(-1.8deg) translateY(0); }
  50% { transform: rotate(2.2deg) translateY(-3px); }
}
@keyframes fplSwayReverse {
  0%, 100% { transform: scaleX(-1) rotate(-1.8deg) translateY(0); }
  50% { transform: scaleX(-1) rotate(2.2deg) translateY(-3px); }
}
@keyframes fplRise {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}
.fpl-sway-l { transform-origin: 0% 50%; animation: fplSwaySlow 9s ease-in-out infinite; }
.fpl-sway-r { transform-origin: 0% 50%; animation: fplSwayReverse 10.5s ease-in-out infinite; }

/* ---------- 装饰层 ---------- */
.fpl-petals { position: fixed; inset: 0; pointer-events: none; overflow: hidden; z-index: 2; }
.fpl-petal {
  position: absolute; top: -24px; width: 11px; height: 8px; border-radius: 80% 0 80% 0;
  background: linear-gradient(135deg, #ffd6e4, #f4a4be); opacity: .82;
  animation: fplFall linear infinite;
}
.fpl-sprig { position: fixed; pointer-events: none; z-index: 2; opacity: .88; }
.fpl-sprig-tl { top: 18px; left: -18px; width: clamp(180px, 22vw, 310px); }
.fpl-sprig-tr { top: 16px; right: -18px; width: clamp(180px, 22vw, 310px); transform: scaleX(-1); }
.fpl-side { position: fixed; pointer-events: none; z-index: 2; }
.fpl-side-l { left: -12px; top: 36%; width: clamp(90px, 10vw, 150px); }
.fpl-side-r { right: -12px; top: 44%; width: clamp(90px, 10vw, 150px); }
.fpl-meadow {
  position: fixed; left: 0; right: 0; bottom: -2px; height: clamp(54px, 6.8vw, 90px);
  z-index: 4; pointer-events: none; overflow: hidden;
}
.fpl-meadow svg { width: 100%; height: 100%; display: block; }

/* ---------- 顶栏 ---------- */
.fpl-topbar {
  position: relative; z-index: 10; width: 100%; max-width: 1200px; margin: 0 auto;
  padding: 24px 4vw 12px; display: flex; align-items: center; justify-content: space-between; gap: 16px;
}
.fpl-brand { display: inline-flex; align-items: center; gap: 10px; font-size: 17px; font-weight: 700; letter-spacing: -.02em; white-space: nowrap; color: var(--fpl-ink); }
.fpl-topbar-right { display: flex; align-items: center; gap: 12px; }
.fpl-status {
  display: inline-flex; align-items: center; gap: 8px; font-size: 12.5px; font-weight: 500;
  padding: 6px 14px; border-radius: 999px; background: rgba(255, 255, 255, .82);
  border: 1px solid var(--fpl-line); color: #3d4632;
}
.fpl-pill-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--fpl-blossom-deep); }
.fpl-back-btn {
  display: inline-flex; align-items: center; gap: 8px; height: 38px; padding: 0 16px; border-radius: 999px;
  background: rgba(255, 255, 255, .86); border: 1px solid rgba(74, 103, 20, .18);
  font-size: 13.5px; font-weight: 600; color: var(--fpl-ink);
  box-shadow: 0 4px 14px rgba(35, 48, 12, .06);
  backdrop-filter: blur(10px); -webkit-backdrop-filter: blur(10px);
  transition: all .25s ease;
}
.fpl-back-btn:hover { background: #fff; border-color: var(--fpl-olive); color: var(--fpl-olive); transform: translateX(-2px); }

/* ---------- 主分栏玻璃卡 ---------- */
.fpl-main { position: relative; z-index: 8; width: 100%; max-width: 1140px; margin: auto; padding: 16px 4vw 110px; }
.fpl-card {
  position: relative; border-radius: 28px;
  background: rgba(255, 255, 255, .88);
  border: 1px solid rgba(255, 255, 255, .95);
  box-shadow: 0 28px 70px rgba(35, 50, 14, .11), 0 0 0 1px rgba(109, 150, 18, .12);
  backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px);
  display: grid; grid-template-columns: 1.08fr 1fr; overflow: hidden;
  animation: fplRise .8s .1s cubic-bezier(.2, .8, .2, 1) both;
}

/* 左侧插画故事栏 */
.fpl-showcase {
  position: relative; padding: 34px 32px;
  background: linear-gradient(155deg, rgba(252, 228, 236, .52) 0%, rgba(247, 250, 239, .86) 52%, rgba(233, 241, 210, .68) 100%);
  border-right: 1px solid #e6ecdc;
  display: flex; flex-direction: column; justify-content: space-between; gap: 22px; overflow: hidden;
}
.fpl-watermark {
  position: absolute; right: -12px; bottom: -26px;
  font-family: "Noto Serif SC", serif; font-size: 138px; font-weight: 700; line-height: 1; letter-spacing: -.04em;
  color: rgba(74, 103, 20, .055); pointer-events: none; user-select: none;
}
.fpl-art {
  position: relative; border-radius: 20px; overflow: hidden; aspect-ratio: 16 / 10;
  border: 2.5px solid #fff; box-shadow: 0 16px 36px rgba(35, 50, 14, .12); background: #f4f1ea;
}
.fpl-art img {
  width: 100%; height: 100%; object-fit: cover; display: block;
  transition: transform .65s cubic-bezier(.2, .8, .2, 1);
}
.fpl-art:hover img { transform: scale(1.03); }
.fpl-art-badge {
  position: absolute; left: 12px; bottom: 12px; padding: 5px 12px; border-radius: 999px;
  background: rgba(255, 255, 255, .92); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
  font-size: 11.5px; font-weight: 600; color: var(--fpl-olive);
  display: inline-flex; align-items: center; gap: 6px; box-shadow: 0 4px 12px rgba(0, 0, 0, .08);
}
.fpl-quote h2 {
  font-family: "Noto Serif SC", serif; font-size: clamp(22px, 2.2vw, 29px); font-weight: 600;
  line-height: 1.3; color: var(--fpl-ink); margin: 14px 0 8px;
}
.fpl-quote p { font-size: 13.5px; line-height: 1.65; color: #444d3b; }
.fpl-timeline {
  position: relative; z-index: 2; display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px;
  padding-top: 16px; border-top: 1px dashed #d7e0c8;
}
.fpl-step-chip { background: rgba(255, 255, 255, .75); border: 1px solid #e4ead8; border-radius: 14px; padding: 10px 12px; }
.fpl-step-chip span { display: block; font-size: 11px; font-weight: 600; color: var(--fpl-blossom-deep); margin-bottom: 2px; }
.fpl-step-chip strong { display: block; font-size: 12.5px; font-weight: 600; color: var(--fpl-ink); }

/* 右侧表单栏 */
.fpl-form-col {
  position: relative; padding: 36px 38px; display: flex; flex-direction: column; justify-content: center;
  background: rgba(255, 255, 255, .92);
}
.fpl-pill {
  display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 500;
  padding: 5px 12px; border-radius: 999px; background: var(--fpl-pill); color: #2c3a10; letter-spacing: .02em;
}
.fpl-form-head { margin-bottom: 20px; }
.fpl-form-head h3 { font-family: "Noto Serif SC", serif; font-size: 24px; font-weight: 600; color: var(--fpl-ink); margin-top: 10px; }
.fpl-form-head p { font-size: 13px; color: #5a634f; margin-top: 4px; }

.fpl-role-tabs {
  display: grid; grid-template-columns: repeat(3, 1fr);
  background: var(--fpl-cream); border: 1px solid var(--fpl-line); border-radius: 999px;
  padding: 4px; margin-bottom: 18px;
}
.fpl-role-tab {
  padding: 8px 10px; border-radius: 999px; font-size: 13px; font-weight: 600; color: #545d4a;
  transition: all .22s ease; text-align: center; border: none; background: none; cursor: pointer;
}
.fpl-role-tab.is-active { background: var(--fpl-olive); color: #f6fae8; box-shadow: 0 3px 10px rgba(74, 103, 20, .22); }

.fpl-method-bar {
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--fpl-line); margin-bottom: 18px; padding-bottom: 8px;
}
.fpl-method-tabs { display: flex; gap: 18px; }
.fpl-method-btn { position: relative; font-size: 13px; font-weight: 600; color: #636c58; padding-bottom: 6px; transition: color .2s; border: none; background: none; cursor: pointer; }
.fpl-method-btn.is-active { color: var(--fpl-olive); }
.fpl-method-btn.is-active::after {
  content: ""; position: absolute; left: 0; right: 0; bottom: -9px; height: 2.5px;
  border-radius: 999px; background: var(--fpl-olive);
}
.fpl-ssl { font-size: 11.5px; color: var(--fpl-green); font-weight: 600; }

.fpl-form-group { margin-bottom: 14px; }
.fpl-form-group label { display: block; font-size: 12.5px; font-weight: 600; color: #38402e; margin-bottom: 6px; }
.fpl-input {
  width: 100%; height: 42px; padding: 0 14px; border-radius: 12px;
  border: 1px solid #dce3d1; background: #fff; font-size: 14px; color: var(--fpl-ink); outline: none;
  transition: border-color .2s, box-shadow .2s;
}
.fpl-input::placeholder { color: #a7ad9c; }
.fpl-input:focus { border-color: var(--fpl-olive); box-shadow: 0 0 0 3px rgba(109, 150, 18, .12); }
.fpl-input-wrap { position: relative; display: flex; align-items: center; }
.fpl-input-wrap .fpl-input { padding-right: 48px; }
.fpl-eye-btn { position: absolute; right: 10px; font-size: 12px; color: #6b7460; padding: 4px 8px; border-radius: 6px; border: none; background: none; cursor: pointer; }
.fpl-eye-btn:hover { color: var(--fpl-olive); background: var(--fpl-cream); }
.fpl-sms-row { display: grid; grid-template-columns: 1fr auto; gap: 10px; }
.fpl-sms-btn {
  height: 42px; padding: 0 14px; border-radius: 12px; background: var(--fpl-pill); color: var(--fpl-olive);
  font-size: 12.5px; font-weight: 600; white-space: nowrap; border: 1px solid #d7e4b5; transition: all .2s;
}
.fpl-sms-btn:hover:not(:disabled) { background: var(--fpl-lime); color: #1a2307; }
.fpl-sms-btn:disabled { opacity: .6; cursor: not-allowed; }

.fpl-aux-row { display: flex; align-items: center; justify-content: space-between; margin: 4px 0 16px; font-size: 12.5px; color: #58614d; }
.fpl-check-label { display: inline-flex; align-items: center; gap: 6px; cursor: pointer; user-select: none; }
.fpl-check-label input {
  appearance: none; -webkit-appearance: none;
  width: 15px; height: 15px; border-radius: 4px;
  border: 1px solid #c9d4b8; background: #fff; cursor: pointer;
  display: grid; place-content: center; margin: 0;
}
.fpl-check-label input:checked { background: var(--fpl-olive); border-color: var(--fpl-olive); }
.fpl-check-label input:checked::before {
  content: ""; width: 8px; height: 4.5px;
  border-left: 2px solid #fff; border-bottom: 2px solid #fff;
  transform: rotate(-45deg) translate(1px, -1px);
}
.fpl-forgot { color: var(--fpl-olive); font-weight: 600; border: none; background: none; cursor: pointer; }

.fpl-submit {
  width: 100%; height: 44px; border-radius: 999px; cursor: pointer; border: none;
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  background: var(--fpl-olive); color: #f6fae8; font-size: 14.5px; font-weight: 600;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .25), 0 4px 12px rgba(60, 80, 10, .22);
  transition: transform .28s cubic-bezier(.2, .8, .2, 1), background .28s;
}
.fpl-submit:hover:not(:disabled) { transform: translateY(-1px); background: #5b7d18; }
.fpl-submit:disabled { opacity: .65; cursor: default; }

.fpl-msg {
  margin-top: 12px; font-size: 13px; line-height: 1.5; padding: 10px 14px; border-radius: 12px;
  background: var(--fpl-pill); color: #27360c;
  display: flex; align-items: flex-start; gap: 6px;
}
.fpl-msg-error { background: #fdecec; color: #a02830; }

.fpl-demos {
  margin-top: 16px; padding-top: 14px; border-top: 1px dashed #e2e8d8;
  display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px;
}
.fpl-demos > span { font-size: 12px; color: #626b57; }
.fpl-demo-chips { display: flex; gap: 6px; flex-wrap: wrap; }
.fpl-demo-chip {
  font-size: 11.5px; font-weight: 600; padding: 4px 10px; border-radius: 999px;
  background: var(--fpl-cream); border: 1px solid #dce4ce; color: var(--fpl-olive); transition: all .2s;
}
.fpl-demo-chip:hover { background: var(--fpl-pill); border-color: var(--fpl-olive); }

/* ---------- 响应式 ---------- */
@media (max-width: 900px) {
  .fpl-card { grid-template-columns: 1fr; }
  .fpl-showcase { border-right: none; border-bottom: 1px solid #e6ecdc; padding: 26px 24px; }
  .fpl-form-col { padding: 28px 24px; }
}
@media (max-width: 600px) {
  .fpl-timeline { grid-template-columns: 1fr; }
  .fpl-topbar { flex-wrap: wrap; }
  .fpl-status { display: none; }
  .fpl-brand { font-size: 14.5px; }
}
@media (prefers-reduced-motion: reduce) {
  .fpl-sway-l, .fpl-sway-r, .fpl-petal { animation: none !important; }
}
</style>
