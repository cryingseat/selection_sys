<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, publicityStatus, announcementType, meta as resolveMeta } from '../../utils/labels'
import { fmt, monthDay } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import ModalDialog from '../../components/ModalDialog.vue'
import EmptyState from '../../components/EmptyState.vue'
import FloraDecor from '../../components/FloraDecor.vue'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const error = ref('')
const home = ref(null)
const activeCollege = ref(0)
const keyword = ref('')
const onlyAvailable = ref(false)
const activeLab = ref(null)
const activeMentor = ref(null)
const activeAnnouncement = ref(null)
const activeStage = ref(1)
const openFaq = ref(0)

const batch = computed(() => home.value?.batch || {})
const labs = computed(() => home.value?.labs || [])
const colleges = computed(() => home.value?.colleges || [])
const announcements = computed(() => home.value?.announcements || [])
const assignments = computed(() => home.value?.publicAssignments || [])
const stats = computed(() => home.value?.stats || {})
const stages = computed(() => home.value?.stages || [])
const scopedCollegeId = computed(() => home.value?.scopedCollegeId || null)
const statusMeta = computed(() => resolveMeta(batchStatus, batch.value.status))
const visibleColleges = computed(() => scopedCollegeId.value
  ? colleges.value.filter((college) => college.id === scopedCollegeId.value)
  : colleges.value)
const filteredLabs = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return labs.value.filter((lab) => {
    if (activeCollege.value && lab.collegeId !== activeCollege.value) return false
    if (onlyAvailable.value && !(lab.remainingQuota > 0)) return false
    if (!key) return true
    const text = `${lab.name}${lab.code}${lab.direction}${lab.intro}${(lab.mentors || []).map((m) => `${m.name}${m.researchDirection}`).join('')}`.toLowerCase()
    return text.includes(key)
  })
})

/* ---------- 常见问题（沿用系统规则口径） ---------- */
const faqs = [
  { q: '可以同时填报几位导师？', a: '每名学生在同一批次最多填报 <strong>3 位导师</strong>，平行意向不设先后顺序，也不能重复选择同一位导师。' },
  { q: '提交后还能修改吗？', a: '在意向填报截止前可以保存草稿、修改并重新提交，也可以撤回报名；截止后系统自动锁定，不能再修改。' },
  { q: '收到多个拟录取怎么办？', a: '你可以在规定时间内自主选择其中一个并确认。确认后其他拟录取意向自动释放，名额回到补录池。' },
  { q: '逾期没有确认会怎样？', a: '超过确认截止时间未操作，视为放弃全部拟录取；空缺名额将按候补顺位进入补录。' },
  { q: '调剂会把我分到不喜欢的实训室吗？', a: '调剂只会在你授权的范围内进行。你可以在填报时明确拒绝某些实训室，系统不会将你分配到这些实训室。' },
  { q: '公示后结果还能改吗？', a: '公示发布后结果锁定。如确需更正，学院会撤回公示、修改后重新公示，所有操作都会留痕。' }
]

/* ---------- 装饰数据：玻璃方块 / 网格方块 / 飘落花瓣 ---------- */
const glassCoords = [
  [4, 62], [4, 70], [7.5, 70], [11, 70], [4, 78],
  [46, 75], [49.5, 79], [49.5, 75], [53, 75],
  [86, 58], [86, 67], [82.5, 76], [89.5, 76], [86, 84]
]
const squares = Array.from({ length: 18 }, () => ({
  left: Math.floor(Math.random() * 28) * 48 - 10,
  top: Math.floor(Math.random() * 5) * 48 + 6
}))
function petals(count) {
  return Array.from({ length: count }, () => ({
    left: Math.round(Math.random() * 100),
    dx: Math.round(Math.random() * 160 - 80),
    dur: +(10 + Math.random() * 9).toFixed(2),
    delay: +(-Math.random() * 15).toFixed(2),
    scale: +(0.65 + Math.random() * 0.7).toFixed(2)
  }))
}
const heroPetals = petals(22)
const faqPetals = petals(16)

/* ---------- 工具函数 ---------- */
async function load() {
  loading.value = true
  error.value = ''
  try {
    home.value = await publicApi.home()
    if (home.value.scopedCollegeId) activeCollege.value = home.value.scopedCollegeId
    const current = home.value.stages?.find((s) => s.state === 'active')
    activeStage.value = current?.no || 1
  } catch (err) {
    error.value = err.message
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function collegeCount(collegeId) {
  return labs.value.filter((lab) => lab.collegeId === collegeId).length
}

function collegeAccent(collegeId) {
  const college = colleges.value.find((item) => item.id === collegeId)
  return college?.accentColor || '#6d9612'
}

function setCollege(id) {
  activeCollege.value = id
}

function resetFilters() {
  activeCollege.value = scopedCollegeId.value || 0
  keyword.value = ''
  onlyAvailable.value = false
}

function shortDate(value) {
  if (!value) return '待定'
  const text = String(value)
  return `${Number(text.slice(5, 7))}月${Number(text.slice(8, 10))}日`
}

function shortOpenDay() {
  return `${shortDate(batch.value.openDayStart)}–${shortDate(batch.value.openDayEnd)}`
}

function ctaRoute() {
  return auth.isLoggedIn ? auth.homePath : '/login'
}

function goLogin() {
  router.push(ctaRoute())
}

function mentorAction(mentor, lab) {
  if (!auth.isLoggedIn) {
    router.push({ name: 'login' })
    return
  }
  if (auth.role === 'STUDENT') {
    router.push({ name: 'student-application' })
    return
  }
  activeMentor.value = { mentor, lab }
}

function openMentor(mentor, lab) {
  activeMentor.value = { mentor, lab }
}

function directionTags(lab) {
  return (lab.direction || '').split(/[、,，]/).filter(Boolean).slice(0, 4)
}

function mentorNames(lab) {
  return (lab.mentors || []).map((m) => m.name).join('、')
}

function pad2(n) {
  return String(n).padStart(2, '0')
}

const stageImages = ['hero_campus', 'courtyard_interview', 'lab_discussion', 'mentor_welcome']
function stageImg(no) {
  return `/images/${stageImages[(no - 1) % stageImages.length]}.jpg`
}
function miniTint(no) {
  return (no - 1) % 4 < 2 ? '#fce8ef' : '#eef5df'
}
const avatarTints = [
  'linear-gradient(135deg, #fce4ec, #e9f1d2)',
  'linear-gradient(135deg, #e7efcf, #d8ecf8)',
  'linear-gradient(135deg, #ffe6ee, #fff3cd)',
  'linear-gradient(135deg, #e8f5e9, #fce4ec)'
]
function avatarTint(index) {
  return avatarTints[index % avatarTints.length]
}

function scrollTo(id) {
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth' })
}
</script>

<template>
  <div class="fp-home">
    <!-- ============ 浮动胶囊导航 ============ -->
    <header class="fp-nav">
      <a class="fp-brand" href="#top" @click.prevent="scrollTo('top')">
        <FloraDecor kind="logo" :size="26" />
        <span class="fp-brand-text">
          <strong>河北师范大学</strong>
          <em>实训室进组选拔</em>
        </span>
      </a>
      <nav class="fp-nav-links" aria-label="主导航">
        <a href="#mentors" @click.prevent="scrollTo('mentors')">实训室与导师</a>
        <a href="#process" @click.prevent="scrollTo('process')">选拔流程</a>
        <a href="#notices" @click.prevent="scrollTo('notices')">通知公告</a>
        <a href="#faq" @click.prevent="scrollTo('faq')">常见问题</a>
        <a v-if="assignments.length" href="#results" @click.prevent="scrollTo('results')">公示结果</a>
      </nav>
      <span class="fp-nav-stage"><i class="fp-dot" />{{ statusMeta.label }}</span>
      <button class="fp-btn" type="button" @click="goLogin">
        {{ auth.isLoggedIn ? '进入工作台' : '登录 / 进入系统' }}
      </button>
    </header>

    <div v-if="error" class="fp-error">
      <Icon name="alert" :size="16" />{{ error }}
      <button class="fp-error-retry" type="button" @click="load">重试</button>
    </div>

    <div v-if="loading" class="fp-loading">
      <span class="fp-pill"><i class="fp-pill-dot" />正在布置页面…</span>
      <div class="fp-loading-grid">
        <div v-for="i in 3" :key="i" class="fp-skeleton" />
      </div>
    </div>

    <template v-else-if="home">
      <!-- ============ 首屏 HERO ============ -->
      <section id="top" class="fp-hero">
        <FloraDecor kind="cloud-l" class="fp-cloud fp-cloud-l fp-drift-a" />
        <FloraDecor kind="cloud-r" class="fp-cloud fp-cloud-r fp-drift-b" />

        <div class="fp-hero-copy">
          <span class="fp-pill"><i class="fp-pill-dot" />{{ batch.name || '实训室进组选拔' }} · {{ batch.gradeScope || '全校' }}年级</span>
          <h1>走近真实空间，<br />遇见契合的<em>同行导师。</em></h1>
          <p>先浏览各学院实训室的真实空间、器物环境与项目方向，再挑选至多 3 位平行意向导师（不分先后）。每一次选择，都有据可循。</p>
          <div class="fp-hero-actions">
            <a class="fp-btn" href="#mentors" @click.prevent="scrollTo('mentors')">浏览实训室导师</a>
            <a class="fp-btn fp-btn-ghost" href="#process" @click.prevent="scrollTo('process')">查看选拔流程</a>
          </div>
        </div>

        <div class="fp-hero-glass" aria-hidden="true">
          <i
            v-for="(g, i) in glassCoords"
            :key="i"
            class="fp-glass"
            :class="{ 'is-center': g[0] > 40 && g[0] < 60 }"
            :style="{ left: g[0] + '%', top: g[1] + '%' }"
          />
        </div>
        <div class="fp-petals" aria-hidden="true">
          <i
            v-for="(p, i) in heroPetals"
            :key="i"
            class="fp-petal"
            :style="{ left: p.left + '%', '--dx': p.dx + 'px', '--s': p.scale, animationDuration: p.dur + 's', animationDelay: p.delay + 's' }"
          />
        </div>

        <div class="fp-hero-media" aria-hidden="true">
          <img src="/images/hero_campus.jpg" alt="樱花校园水彩风景" />
        </div>
        <FloraDecor kind="meadow" class="fp-meadow" />
      </section>

      <!-- ============ 实训室与导师 ============ -->
      <section id="mentors" class="fp-mentors">
        <div class="fp-sq-field" aria-hidden="true">
          <i v-for="(s, i) in squares" :key="i" class="fp-grid-sq" :style="{ left: s.left + 'px', top: s.top + 'px' }" />
        </div>
        <FloraDecor kind="sprig-tl" class="fp-sprig fp-sprig-tl" />
        <FloraDecor kind="sprig-tr" class="fp-sprig fp-sprig-tr" />
        <FloraDecor kind="side-l" class="fp-side-flora fp-side-flora-l fp-sway-l" style="top: 38%" />
        <FloraDecor kind="side-r" class="fp-side-flora fp-side-flora-r fp-sway-r" style="top: 52%" />

        <div class="fp-sec-head" v-reveal>
          <span class="fp-pill"><i class="fp-pill-dot" />百花齐放 · 实训室名录</span>
          <h2 class="fp-h2">实训室与导师风采</h2>
          <FloraDecor kind="divider" class="fp-divider" />
          <p>按学院与关键词筛选实训室，查看导师课题方向、招收名额与本季实时余量</p>
        </div>

        <!-- 导师风采横幅 -->
        <div class="fp-msb" v-reveal>
          <div class="fp-msb-art">
            <img src="/images/lab_discussion.jpg" alt="实训室组会研讨水彩插画" />
            <span class="fp-msb-badge"><i class="fp-pill-dot" />实训室开放日 · 师生面对面实景绘卷</span>
          </div>
          <div class="fp-msb-copy">
            <span class="fp-msb-kicker">师生共研 · 双向契合</span>
            <h3>在窗明几净的实训室里，找到志同道合的同行导师</h3>
            <p>先沟通、后锁定。导师公开研究方向与实训条件，学生至多填报 3 个平行意向，双向奔赴、不设先后，让每一次科研选择都不再盲目。</p>
            <div class="fp-msb-metrics">
              <div class="fp-msb-metric"><strong>{{ stats.labCount ?? labs.length }}</strong><span>本季开放实训室</span></div>
              <div class="fp-msb-metric"><strong>{{ stats.mentorCount ?? '—' }}</strong><span>驻室导师在线</span></div>
              <div class="fp-msb-metric"><strong>1:1 面谈</strong><span>开放日深度双向沟通</span></div>
            </div>
            <FloraDecor kind="panel-flora" class="fp-panel-flora" />
          </div>
        </div>

        <!-- 筛选条 -->
        <div class="fp-filter-bar" v-reveal>
          <button class="fp-filter-btn" :class="{ 'is-active': activeCollege === 0 }" type="button" @click="setCollege(0)">
            全部学院 ({{ labs.length }})
          </button>
          <button
            v-for="college in visibleColleges"
            :key="college.id"
            class="fp-filter-btn"
            :class="{ 'is-active': activeCollege === college.id }"
            type="button"
            @click="setCollege(college.id)"
          >
            <i class="fp-dot" :style="{ backgroundColor: college.accentColor || '#6d9612' }" />
            {{ college.name }} ({{ collegeCount(college.id) }})
          </button>
        </div>
        <div class="fp-tools" v-reveal>
          <div class="fp-search">
            <Icon name="search" :size="14" />
            <input v-model="keyword" placeholder="搜索实训室、导师或方向标签..." />
            <button v-if="keyword" class="fp-search-clear" type="button" @click="keyword = ''">×</button>
          </div>
          <button class="fp-filter-btn" :class="{ 'is-active': onlyAvailable }" type="button" @click="onlyAvailable = !onlyAvailable">仅看有余量</button>
        </div>

        <!-- 实训室卡片 -->
        <div v-if="!filteredLabs.length" class="fp-empty">
          <EmptyState title="未找到匹配的实训室或导师" desc="请尝试切换学院、清空搜索关键词，或关闭“仅看有余量”。" icon="search">
            <button class="fp-btn fp-btn-ghost" type="button" @click="resetFilters">重置全部筛选</button>
          </EmptyState>
        </div>
        <div v-else class="fp-mentor-grid">
          <article
            v-for="(lab, index) in filteredLabs"
            :key="lab.id"
            v-reveal
            class="fp-m-card"
            :style="{ '--accent': collegeAccent(lab.collegeId), '--d': `${Math.min(index, 6) * 60}ms` }"
          >
            <div>
              <div class="fp-m-top">
                <div class="fp-m-profile">
                  <div class="fp-m-avatar" :style="{ background: avatarTint(index) }">{{ lab.name.slice(0, 1) }}</div>
                  <div>
                    <h3 class="fp-m-name">{{ lab.name }}</h3>
                    <div class="fp-m-sub">{{ lab.collegeName }} · {{ lab.location || lab.contact || '校内实训空间' }}</div>
                  </div>
                </div>
                <span class="fp-quota" :class="{ 'is-full': !(lab.remainingQuota > 0) }">
                  {{ lab.remainingQuota > 0 ? `余 ${lab.remainingQuota} · 共 ${lab.totalQuota}` : '名额已满' }}
                </span>
              </div>
              <div class="fp-m-lab">{{ lab.code }} · 开放日 {{ shortOpenDay() }}</div>
              <div class="fp-m-tags">
                <span v-for="tag in directionTags(lab)" :key="tag" class="fp-m-tag">{{ tag }}</span>
              </div>
              <p class="fp-m-bio">“{{ lab.intro }}”</p>
              <div v-if="(lab.mentors || []).length" class="fp-m-mentors">
                <span class="fp-m-mentors-label">驻室导师（{{ lab.mentors.length }}）· 点击看详情</span>
                <div class="fp-m-chips">
                  <button
                    v-for="mentor in lab.mentors"
                    :key="mentor.id"
                    class="fp-m-chip"
                    type="button"
                    :title="`${mentor.name} ${mentor.title} · ${mentor.researchDirection}`"
                    @click="openMentor(mentor, lab)"
                  >
                    {{ mentor.name }}<em>{{ mentor.title }}</em>
                  </button>
                </div>
              </div>
            </div>
            <div class="fp-m-foot">
              <span class="fp-m-paper">平行意向不设先后 · 每生至多 3 位</span>
              <button class="fp-m-detail" type="button" @click="activeLab = lab">查看详情 →</button>
            </div>
          </article>
        </div>
        <FloraDecor kind="meadow" class="fp-meadow" />
      </section>

      <!-- ============ 八步选拔流程（竖向手风琴） ============ -->
      <section id="process" class="fp-process">
        <FloraDecor kind="loop-cloud" class="fp-loop-cloud fp-drift-a" />
        <FloraDecor kind="sprig-tr" class="fp-sprig fp-sprig-tr fp-sway-r" />

        <div class="fp-loop-head">
          <span class="fp-pill"><i class="fp-pill-dot" />循序渐进 · 阳光选拔</span>
          <h2 class="fp-h2">八步走完一次双向选择</h2>
          <FloraDecor kind="divider" class="fp-divider" />
        </div>

        <div class="fp-track">
          <div
            v-for="stage in stages"
            :key="stage.no"
            class="fp-panel"
            :class="{ 'is-active': activeStage === stage.no }"
            :data-step="stage.no"
            tabindex="0"
            role="button"
            :aria-label="`第${stage.no}步：${stage.title}`"
            @click="activeStage = stage.no"
            @keyup.enter="activeStage = stage.no"
          >
            <div class="fp-panel-in">
              <div class="fp-num">{{ pad2(stage.no) }}</div>
              <div class="fp-step">
                <FloraDecor kind="mini-cloud" class="fp-mini-cloud" :tint="miniTint(stage.no)" />
                <div class="fp-step-art">
                  <img :src="stageImg(stage.no)" :alt="stage.title" />
                  <span class="fp-step-tag">Step {{ pad2(stage.no) }} · {{ stage.title }}</span>
                </div>
                <span class="fp-step-date" :class="stage.state">{{ stage.time }}</span>
                <h3>{{ stage.title }}</h3>
                <p>{{ stage.desc }}</p>
              </div>
            </div>
            <FloraDecor kind="panel-flora" class="fp-panel-flora" />
          </div>
        </div>
      </section>

      <!-- ============ 通知公告 ============ -->
      <section id="notices" class="fp-notices">
        <FloraDecor kind="sprig-tl" class="fp-sprig fp-sprig-tl fp-sway-l" />
        <FloraDecor kind="side-r" class="fp-side-flora fp-side-flora-r fp-sway-r" style="top: 52%" />

        <div class="fp-sec-head" v-reveal>
          <span class="fp-pill"><i class="fp-pill-dot" />及时传达 · 院系动态</span>
          <h2 class="fp-h2">通知公告</h2>
          <FloraDecor kind="divider" class="fp-divider" />
          <p>双选日程安排、名额增补及评审细则第一时间向全院师生公开发布</p>
        </div>

        <div class="fp-notice-wrap">
          <div class="fp-notice-layout">
            <aside class="fp-notice-art" v-reveal>
              <img src="/images/notice_board.jpg" alt="校园公告栏水彩插画" />
              <div class="fp-notice-overlay">
                <span class="fp-pill"><i class="fp-pill-dot" />阳光公示 · 公开透明</span>
                <h4>每一份名额变动与日程提醒，皆在花期下公开透明</h4>
                <p>点击右侧任意公告条目即可在线查阅通知全文。</p>
              </div>
            </aside>

            <div class="fp-notice-list">
              <article
                v-for="(item, index) in announcements"
                :key="item.id"
                v-reveal
                class="fp-notice-item"
                :style="{ '--d': `${index * 60}ms` }"
                tabindex="0"
                @click="activeAnnouncement = item"
                @keyup.enter="activeAnnouncement = item"
              >
                <div class="fp-n-left">
                  <div class="fp-n-date">
                    <strong>{{ monthDay(item.publishStart).day }}</strong>
                    <span>{{ monthDay(item.publishStart).month }}</span>
                  </div>
                  <div class="fp-n-body">
                    <div class="fp-n-row">
                      <span class="fp-n-badge">{{ resolveMeta(announcementType, item.type).label }}</span>
                      <h3 class="fp-n-title">{{ item.title }}</h3>
                    </div>
                    <p class="fp-n-summary">{{ item.content }}</p>
                  </div>
                </div>
                <span class="fp-n-read">阅读全文 →</span>
              </article>
              <div v-if="!announcements.length" class="fp-empty-mini">暂无公告，选拔相关通知将在这里发布。</div>
            </div>
          </div>
        </div>
      </section>

      <!-- ============ 公示结果 ============ -->
      <section v-if="assignments.length" id="results" class="fp-results">
        <div class="fp-sec-head" v-reveal>
          <span class="fp-pill"><i class="fp-pill-dot" />阳光公示 · 结果锁定</span>
          <h2 class="fp-h2">公示结果</h2>
          <FloraDecor kind="divider" class="fp-divider" />
          <p>{{ home.publicity?.title || '本批次选拔结果已公示，公示期内如有异议请与学院联系。' }}</p>
        </div>
        <div class="fp-table-card" v-reveal>
          <div class="fp-table-head">
            <StatusPill :value="home.publicity?.status || 'PUBLISHED'" :map="publicityStatus" />
          </div>
          <div class="fp-table-wrap">
            <table class="fp-table">
              <thead>
                <tr><th>学生</th><th>专业</th><th>实训室</th><th>导师</th><th>录取方式</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in assignments" :key="row.id">
                  <td><strong>{{ row.studentName }}</strong><span class="fp-muted"> · {{ row.studentNo }}</span></td>
                  <td>{{ row.major }}</td>
                  <td>{{ row.labName }}</td>
                  <td>{{ row.mentorName }}</td>
                  <td><span class="fp-n-badge">{{ resolveMeta(assignmentSource, row.source).label }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <!-- ============ 规则 + 常见问题 + 底部 CTA ============ -->
      <section id="faq" class="fp-faq">
        <div class="fp-petals" aria-hidden="true">
          <i
            v-for="(p, i) in faqPetals"
            :key="i"
            class="fp-petal"
            :style="{ left: p.left + '%', '--dx': p.dx + 'px', '--s': p.scale, animationDuration: p.dur + 's', animationDelay: p.delay + 's' }"
          />
        </div>
        <FloraDecor kind="sprig-tl" class="fp-sprig fp-sprig-tl fp-sway-l" />
        <FloraDecor kind="sprig-tr" class="fp-sprig fp-sprig-tr fp-sway-r" />
        <FloraDecor kind="side-l" class="fp-side-flora fp-side-flora-l fp-sway-l" style="top: 34%" />

        <div class="fp-faq-inner">
          <div class="fp-sec-head" v-reveal>
            <span class="fp-pill"><i class="fp-pill-dot" />答疑解惑 · 常见问题</span>
            <h2 class="fp-h2">双选常见问题解答</h2>
            <FloraDecor kind="divider" class="fp-divider" />
          </div>

          <!-- 选拔规则速览 -->
          <div class="fp-rules" v-reveal>
            <div class="fp-rule"><Icon name="form" :size="16" /><span>每名学生在同一批次最多填报 <strong>3 个平行意向</strong>，不设志愿先后，不能重复选择同一导师。</span></div>
            <div class="fp-rule"><Icon name="check" :size="16" /><span>一名学生可以收到多个拟录取意向，但只能在规定时间内 <strong>确认其中一个</strong>；确认后其他意向自动释放名额。</span></div>
            <div class="fp-rule"><Icon name="clock" :size="16" /><span>逾期未确认视为放弃；放弃与逾期产生的空缺进入补录，按候补顺位依次递补。</span></div>
            <div class="fp-rule"><Icon name="shield" :size="16" /><span>调剂仅在学生授权范围内进行，优先实训室内部，其次跨实训室，最后安排谈话安置。</span></div>
          </div>

          <div class="fp-faq-list">
            <div
              v-for="(faq, index) in faqs"
              :key="index"
              v-reveal
              class="fp-faq-item"
              :class="{ 'is-open': openFaq === index }"
              :style="{ '--d': `${index * 50}ms` }"
            >
              <button
                class="fp-faq-q"
                type="button"
                :aria-expanded="openFaq === index"
                @click="openFaq = openFaq === index ? -1 : index"
              >
                <span>{{ index + 1 }}. {{ faq.q }}</span>
                <span class="fp-faq-icon">+</span>
              </button>
              <div class="fp-faq-a" :style="{ maxHeight: openFaq === index ? '220px' : '0px' }">
                <div class="fp-faq-a-in" v-html="faq.a" />
              </div>
            </div>
          </div>

          <!-- 底部启航 CTA -->
          <div class="fp-cta-card" v-reveal>
            <div class="fp-cta-banner">
              <img src="/images/bridge_scenery.jpg" alt="校园溪流石拱桥水彩风景画" />
              <span class="fp-cta-caption">走过春溪石桥 · 奔赴下一段学术山海</span>
            </div>
            <span class="fp-pill"><i class="fp-pill-dot" />花开有期 · 立即启航</span>
            <h2>准备好开启你的选拔之旅了吗？</h2>
            <p>使用学号或工号登录，立即管理你的双选志愿与招生名额。</p>
            <div class="fp-cta-btns">
              <button class="fp-btn" type="button" @click="goLogin">{{ auth.isLoggedIn ? '进入工作台' : '学生端登录' }}</button>
              <button class="fp-btn fp-btn-ghost" type="button" @click="goLogin">{{ auth.isLoggedIn ? '返回首页工作台' : '导师端登录' }}</button>
            </div>
          </div>
        </div>
        <FloraDecor kind="meadow" class="fp-meadow" />
      </section>
    </template>

    <!-- ============ 页脚 ============ -->
    <footer class="fp-footer">
      <a class="fp-f-logo" href="#top" @click.prevent="scrollTo('top')">
        <FloraDecor kind="logo" :size="46" />
        <span class="fp-f-logo-text">河北师范大学 · 实训室进组选拔</span>
      </a>
      <nav class="fp-f-links" aria-label="页脚导航">
        <a href="#mentors" @click.prevent="scrollTo('mentors')">实训室与导师</a>
        <a href="#process" @click.prevent="scrollTo('process')">选拔流程</a>
        <a href="#notices" @click.prevent="scrollTo('notices')">通知公告</a>
        <a href="#faq" @click.prevent="scrollTo('faq')">常见问题</a>
      </nav>
      <div class="fp-f-support">教务支持 · 各学院教务办公室</div>
      <div class="fp-f-copy">© 2026 河北师范大学 · 学生实训室进组选拔系统 · 数据仅用于教学演示</div>
    </footer>

    <!-- ============ 弹窗 ============ -->
    <ModalDialog :open="Boolean(activeLab)" :title="activeLab?.name || ''" :subtitle="activeLab?.code" wide @close="activeLab = null">
      <div v-if="activeLab" class="fp-modal-stack">
        <div v-if="activeLab.coverImage" class="fp-modal-cover">
          <img :src="activeLab.coverImage" :alt="activeLab.name" />
        </div>
        <div class="fp-modal-grid">
          <div><div class="fp-label">所属学院</div><p>{{ activeLab.collegeName }}</p></div>
          <div><div class="fp-label">空间位置</div><p>{{ activeLab.location || activeLab.contact || '—' }}</p></div>
          <div><div class="fp-label">研究方向</div><p>{{ activeLab.direction }}</p></div>
          <div><div class="fp-label">联系方式</div><p>{{ activeLab.contact || '—' }}</p></div>
        </div>
        <div><div class="fp-label">实训室简介</div><p>{{ activeLab.intro }}</p></div>
        <div><div class="fp-label">培养安排</div><p>{{ activeLab.trainingPlan || '—' }}</p></div>
        <div><div class="fp-label">选拔要求</div><p>{{ activeLab.requirements || '—' }}</p></div>
        <div>
          <div class="fp-label">导师名单（{{ (activeLab.mentors || []).length }}）</div>
          <div class="fp-modal-mentors">
            <div v-for="mentor in activeLab.mentors || []" :key="mentor.id" class="fp-modal-mentor">
              <strong>{{ mentor.name }}</strong><span>{{ mentor.title }}</span>
              <p>{{ mentor.researchDirection }}</p>
              <em>招生 {{ mentor.quota }} 人 · 已用 {{ mentor.confirmedCount || 0 }} 人</em>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="fp-btn fp-btn-ghost" type="button" @click="activeLab = null">关闭</button>
        <button class="fp-btn" type="button" @click="goLogin">去了解填报</button>
      </template>
    </ModalDialog>

    <ModalDialog :open="Boolean(activeMentor)" :title="activeMentor?.mentor?.name || ''" :subtitle="activeMentor?.lab?.name" @close="activeMentor = null">
      <div v-if="activeMentor" class="fp-modal-stack">
        <div class="fp-modal-mentor-head">
          <div class="fp-m-avatar" style="width: 52px; height: 52px; font-size: 18px">{{ activeMentor.mentor.name.slice(0, 1) }}</div>
          <div>
            <h3>{{ activeMentor.mentor.name }} <span>{{ activeMentor.mentor.title }}</span></h3>
            <p>{{ activeMentor.lab.collegeName }} · {{ activeMentor.lab.name }}</p>
          </div>
        </div>
        <div class="fp-modal-grid">
          <div><div class="fp-label">研究方向</div><p>{{ activeMentor.mentor.researchDirection }}</p></div>
          <div><div class="fp-label">招生名额</div><p>{{ activeMentor.mentor.quota }} 人</p></div>
          <div><div class="fp-label">报名热度</div><p>{{ activeMentor.mentor.applicantCount || 0 }} 人</p></div>
          <div><div class="fp-label">空间位置</div><p>{{ activeMentor.lab.location || activeMentor.lab.contact || '—' }}</p></div>
        </div>
        <p>{{ activeMentor.mentor.profile }}</p>
        <div class="fp-rule"><Icon name="spark" :size="16" /><span>平行意向不设先后，最多可选择 3 位导师。提交前请确认自己的兴趣方向与投入时间。</span></div>
      </div>
      <template #footer>
        <button class="fp-btn fp-btn-ghost" type="button" @click="activeMentor = null">关闭</button>
        <button class="fp-btn" type="button" @click="mentorAction(activeMentor.mentor, activeMentor.lab)">
          {{ auth.isLoggedIn ? '去填报' : '登录后填报' }}
        </button>
      </template>
    </ModalDialog>

    <ModalDialog :open="Boolean(activeAnnouncement)" :title="activeAnnouncement?.title || ''" :subtitle="fmt(activeAnnouncement?.publishStart)" @close="activeAnnouncement = null">
      <p class="fp-modal-article">{{ activeAnnouncement?.content }}</p>
    </ModalDialog>
  </div>
</template>

<style>
/* ============================================================
   公开首页 · 日系水彩风（花栖 Flora 主题）
   命名前缀 fp-，与内页国风样式互不干扰
   ============================================================ */
.fp-home {
  --fp-ink: #161914;
  --fp-olive: #4a6714;
  --fp-green: #6d9612;
  --fp-lime: #b9d458;
  --fp-blossom: #f5b8cb;
  --fp-blossom-deep: #d96b8a;
  --fp-pill-bg: #e9f1d2;
  --fp-line: #ecefe6;
  --fp-cream: #fafbf7;
  position: relative;
  background: #fff;
  color: var(--fp-ink);
  overflow-x: hidden;
  font-family: "Inter Tight", "Inter", "PingFang SC", "Microsoft YaHei", sans-serif;
}
.fp-home .fp-muted { color: #6b7361; font-size: 12px; }
.fp-home .fp-label { font-size: 11px; font-weight: 600; color: var(--fp-olive); letter-spacing: .06em; margin-bottom: 4px; }

@keyframes fpFall {
  to { transform: translate(var(--dx, 30px), 110vh) rotate(540deg) scale(var(--s, 1)); }
}
@keyframes fpDriftA { to { transform: translateX(26px) translateY(4px); } }
@keyframes fpDriftB { to { transform: translateX(-30px) translateY(6px); } }
@keyframes fpSwayL {
  0%, 100% { transform: rotate(-1.8deg) translateY(0); }
  50% { transform: rotate(2.2deg) translateY(-3px); }
}
@keyframes fpSwayR {
  0%, 100% { transform: scaleX(-1) rotate(-1.8deg) translateY(0); }
  50% { transform: scaleX(-1) rotate(2.2deg) translateY(-3px); }
}
@keyframes fpRise {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}
.fp-drift-a { animation: fpDriftA 14s ease-in-out infinite alternate; }
.fp-drift-b { animation: fpDriftB 17s ease-in-out infinite alternate; }
.fp-sway-l { transform-origin: 0% 50%; animation: fpSwayL 9s ease-in-out infinite; }
.fp-sway-r { transform-origin: 0% 50%; animation: fpSwayR 10.5s ease-in-out infinite; }

/* ---------- 通用元件 ---------- */
.fp-home .fp-pill {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; font-weight: 500; padding: 5px 12px; border-radius: 999px;
  background: var(--fp-pill-bg); color: #2c3a10; letter-spacing: .02em;
}
.fp-home .fp-pill-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--fp-blossom-deep); flex: 0 0 auto; }
.fp-home .fp-dot { width: 7px; height: 7px; border-radius: 50%; flex: 0 0 auto; display: inline-block; }
.fp-home .fp-h2 {
  font-family: "Noto Serif SC", serif;
  font-size: clamp(28px, 3.2vw, 44px); font-weight: 600; letter-spacing: -.02em;
  line-height: 1.2; text-align: center; color: var(--fp-ink);
}
.fp-home .fp-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  height: 38px; padding: 0 18px; border-radius: 999px; border: none; cursor: pointer;
  background: var(--fp-olive); color: #f6fae8; font-size: 13.5px; font-weight: 600; white-space: nowrap;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .25), 0 3px 10px rgba(60, 80, 10, .22);
  transition: transform .3s cubic-bezier(.2, .8, .2, 1), background .3s;
}
.fp-home .fp-btn:hover { transform: translateY(-1px); background: #5b7d18; color: #f6fae8; }
.fp-home .fp-btn-ghost {
  background: rgba(255, 255, 255, .86); color: var(--fp-ink);
  border: 1px solid rgba(0, 0, 0, .09); box-shadow: 0 2px 8px rgba(0, 0, 0, .04);
}
.fp-home .fp-btn-ghost:hover { background: #fff; color: var(--fp-ink); }

/* ---------- 浮动胶囊导航 ---------- */
.fp-nav {
  position: fixed; top: 18px; left: 50%; transform: translateX(-50%);
  z-index: 60; display: flex; align-items: center; gap: 20px;
  padding: 6px 6px 6px 16px; border-radius: 999px;
  background: rgba(255, 255, 255, .88);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, .05), 0 8px 24px rgba(0, 0, 0, .06);
  backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  max-width: calc(100vw - 24px);
}
.fp-brand { display: inline-flex; align-items: center; gap: 8px; white-space: nowrap; }
.fp-brand-text { display: flex; flex-direction: column; line-height: 1.2; }
.fp-brand-text strong { font-size: 15px; font-weight: 700; letter-spacing: .02em; color: var(--fp-ink); }
.fp-brand-text em { font-style: normal; font-size: 10px; color: #6b7361; letter-spacing: .18em; }
.fp-nav-links { display: flex; align-items: center; gap: 18px; }
.fp-nav-links a { font-size: 13.5px; font-weight: 500; color: #353b2e; white-space: nowrap; transition: color .2s; }
.fp-nav-links a:hover { color: var(--fp-olive); }
.fp-nav-stage {
  display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600;
  color: var(--fp-olive); background: rgba(185, 212, 88, .18); padding: 4px 11px; border-radius: 999px; white-space: nowrap;
}
.fp-nav-stage .fp-dot { width: 6px; height: 6px; background: var(--fp-green); }

/* ---------- 错误条 / 加载态 ---------- */
.fp-error {
  position: fixed; top: 76px; left: 50%; transform: translateX(-50%); z-index: 70;
  display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-radius: 999px;
  background: #fdecec; color: #a02830; font-size: 13px; box-shadow: 0 8px 22px rgba(160, 40, 48, .15);
}
.fp-error-retry { border: none; background: none; color: #a02830; font-weight: 700; cursor: pointer; text-decoration: underline; }
.fp-loading { min-height: 100vh; padding: 140px 4vw 80px; display: flex; flex-direction: column; align-items: center; gap: 26px; }
.fp-loading-grid { width: min(1140px, 100%); display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.fp-skeleton {
  height: 380px; border-radius: 22px; background: linear-gradient(100deg, #f4f7ee 30%, #fdfbf6 50%, #f4f7ee 70%);
  background-size: 200% 100%; animation: fpShimmer 1.6s linear infinite;
}
@keyframes fpShimmer { to { background-position: -200% 0; } }

/* ---------- 首屏 HERO ---------- */
.fp-hero { position: relative; height: 100vh; min-height: 650px; overflow: hidden; background: #fff; }
.fp-cloud { position: absolute; z-index: 4; pointer-events: none; }
.fp-cloud-l { top: -2%; left: -5%; width: min(42vw, 560px); }
.fp-cloud-r { top: 3%; right: -6%; width: min(40vw, 540px); }
.fp-hero-copy {
  position: relative; z-index: 6; top: 22%; max-width: 780px; margin: 0 auto; padding: 0 20px; text-align: center;
}
.fp-hero-copy h1 {
  margin: 18px 0 16px; font-family: "Noto Serif SC", serif;
  font-size: clamp(34px, 4.7vw, 64px); font-weight: 600; letter-spacing: -.03em; line-height: 1.16; color: var(--fp-ink);
  animation: fpRise .9s .15s cubic-bezier(.2, .8, .2, 1) both;
}
.fp-hero-copy h1 em { font-style: normal; color: var(--fp-olive); }
.fp-hero-copy > .fp-pill { animation: fpRise .8s .05s cubic-bezier(.2, .8, .2, 1) both; }
.fp-hero-copy p {
  font-size: 15.5px; color: #434a3b; max-width: 540px; margin: 0 auto 24px; line-height: 1.65;
  animation: fpRise .9s .3s cubic-bezier(.2, .8, .2, 1) both;
}
.fp-hero-actions { display: flex; align-items: center; justify-content: center; gap: 12px; flex-wrap: wrap; animation: fpRise .9s .45s cubic-bezier(.2, .8, .2, 1) both; }
.fp-hero-media {
  position: absolute; left: 0; right: 0; bottom: 0; height: 68%; z-index: 2; pointer-events: none;
  mask-image: linear-gradient(to bottom, transparent 0%, #000 24%); -webkit-mask-image: linear-gradient(to bottom, transparent 0%, #000 24%);
}
.fp-hero-media img { width: 100%; height: 100%; object-fit: cover; object-position: center 62%; display: block; }
.fp-hero-glass, .fp-petals { position: absolute; inset: 0; pointer-events: none; overflow: hidden; }
.fp-hero-glass { z-index: 5; }
.fp-petals { z-index: 6; }
.fp-glass {
  position: absolute; border-radius: 12px;
  background: rgba(255, 255, 255, .28); border: 1px solid rgba(255, 255, 255, .68);
  backdrop-filter: blur(10px) saturate(1.2); -webkit-backdrop-filter: blur(10px) saturate(1.2);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .45);
  width: clamp(34px, 5vw, 68px); aspect-ratio: 1;
  animation: fpGlassPulse 4s ease-in-out infinite;
}
.fp-glass.is-center { width: clamp(26px, 3.2vw, 42px); }
@keyframes fpGlassPulse {
  0%, 100% { opacity: .85; }
  50% { opacity: .35; }
}
.fp-petal {
  position: absolute; top: -24px; width: 11px; height: 8px; border-radius: 80% 0 80% 0;
  background: linear-gradient(135deg, #ffd6e4, #f4a4be); opacity: .82;
  animation: fpFall linear infinite;
}
.fp-meadow {
  position: absolute; left: 0; right: 0; bottom: -2px; height: clamp(54px, 6.8vw, 92px);
  z-index: 4; pointer-events: none; overflow: hidden;
}
.fp-meadow svg { width: 100%; height: 100%; display: block; }

/* ---------- 区块通用 ---------- */
.fp-sec-head { position: relative; z-index: 3; max-width: 680px; margin: 0 auto; text-align: center; }
.fp-sec-head .fp-h2 { margin: 14px 0 12px; }
.fp-sec-head p { font-size: 14.5px; color: #4b5243; line-height: 1.6; }
.fp-divider { display: flex; align-items: center; justify-content: center; margin: 10px auto 6px; pointer-events: none; }
.fp-divider svg { width: clamp(190px, 24vw, 270px); height: auto; overflow: visible; }
.fp-sprig { position: absolute; pointer-events: none; z-index: 2; opacity: .88; }
.fp-sprig-tl { top: 26px; left: -18px; width: clamp(180px, 22vw, 310px); }
.fp-sprig-tr { top: 22px; right: -18px; width: clamp(180px, 22vw, 310px); transform: scaleX(-1); }
.fp-side-flora { position: absolute; pointer-events: none; z-index: 2; }
.fp-side-flora-l { left: -12px; width: clamp(95px, 11vw, 165px); }
.fp-side-flora-r { right: -12px; width: clamp(95px, 11vw, 165px); }

/* ---------- 实训室与导师区 ---------- */
.fp-mentors {
  position: relative; padding: 110px 4vw 100px; overflow: hidden;
  background:
    radial-gradient(ellipse 55% 38% at 6% 18%, rgba(252, 228, 236, .48), transparent 70%),
    radial-gradient(ellipse 50% 35% at 94% 42%, rgba(233, 241, 210, .62), transparent 70%),
    radial-gradient(ellipse 60% 35% at 50% 96%, rgba(247, 250, 238, .9), transparent 75%),
    #fdfefb;
}
.fp-sq-field { position: absolute; inset: 0 0 auto 0; height: 42%; z-index: 1; pointer-events: none; mask-image: linear-gradient(#000, transparent); -webkit-mask-image: linear-gradient(#000, transparent); }
.fp-grid-sq { position: absolute; width: 46px; height: 46px; border: 1px solid #ebefe3; border-radius: 10px; pointer-events: none; }

.fp-msb {
  position: relative; z-index: 3; max-width: 1140px; margin: 28px auto 34px;
  border-radius: 24px; border: 1px solid #e2ead4;
  background: linear-gradient(135deg, rgba(252, 228, 236, .42) 0%, rgba(250, 251, 247, .96) 46%, rgba(233, 241, 210, .55) 100%);
  padding: 18px 22px 18px 18px; display: grid; grid-template-columns: 1.15fr 1fr; gap: 28px; align-items: center;
  box-shadow: 0 18px 42px rgba(45, 60, 15, .06);
}
.fp-msb-art {
  position: relative; border-radius: 18px; overflow: hidden; aspect-ratio: 16 / 9;
  border: 2px solid #fff; box-shadow: 0 12px 28px rgba(35, 48, 12, .1); background: #f7f4f0;
}
.fp-msb-art img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .65s cubic-bezier(.2, .8, .2, 1); }
.fp-msb:hover .fp-msb-art img { transform: scale(1.03); }
.fp-msb-badge {
  position: absolute; left: 14px; bottom: 14px; display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 13px; border-radius: 999px; background: rgba(255, 255, 255, .9); backdrop-filter: blur(10px);
  font-size: 12px; font-weight: 600; color: var(--fp-olive); box-shadow: 0 4px 14px rgba(0, 0, 0, .08);
}
.fp-msb-copy { padding: 6px 8px 6px 0; position: relative; overflow: hidden; }
.fp-msb-kicker {
  display: inline-flex; align-items: center; gap: 6px; font-size: 12px; font-weight: 600; color: var(--fp-blossom-deep);
  background: rgba(245, 184, 203, .24); padding: 4px 11px; border-radius: 999px; margin-bottom: 12px;
}
.fp-msb-copy h3 { font-family: "Noto Serif SC", serif; font-size: clamp(20px, 2vw, 26px); font-weight: 600; line-height: 1.35; margin-bottom: 10px; }
.fp-msb-copy p { font-size: 14px; line-height: 1.68; color: #444c3b; margin-bottom: 18px; }
.fp-msb-metrics { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding-top: 14px; border-top: 1px dashed #dce3d0; }
.fp-msb-metric strong { display: block; font-family: "Noto Serif SC", serif; font-size: 20px; font-weight: 700; color: var(--fp-olive); }
.fp-msb-metric span { font-size: 12px; color: #5b644f; }
.fp-panel-flora {
  position: absolute; right: 12px; bottom: 0; width: clamp(110px, 14vw, 195px);
  pointer-events: none; z-index: 1; opacity: .72; transition: opacity .4s ease, transform .5s ease;
}

.fp-filter-bar { position: relative; z-index: 3; display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; margin: 28px 0 16px; }
.fp-filter-btn {
  padding: 7px 15px; border-radius: 999px; font-size: 13px; font-weight: 500; cursor: pointer;
  background: var(--fp-cream); border: 1px solid var(--fp-line); color: #3d4435;
  display: inline-flex; align-items: center; gap: 6px; transition: all .25s ease;
}
.fp-filter-btn:hover { border-color: var(--fp-olive); color: var(--fp-olive); }
.fp-filter-btn.is-active {
  background: var(--fp-olive); border-color: var(--fp-olive); color: #f6fae8;
  box-shadow: 0 4px 12px rgba(74, 103, 20, .2);
}
.fp-tools {
  position: relative; z-index: 3; display: flex; align-items: center; justify-content: center; gap: 10px;
  flex-wrap: wrap; margin-bottom: 38px;
}
.fp-search {
  display: flex; align-items: center; gap: 8px; padding: 0 14px; height: 36px; min-width: 300px;
  background: #fff; border: 1px solid var(--fp-line); border-radius: 999px; color: #6b7361;
}
.fp-search input { flex: 1; border: none; outline: none; background: transparent; font-size: 13px; color: var(--fp-ink); }
.fp-search-clear { border: none; background: #eef2e4; border-radius: 50%; width: 20px; height: 20px; cursor: pointer; color: #5b644f; line-height: 1; }

.fp-mentor-grid { position: relative; z-index: 3; max-width: 1140px; margin: 0 auto; display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.fp-m-card {
  position: relative; overflow: hidden; border-radius: 22px; background: var(--fp-cream);
  border: 1px solid var(--fp-line); padding: 24px; display: flex; flex-direction: column; justify-content: space-between;
  transition: transform .35s cubic-bezier(.2, .8, .2, 1), box-shadow .35s ease, border-color .35s ease;
}
.fp-m-card:hover { transform: translateY(-4px); border-color: #d7dfc7; box-shadow: 0 18px 36px rgba(45, 60, 15, .07); }
.fp-m-card::after {
  content: ""; position: absolute; right: -14px; bottom: -14px; width: 96px; height: 96px; pointer-events: none; opacity: .38;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100' fill='none'%3E%3Cpath d='M95 95C72 78 52 54 38 22M68 74C52 78 36 75 20 68M78 55C76 38 68 25 56 14' stroke='%236d9612' stroke-width='1.8' stroke-linecap='round' opacity='.55'/%3E%3Ccircle cx='38' cy='22' r='8' fill='%23f5b8cb' opacity='.75'/%3E%3Ccircle cx='56' cy='14' r='6' fill='%23FADCE6' opacity='.85'/%3E%3Ccircle cx='20' cy='68' r='7' fill='%23f5b8cb' opacity='.7'/%3E%3Cellipse cx='54' cy='52' rx='10' ry='4.5' transform='rotate(-35 54 52)' fill='%23b9d458' opacity='.65'/%3E%3Cellipse cx='44' cy='76' rx='9' ry='4' transform='rotate(15 44 76)' fill='%236d9612' opacity='.45'/%3E%3C/svg%3E");
  background-size: contain; background-repeat: no-repeat; transition: transform .4s ease, opacity .4s ease;
}
.fp-m-card:hover::after { transform: scale(1.08) rotate(-4deg); opacity: .58; }
.fp-m-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.fp-m-profile { display: flex; align-items: center; gap: 12px; min-width: 0; }
.fp-m-avatar {
  width: 52px; height: 52px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  font-family: "Noto Serif SC", serif; font-weight: 700; font-size: 18px; color: #263112;
  border: 2px solid #fff; box-shadow: 0 4px 12px rgba(0, 0, 0, .06);
}
.fp-m-name { font-size: 16.5px; font-weight: 600; color: var(--fp-ink); line-height: 1.3; }
.fp-m-sub { font-size: 12px; color: #5c6550; margin-top: 2px; }
.fp-quota {
  position: relative; flex-shrink: 0; background: var(--fp-lime); color: #182006;
  font-weight: 600; font-size: 11.5px; padding: 5px 12px; border-radius: 999px; white-space: nowrap;
  mask-image:
    radial-gradient(circle 4px at 0 50%, #0000 96%, #000) left/51% 100% no-repeat,
    radial-gradient(circle 4px at 100% 50%, #0000 96%, #000) right/51% 100% no-repeat;
  -webkit-mask-image:
    radial-gradient(circle 4px at 0 50%, #0000 96%, #000) left/51% 100% no-repeat,
    radial-gradient(circle 4px at 100% 50%, #0000 96%, #000) right/51% 100% no-repeat;
}
.fp-quota.is-full { background: #e5e8df; color: #6a7260; }
.fp-m-lab {
  display: inline-block; margin-top: 12px; font-size: 12px; font-weight: 600; color: var(--fp-olive);
  background: rgba(185, 212, 88, .22); padding: 3px 10px; border-radius: 6px;
}
.fp-m-tags { display: flex; flex-wrap: wrap; gap: 6px; margin: 14px 0 12px; }
.fp-m-tag { font-size: 11.5px; padding: 3px 9px; border-radius: 6px; background: #fff; border: 1px solid var(--fp-line); color: #464f3b; }
.fp-m-bio {
  font-size: 13.5px; line-height: 1.6; color: #3e4437;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.fp-m-mentors { margin-top: 14px; padding-top: 12px; border-top: 1px dashed #dfe4d6; }
.fp-m-mentors-label { font-size: 11px; color: #5f6854; font-weight: 600; }
.fp-m-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.fp-m-chip {
  display: inline-flex; align-items: center; gap: 5px; padding: 3px 10px; border-radius: 999px; cursor: pointer;
  background: #fff; border: 1px solid var(--fp-line); font-size: 12px; font-weight: 600; color: #3d4435;
  transition: all .2s;
}
.fp-m-chip em { font-style: normal; font-size: 10px; font-weight: 400; color: #8a9279; }
.fp-m-chip:hover { border-color: var(--fp-green); color: var(--fp-olive); }
.fp-m-foot {
  border-top: 1px dashed #dfe4d6; padding-top: 14px; margin-top: 16px;
  display: flex; align-items: center; justify-content: space-between; gap: 10px;
}
.fp-m-paper { font-size: 12px; color: #5f6854; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 65%; }
.fp-m-detail { border: none; background: none; cursor: pointer; font-size: 13px; font-weight: 600; color: var(--fp-olive); white-space: nowrap; transition: transform .2s; }
.fp-m-detail:hover { transform: translateX(2px); }
.fp-empty { position: relative; z-index: 3; max-width: 720px; margin: 0 auto; }

/* ---------- 流程区（8 面板手风琴） ---------- */
.fp-process {
  position: relative; height: 100vh; min-height: 700px; overflow: hidden;
  background:
    radial-gradient(ellipse 48% 35% at 88% 15%, rgba(252, 228, 236, .42), transparent 70%),
    radial-gradient(ellipse 45% 40% at 12% 85%, rgba(233, 241, 210, .5), transparent 70%),
    #fdfefb;
  border-top: 1px solid var(--fp-line);
}
.fp-loop-cloud { position: absolute; z-index: 3; top: 4vh; left: -3vw; width: min(32vw, 440px); pointer-events: none; }
.fp-loop-head { position: absolute; z-index: 4; top: 7vh; width: 100%; display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 0 16px; pointer-events: none; }
.fp-track { position: absolute; z-index: 2; inset: 0; display: flex; }
.fp-panel {
  position: relative; flex: 0 0 clamp(62px, 7.2vw, 116px); height: 100%; overflow: hidden;
  border-right: 1px solid var(--fp-line); cursor: pointer; outline: none;
  transition: flex-grow .9s cubic-bezier(.77, 0, .18, 1), background-color .4s;
}
.fp-panel:last-child { border-right: none; }
.fp-panel:hover { background-color: rgba(250, 251, 247, .6); }
.fp-panel.is-active { flex-grow: 1; cursor: default; background-color: #fff; }
.fp-panel-in {
  position: absolute; left: clamp(14px, 1.8vw, 28px); bottom: 9vh;
  display: flex; align-items: flex-end; gap: clamp(16px, 2vw, 30px); width: max-content;
}
.fp-num {
  font-size: clamp(150px, 31vh, 330px); font-weight: 600; line-height: .74; letter-spacing: -.07em;
  user-select: none; font-family: "Inter Tight", "Inter", sans-serif;
}
.fp-panel[data-step="1"] .fp-num, .fp-panel[data-step="5"] .fp-num { color: var(--fp-olive); }
.fp-panel[data-step="2"] .fp-num, .fp-panel[data-step="6"] .fp-num { color: var(--fp-green); }
.fp-panel[data-step="3"] .fp-num, .fp-panel[data-step="7"] .fp-num { color: var(--fp-lime); }
.fp-panel[data-step="4"] .fp-num, .fp-panel[data-step="8"] .fp-num { color: var(--fp-blossom-deep); }
.fp-step {
  position: relative; width: min(410px, 31vw); padding-bottom: 4px;
  opacity: 0; transform: translateX(-24px); filter: blur(6px);
  transition: opacity .35s, transform .45s, filter .35s;
}
.fp-panel.is-active .fp-step {
  opacity: 1; transform: none; filter: blur(0);
  transition: opacity .65s .35s, transform .85s .3s cubic-bezier(.2, .8, .2, 1), filter .65s .35s;
}
.fp-step-date {
  display: inline-block; font-size: 12px; font-weight: 600; color: var(--fp-olive);
  background: var(--fp-pill-bg); padding: 4px 10px; border-radius: 999px; margin-bottom: 10px;
}
.fp-step-date.active { color: #7a1f3d; background: #fce4ec; }
.fp-step-date.done { color: #5b644f; background: #eef1e6; }
.fp-step h3 { font-family: "Noto Serif SC", serif; font-size: clamp(20px, 1.7vw, 26px); font-weight: 600; letter-spacing: -.02em; margin-bottom: 10px; color: var(--fp-ink); }
.fp-step p { font-size: 14px; line-height: 1.6; color: #34392e; }
.fp-step-art {
  position: relative; width: 100%; height: clamp(145px, 20vh, 210px); border-radius: 16px; overflow: hidden;
  margin-bottom: 14px; border: 2px solid #fff; box-shadow: 0 12px 28px rgba(35, 48, 14, .11); background: var(--fp-cream);
}
.fp-step-art img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .7s cubic-bezier(.2, .8, .2, 1); }
.fp-panel.is-active:hover .fp-step-art img { transform: scale(1.04); }
.fp-step-tag {
  position: absolute; right: 10px; bottom: 10px; padding: 3px 10px; border-radius: 999px;
  font-size: 11px; font-weight: 600; background: rgba(255, 255, 255, .88); color: #2c3a10; backdrop-filter: blur(6px);
}
.fp-mini-cloud {
  position: absolute; left: -6%; bottom: calc(100% + clamp(54px, 10vh, 120px));
  width: min(16vw, 210px); pointer-events: none; animation: fpDriftA 9s ease-in-out infinite alternate;
}
.fp-panel .fp-panel-flora { opacity: .32; width: 78px; right: 4px; }
.fp-panel.is-active .fp-panel-flora { opacity: .88; transform: translateY(-2px); width: clamp(110px, 14vw, 195px); }

/* ---------- 通知公告区 ---------- */
.fp-notices {
  position: relative; padding: 110px 4vw; overflow: hidden;
  background:
    radial-gradient(ellipse 52% 40% at 92% 20%, rgba(252, 228, 236, .44), transparent 70%),
    radial-gradient(ellipse 55% 42% at 8% 82%, rgba(225, 238, 195, .48), transparent 70%),
    var(--fp-cream);
  border-top: 1px solid var(--fp-line); border-bottom: 1px solid var(--fp-line);
}
.fp-notice-wrap { max-width: 1140px; margin: 0 auto; }
.fp-notice-layout { display: grid; grid-template-columns: 360px 1fr; gap: 28px; align-items: stretch; margin-top: 34px; }
.fp-notice-art {
  position: relative; border-radius: 24px; overflow: hidden; border: 2px solid #fff;
  box-shadow: 0 18px 40px rgba(40, 55, 15, .09); background: #f5f2eb;
  display: flex; flex-direction: column; justify-content: flex-end; min-height: 460px;
}
.fp-notice-art img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; object-position: center 35%; display: block; transition: transform .7s ease; }
.fp-notice-art:hover img { transform: scale(1.03); }
.fp-notice-overlay {
  position: relative; z-index: 2; margin: 14px; padding: 18px 18px 16px; border-radius: 18px;
  background: rgba(255, 255, 255, .9); backdrop-filter: blur(12px); -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, .95); box-shadow: 0 10px 24px rgba(20, 28, 10, .08);
}
.fp-notice-overlay .fp-pill { margin-bottom: 8px; }
.fp-notice-overlay h4 { font-family: "Noto Serif SC", serif; font-size: 16.5px; font-weight: 600; margin-bottom: 5px; }
.fp-notice-overlay p { font-size: 12.5px; line-height: 1.55; color: #49523e; }
.fp-notice-list { display: flex; flex-direction: column; gap: 12px; min-width: 0; }
.fp-notice-item {
  position: relative; overflow: hidden; background: #fff; border: 1px solid var(--fp-line); border-radius: 18px;
  padding: 20px 24px; display: flex; justify-content: space-between; align-items: center; gap: 18px; cursor: pointer;
  transition: border-color .25s ease, transform .25s ease, box-shadow .25s ease;
}
.fp-notice-item:hover { border-color: var(--fp-olive); transform: translateX(4px); box-shadow: 0 10px 24px rgba(45, 60, 15, .05); }
.fp-n-left { display: flex; align-items: center; gap: 18px; min-width: 0; }
.fp-n-date { flex-shrink: 0; width: 60px; text-align: center; padding: 8px 6px; border-radius: 12px; background: var(--fp-cream); border: 1px solid var(--fp-line); }
.fp-n-date strong { display: block; font-size: 16px; font-weight: 700; line-height: 1.2; }
.fp-n-date span { font-size: 11px; color: #6b7361; }
.fp-n-body { min-width: 0; }
.fp-n-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 5px; }
.fp-n-badge { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 999px; background: var(--fp-pill-bg); color: #2c3a10; white-space: nowrap; }
.fp-n-title { font-size: 15.5px; font-weight: 600; }
.fp-n-summary { font-size: 13px; color: #58604e; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fp-n-read { flex-shrink: 0; font-size: 13px; font-weight: 600; color: var(--fp-olive); white-space: nowrap; }
.fp-empty-mini { padding: 40px; text-align: center; color: #8a9279; font-size: 13px; background: #fff; border: 1px dashed #dce3d0; border-radius: 18px; }

/* ---------- 公示结果区 ---------- */
.fp-results {
  position: relative; padding: 110px 4vw; overflow: hidden;
  background:
    radial-gradient(ellipse 50% 36% at 90% 14%, rgba(233, 241, 210, .55), transparent 70%),
    #fdfefb;
  border-bottom: 1px solid var(--fp-line);
}
.fp-table-card { max-width: 1140px; margin: 34px auto 0; background: #fff; border: 1px solid var(--fp-line); border-radius: 24px; box-shadow: 0 18px 42px rgba(45, 60, 15, .06); overflow: hidden; }
.fp-table-head { display: flex; align-items: center; justify-content: flex-end; padding: 14px 20px; border-bottom: 1px solid var(--fp-line); background: var(--fp-cream); }
.fp-table-wrap { overflow-x: auto; }
.fp-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.fp-table th { text-align: left; font-size: 11px; letter-spacing: .08em; color: #6b7361; font-weight: 600; padding: 12px 20px; border-bottom: 1px solid var(--fp-line); white-space: nowrap; }
.fp-table td { padding: 13px 20px; border-bottom: 1px dashed #e6ebe0; color: #3e4437; white-space: nowrap; }
.fp-table tbody tr:last-child td { border-bottom: none; }
.fp-table tbody tr:hover { background: #fafbf7; }
.fp-table strong { color: var(--fp-ink); }

/* ---------- FAQ 与底部 CTA ---------- */
.fp-faq {
  position: relative; padding: 110px 4vw 130px; overflow: hidden;
  background:
    radial-gradient(ellipse 80% 45% at 50% 100%, rgba(252, 228, 236, .55), transparent 70%),
    #fff;
}
.fp-faq-inner { position: relative; z-index: 3; max-width: 880px; margin: 0 auto; }
.fp-rules { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; margin-top: 38px; }
.fp-rule {
  display: flex; align-items: flex-start; gap: 10px; padding: 16px 18px; border-radius: 16px;
  background: var(--fp-cream); border: 1px solid var(--fp-line); font-size: 13px; line-height: 1.65; color: #464e3d;
}
.fp-rule svg { flex-shrink: 0; color: var(--fp-olive); margin-top: 2px; }
.fp-rule strong { color: var(--fp-ink); }
.fp-faq-list { margin-top: 30px; display: flex; flex-direction: column; gap: 12px; }
.fp-faq-item { position: relative; overflow: hidden; border-radius: 18px; border: 1px solid var(--fp-line); background: var(--fp-cream); transition: border-color .25s ease, background .25s ease; }
.fp-faq-item.is-open { background: #fff; border-color: #d3dec0; box-shadow: 0 12px 28px rgba(45, 60, 15, .05); }
.fp-faq-q {
  width: 100%; padding: 20px 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px;
  text-align: left; border: none; background: none; cursor: pointer;
  font-size: 15.5px; font-weight: 600; color: var(--fp-ink);
}
.fp-faq-icon {
  flex-shrink: 0; width: 26px; height: 26px; border-radius: 50%; background: var(--fp-pill-bg); color: var(--fp-olive);
  display: flex; align-items: center; justify-content: center; font-size: 17px; font-weight: 500;
  transition: transform .3s ease, background .3s ease;
}
.fp-faq-item.is-open .fp-faq-icon { transform: rotate(45deg); background: var(--fp-olive); color: #fff; }
.fp-faq-a { max-height: 0; overflow: hidden; transition: max-height .38s cubic-bezier(.25, .8, .25, 1); }
.fp-faq-a-in { padding: 0 24px 20px; font-size: 14px; line-height: 1.65; color: #464e3d; border-top: 1px dashed #e6ebe0; padding-top: 14px; }
.fp-faq-a-in strong { color: var(--fp-olive); }

.fp-cta-card {
  margin-top: 84px; padding: 50px 28px; text-align: center; border-radius: 26px;
  background: linear-gradient(135deg, rgba(252, 228, 236, .68) 0%, rgba(255, 255, 255, .92) 48%, rgba(233, 241, 210, .82) 100%);
  border: 1px solid rgba(111, 154, 12, .22);
  backdrop-filter: blur(14px); -webkit-backdrop-filter: blur(14px);
  box-shadow: 0 24px 48px rgba(45, 65, 15, .08);
}
.fp-cta-banner {
  position: relative; width: 100%; height: clamp(190px, 26vw, 270px); border-radius: 20px; overflow: hidden;
  margin-bottom: 26px; border: 2px solid rgba(255, 255, 255, .95); box-shadow: 0 14px 34px rgba(45, 65, 15, .1);
}
.fp-cta-banner img { width: 100%; height: 100%; object-fit: cover; object-position: center 52%; display: block; }
.fp-cta-caption {
  position: absolute; left: 16px; bottom: 14px; padding: 5px 13px; border-radius: 999px;
  background: rgba(255, 255, 255, .88); backdrop-filter: blur(8px); font-size: 12px; font-weight: 600; color: #2c3a10;
}
.fp-cta-card h2 {
  margin: 16px 0 10px; font-family: "Noto Serif SC", serif;
  font-size: clamp(24px, 2.8vw, 38px); font-weight: 600; letter-spacing: -.02em;
}
.fp-cta-card p { font-size: 14.5px; color: #384030; margin-bottom: 24px; }
.fp-cta-btns { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }

/* ---------- 页脚 ---------- */
.fp-footer {
  background: var(--fp-lime); padding: 36px 4vw 24px;
  display: grid; grid-template-columns: 1fr auto 1fr; align-items: center; row-gap: 22px;
}
.fp-f-logo { display: inline-flex; align-items: center; gap: 12px; width: fit-content; }
.fp-f-logo-text { font-family: "Noto Serif SC", serif; font-weight: 700; font-size: 18px; color: #141a08; }
.fp-f-links { display: flex; gap: 24px; font-size: 13.5px; font-weight: 500; color: #1d240c; }
.fp-f-links a:hover { text-decoration: underline; color: #1d240c; }
.fp-f-support { justify-self: end; font-size: 13px; color: #1d240c; }
.fp-f-copy { grid-column: 1 / -1; text-align: center; font-size: 11.5px; color: #2a330f; }

/* ---------- 弹窗内容（配合全局 ModalDialog） ---------- */
.fp-modal-stack { display: flex; flex-direction: column; gap: 14px; }
.fp-modal-stack p { font-size: 13px; line-height: 1.7; color: #3e4437; }
.fp-modal-cover { border-radius: 18px; overflow: hidden; aspect-ratio: 16 / 7; }
.fp-modal-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.fp-modal-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.fp-modal-mentor-head { display: flex; align-items: center; gap: 14px; }
.fp-modal-mentor-head h3 { font-family: "Noto Serif SC", serif; font-size: 20px; font-weight: 600; }
.fp-modal-mentor-head h3 span { font-size: 13px; color: #6b7361; font-weight: 400; }
.fp-modal-mentor-head p { font-size: 12px; color: #6b7361; margin-top: 3px; }
.fp-modal-mentors { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; margin-top: 6px; }
.fp-modal-mentor { padding: 12px 14px; border-radius: 12px; background: var(--fp-cream); border: 1px solid var(--fp-line); }
.fp-modal-mentor strong { display: block; font-size: 13.5px; }
.fp-modal-mentor span { font-size: 11px; color: #6b7361; }
.fp-modal-mentor p { font-size: 12px; color: #464e3d; margin: 4px 0; }
.fp-modal-mentor em { font-style: normal; font-size: 11px; color: var(--fp-olive); }
.fp-modal-article { font-size: 13.5px; line-height: 1.9; white-space: pre-wrap; color: #3e4437; }

/* ---------- 响应式 ---------- */
@media (max-width: 960px) {
  .fp-mentor-grid { grid-template-columns: repeat(2, 1fr); }
  .fp-msb { grid-template-columns: 1fr; gap: 18px; }
  .fp-notice-layout { grid-template-columns: 1fr; }
  .fp-notice-art { min-height: 300px; }
  .fp-nav { gap: 14px; }
  .fp-nav-links { gap: 12px; }
  .fp-nav-stage { display: none; }
}
@media (max-width: 760px) {
  .fp-nav { top: 12px; gap: 10px; padding: 5px 5px 5px 12px; }
  .fp-nav-links { display: none; }
  .fp-brand-text em { display: none; }
  .fp-hero { height: 100svh; min-height: 580px; }
  .fp-hero-copy { top: 18%; }
  .fp-cloud-l { width: 66vw; left: -16vw; top: 4%; }
  .fp-cloud-r { width: 62vw; right: -16vw; top: 8%; }
  .fp-hero-glass .fp-glass:nth-child(even) { display: none; }
  .fp-mentor-grid { grid-template-columns: 1fr; }
  .fp-tools { width: 100%; }
  .fp-search { min-width: 0; flex: 1; }
  .fp-rules { grid-template-columns: 1fr; }
  .fp-modal-grid, .fp-modal-mentors { grid-template-columns: 1fr; }

  /* 流程区在移动端改为纵向堆叠 */
  .fp-process { height: auto; min-height: 0; padding: 96px 0 40px; }
  .fp-loop-head { position: relative; top: auto; }
  .fp-loop-cloud { width: 58vw; left: -14vw; top: 1vh; opacity: .75; }
  .fp-track { position: relative; inset: auto; flex-direction: column; margin-top: 36px; }
  .fp-panel { flex: none; height: auto; overflow: visible; border-right: none; border-top: 1px solid var(--fp-line); cursor: auto; }
  .fp-panel-in { position: relative; left: auto; bottom: auto; width: auto; padding: 26px 20px; gap: 18px; align-items: flex-start; }
  .fp-num { font-size: 96px; line-height: .85; }
  .fp-step { opacity: 1 !important; transform: none !important; filter: none !important; width: auto; }
  .fp-mini-cloud { display: none; }
  .fp-panel .fp-panel-flora { opacity: .5; width: 100px; }

  .fp-notice-item { flex-direction: column; align-items: flex-start; }
  .fp-n-summary { white-space: normal; }
  .fp-n-read { align-self: flex-end; }
  .fp-footer { grid-template-columns: 1fr; justify-items: center; text-align: center; }
  .fp-f-support { justify-self: center; }
  .fp-f-links { flex-wrap: wrap; justify-content: center; gap: 16px; }
}
@media (prefers-reduced-motion: reduce) {
  .fp-drift-a, .fp-drift-b, .fp-sway-l, .fp-sway-r, .fp-petal, .fp-mini-cloud, .fp-glass { animation: none !important; }
}
</style>
