<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { publicApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, publicityStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, monthDay } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import ModalDialog from '../../components/ModalDialog.vue'
import EmptyState from '../../components/EmptyState.vue'

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

const batch = computed(() => home.value?.batch || {})
const labs = computed(() => home.value?.labs || [])
const colleges = computed(() => home.value?.colleges || [])
const announcements = computed(() => home.value?.announcements || [])
const assignments = computed(() => home.value?.publicAssignments || [])
const stats = computed(() => home.value?.stats || {})
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

async function load() {
  loading.value = true
  error.value = ''
  try {
    home.value = await publicApi.home()
    if (home.value.scopedCollegeId) activeCollege.value = home.value.scopedCollegeId
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

function tagTint(index) {
  const colors = ['#f2efe9', '#f4f4f5', '#f7f5f0', '#ede9e2']
  return colors[index % colors.length]
}
</script>

<template>
  <div class="public-page">
    <nav class="public-nav">
      <div class="public-nav-inner">
        <router-link to="/" class="brand" style="padding: 0">
          <span class="brand-mark">选</span>
          <span class="brand-text">
            <strong>实训室进组选拔</strong>
            <span>Open Studio</span>
          </span>
        </router-link>
        <div class="public-nav-links">
          <a href="#process">选拔流程</a>
          <a href="#labs">实训室</a>
          <a href="#news">通知公告</a>
          <a v-if="assignments.length" href="#results">公示结果</a>
        </div>
        <div class="spacer" />
        <span class="nav-stage-pill">
          <span class="dot" />
          {{ statusMeta.label }} · {{ fmt(batch.intentStart, '待定') }}
        </span>
        <span v-if="auth.isLoggedIn" class="pill pill--gray">{{ auth.user?.collegeName || '全校' }}</span>
        <router-link :to="ctaRoute()" class="btn btn-primary btn-sm">
          {{ auth.isLoggedIn ? '进入工作台' : '登录' }} <Icon name="arrow" :size="14" />
        </router-link>
      </div>
    </nav>

    <main class="public-main">
      <div v-if="error" class="notice notice--danger">
        <Icon name="alert" :size="16" />{{ error }}
        <button class="link-btn" style="margin-left: auto" @click="load">重试</button>
      </div>

      <template v-if="loading">
        <div class="editorial-head">
          <div>
            <p class="kicker">CAMPUS STUDIO DIRECTORY · 2026 AUTUMN</p>
            <h1>走近真实空间，<br />选择你的同行导师</h1>
          </div>
          <div class="skeleton" style="height: 60px; width: 360px" />
        </div>
        <div class="studio-grid">
          <div v-for="i in 2" :key="i" class="studio-card"><div class="skeleton" style="height: 420px" /></div>
        </div>
      </template>

      <template v-else-if="home">
        <section class="editorial-head">
          <div>
            <p class="kicker">CAMPUS STUDIO DIRECTORY · 2026 AUTUMN</p>
            <h1>走近真实空间，<br />选择你的同行导师</h1>
          </div>
          <p class="editorial-intro">
            摒弃冗长履历表。先浏览各学院实训室的真实空间、器物环境与项目方向，再挑选至多
            <strong>3 位平行意向导师</strong>（不分先后）。<br />
            <span class="tiny">当前面向 {{ batch.gradeScope || '全校' }} 年级 · {{ batch.name }}</span>
          </p>
        </section>

        <section id="labs" class="gallery-toolbar">
          <div class="filter-pills">
            <button class="filter-pill" :class="{ active: activeCollege === 0 }" @click="setCollege(0)">
              全部学院 <span class="count">{{ labs.length }}</span>
            </button>
            <button
              v-for="college in visibleColleges"
              :key="college.id"
              class="filter-pill"
              :class="{ active: activeCollege === college.id }"
              @click="setCollege(college.id)"
            >
              <span class="dot" :style="{ backgroundColor: college.accentColor || '#71717a' }" />
              {{ college.name }} <span class="count">{{ collegeCount(college.id) }}</span>
            </button>
          </div>
          <div class="gallery-tools">
            <div class="search-pill">
              <Icon name="search" :size="14" />
              <input v-model="keyword" placeholder="搜索实训室、导师或方向标签..." />
              <button v-if="keyword" class="clear" type="button" @click="keyword = ''">×</button>
            </div>
            <button class="filter-pill" :class="{ active: onlyAvailable }" @click="onlyAvailable = !onlyAvailable">仅看有余量</button>
          </div>
        </section>

        <div v-if="!filteredLabs.length" class="studio-empty">
          <EmptyState title="未找到匹配的实训室或导师" desc="请尝试切换学院、清空搜索关键词，或关闭“仅看有余量”。" icon="search">
            <button class="btn btn-primary btn-sm" type="button" @click="resetFilters">重置全部筛选</button>
          </EmptyState>
        </div>

        <section v-else class="studio-grid">
          <article v-for="lab in filteredLabs" :key="lab.id" class="studio-card">
            <div class="studio-cover">
              <img v-if="lab.coverImage" :src="lab.coverImage" :alt="lab.name" />
              <div v-else class="cover-fallback">{{ lab.code }}</div>
              <div class="studio-badges">
                <div class="studio-badges-left">
                  <span class="badge-light">{{ lab.collegeName }}</span>
                  <span class="badge-dark">{{ lab.location || lab.contact || '校内实训空间' }}</span>
                </div>
                <span class="badge-light">{{ lab.remainingQuota > 0 ? `余 ${lab.remainingQuota}` : '名额已满' }}</span>
              </div>
              <div class="studio-title">
                <div>
                  <p>{{ lab.code }} · 开放日 {{ shortOpenDay() }}</p>
                  <h2>{{ lab.name }}</h2>
                </div>
                <span class="studio-quota">共招 {{ lab.totalQuota }} 人</span>
              </div>
            </div>

            <div class="studio-vibe">
              <p>“{{ lab.intro }}”</p>
              <div class="vibe-tags">
                <span v-for="(tag, index) in directionTags(lab)" :key="tag" :style="{ background: tagTint(index) }">{{ tag }}</span>
              </div>
            </div>

            <div class="studio-mentors">
              <div class="studio-mentors-head">
                <span>驻室导师（{{ (lab.mentors || []).length }}）· 点击卡片看详情</span>
                <span>平行意向不设先后</span>
              </div>
              <div class="mentor-mini-grid">
                <div
                  v-for="mentor in lab.mentors || []"
                  :key="mentor.id"
                  class="mentor-mini"
                  role="button"
                  tabindex="0"
                  @click="openMentor(mentor, lab)"
                  @keyup.enter="openMentor(mentor, lab)"
                >
                  <span class="mentor-avatar" :style="{ backgroundColor: lab.collegeAccent || '#3f3f46' }">{{ mentor.name.slice(0, 1) }}</span>
                  <span class="mentor-mini-body">
                    <strong>{{ mentor.name }} <span>{{ mentor.title }}</span></strong>
                    <em>招录 {{ mentor.quota }} 人 · {{ mentor.applicantCount || 0 }} 报</em>
                    <small>{{ mentor.researchDirection }}</small>
                  </span>
                  <span class="mentor-mini-action" @click.stop="mentorAction(mentor, lab)">+ 意向</span>
                </div>
              </div>
            </div>

            <div class="studio-actions">
              <button class="btn btn-ghost btn-sm" type="button" @click="activeLab = lab">查看实训室详情</button>
              <router-link :to="ctaRoute()" class="btn btn-soft btn-sm">
                {{ auth.isLoggedIn && auth.role === 'STUDENT' ? '去填报' : '登录参与' }}
              </router-link>
            </div>
          </article>
        </section>

        <section id="process" class="section-block">
          <div class="section-block-head">
            <div>
              <p class="kicker">SELECTION PROCESS</p>
              <h2>八步走完一次双向选择</h2>
              <p>线上记录流程与结果，线下开展宣讲、开放日与面试。</p>
            </div>
            <StatusPill :value="batch.status" :map="batchStatus" />
          </div>
          <div class="process-grid">
            <div v-for="stage in home.stages" :key="stage.no" class="process-card" :class="stage.state">
              <div class="process-no">STEP {{ String(stage.no).padStart(2, '0') }}</div>
              <div class="process-title">{{ stage.title }}</div>
              <div class="process-desc">{{ stage.desc }}</div>
              <div class="process-time">{{ stage.time }}</div>
            </div>
          </div>
        </section>

        <section id="news" class="grid-2">
          <div class="card card-pad">
            <div class="section-head">
              <div class="section-title">通知公告</div>
              <span class="pill pill--gray">{{ announcements.length }} 条</span>
            </div>
            <div v-if="announcements.length">
              <div v-for="item in announcements" :key="item.id" class="announcement-item" style="cursor: pointer" @click="activeAnnouncement = item">
                <div class="announcement-date">
                  <strong>{{ monthDay(item.publishStart).day }}</strong>
                  <span>{{ monthDay(item.publishStart).month }}</span>
                </div>
                <div class="announcement-body">
                  <h4>{{ item.title }}</h4>
                  <p>{{ item.content }}</p>
                </div>
              </div>
            </div>
            <EmptyState v-else title="暂无公告" desc="选拔相关通知会在这里发布。" icon="bell" />
          </div>

          <div class="card card-pad">
            <div class="section-head">
              <div class="section-title">选拔规则速览</div>
            </div>
            <div class="stack" style="gap: 12px">
              <div class="notice notice--info">
                <Icon name="form" :size="16" />
                <span>每名学生在同一批次最多填报 <strong>3 个平行意向</strong>，不设志愿先后，不能重复选择同一导师。</span>
              </div>
              <div class="notice notice--success">
                <Icon name="check" :size="16" />
                <span>一名学生可以收到多个拟录取意向，但只能在规定时间内 <strong>确认其中一个</strong>；确认后其他意向自动释放名额。</span>
              </div>
              <div class="notice notice--warn">
                <Icon name="clock" :size="16" />
                <span>逾期未确认视为放弃；放弃与逾期产生的空缺进入补录，按候补顺位依次递补。</span>
              </div>
              <div class="notice notice--info">
                <Icon name="shield" :size="16" />
                <span>调剂仅在学生授权范围内进行，优先实训室内部，其次跨实训室，最后安排谈话安置。</span>
              </div>
            </div>
          </div>
        </section>

        <section class="card card-pad">
          <div class="section-head">
            <div>
              <div class="section-title">常见问题</div>
              <p class="small muted mt-4">关于平行意向、确认、补录与调剂的常见疑问。</p>
            </div>
          </div>
          <div class="faq-list">
            <details class="faq-item">
              <summary>可以同时填报几位导师？</summary>
              <p>每名学生在同一批次最多填报 3 位导师，平行意向不设先后顺序，也不能重复选择同一位导师。</p>
            </details>
            <details class="faq-item">
              <summary>提交后还能修改吗？</summary>
              <p>在意向填报截止前可以保存草稿、修改并重新提交，也可以撤回报名；截止后系统自动锁定，不能再修改。</p>
            </details>
            <details class="faq-item">
              <summary>收到多个拟录取怎么办？</summary>
              <p>你可以在规定时间内自主选择其中一个并确认。确认后其他拟录取意向自动释放，名额回到补录池。</p>
            </details>
            <details class="faq-item">
              <summary>逾期没有确认会怎样？</summary>
              <p>超过确认截止时间未操作，视为放弃全部拟录取；空缺名额将按候补顺位进入补录。</p>
            </details>
            <details class="faq-item">
              <summary>调剂会把我分到不喜欢的实训室吗？</summary>
              <p>调剂只会在你授权的范围内进行。你可以在填报时明确拒绝某些实训室，系统不会将你分配到这些实训室。</p>
            </details>
            <details class="faq-item">
              <summary>公示后结果还能改吗？</summary>
              <p>公示发布后结果锁定。如确需更正，学院会撤回公示、修改后重新公示，所有操作都会留痕。</p>
            </details>
          </div>
        </section>

        <section v-if="assignments.length" id="results" class="card card-pad">
          <div class="section-head">
            <div>
              <div class="section-title">公示结果</div>
              <p class="small muted mt-4">{{ home.publicity?.title }}</p>
            </div>
            <StatusPill :value="home.publicity?.status || 'PUBLISHED'" :map="publicityStatus" />
          </div>
          <div class="table-wrap">
            <table class="table">
              <thead><tr><th>学生</th><th>专业</th><th>实训室</th><th>导师</th><th>录取方式</th></tr></thead>
              <tbody>
                <tr v-for="row in assignments" :key="row.id">
                  <td><strong>{{ row.studentName }}</strong><span class="muted small"> · {{ row.studentNo }}</span></td>
                  <td>{{ row.major }}</td>
                  <td>{{ row.labName }}</td>
                  <td>{{ row.mentorName }}</td>
                  <td><span class="pill pill--blue">{{ resolveMeta(assignmentSource, row.source).label }}</span></td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </main>

    <footer class="public-footer">
      Open Studio · 学生实训室进组选拔系统
      <span style="margin: 0 8px">|</span> 全校实训室与导师空间画廊
      <span style="margin: 0 8px">|</span> 数据仅用于教学演示
    </footer>

    <ModalDialog :open="Boolean(activeLab)" :title="activeLab?.name || ''" :subtitle="activeLab?.code" wide @close="activeLab = null">
      <div v-if="activeLab" class="stack">
        <div v-if="activeLab.coverImage" style="border-radius: 18px; overflow: hidden; aspect-ratio: 16/7">
          <img :src="activeLab.coverImage" :alt="activeLab.name" style="width: 100%; height: 100%; object-fit: cover" />
        </div>
        <div class="grid-2">
          <div><div class="label">所属学院</div><p class="small mt-4">{{ activeLab.collegeName }}</p></div>
          <div><div class="label">空间位置</div><p class="small mt-4">{{ activeLab.location || activeLab.contact || '—' }}</p></div>
          <div><div class="label">研究方向</div><p class="small mt-4">{{ activeLab.direction }}</p></div>
          <div><div class="label">联系方式</div><p class="small mt-4">{{ activeLab.contact || '—' }}</p></div>
        </div>
        <div><div class="label">实训室简介</div><p class="small mt-4">{{ activeLab.intro }}</p></div>
        <div><div class="label">培养安排</div><p class="small mt-4">{{ activeLab.trainingPlan }}</p></div>
        <div><div class="label">选拔要求</div><p class="small mt-4">{{ activeLab.requirements }}</p></div>
        <div>
          <div class="label">导师名单（{{ (activeLab.mentors || []).length }}）</div>
          <div class="grid-2 mt-8">
            <div v-for="mentor in activeLab.mentors || []" :key="mentor.id" class="offer-card">
              <div class="flex" style="justify-content: space-between">
                <strong>{{ mentor.name }}</strong><span class="pill pill--gray">{{ mentor.title }}</span>
              </div>
              <div class="small muted">{{ mentor.researchDirection }}</div>
              <div class="small">招生名额 <strong>{{ mentor.quota }}</strong> 人 · 已用 {{ mentor.confirmedCount || 0 }} 人</div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="activeLab = null">关闭</button>
        <router-link :to="ctaRoute()" class="btn btn-primary">去了解填报</router-link>
      </template>
    </ModalDialog>

    <ModalDialog :open="Boolean(activeMentor)" :title="activeMentor?.mentor?.name || ''" :subtitle="activeMentor?.lab?.name" @close="activeMentor = null">
      <div v-if="activeMentor" class="stack">
        <div class="flex">
          <span class="mentor-avatar" style="width: 48px; height: 48px; font-size: 18px" :style="{ backgroundColor: activeMentor.lab.collegeAccent || '#3f3f46' }">
            {{ activeMentor.mentor.name.slice(0, 1) }}
          </span>
          <div>
            <h3>{{ activeMentor.mentor.name }} <span class="muted small">{{ activeMentor.mentor.title }}</span></h3>
            <div class="small muted">{{ activeMentor.lab.collegeName }} · {{ activeMentor.lab.name }}</div>
          </div>
        </div>
        <div class="kv">
          <dt>研究方向</dt><dd>{{ activeMentor.mentor.researchDirection }}</dd>
          <dt>招生名额</dt><dd>{{ activeMentor.mentor.quota }} 人</dd>
          <dt>报名热度</dt><dd>{{ activeMentor.mentor.applicantCount || 0 }} 人</dd>
          <dt>空间位置</dt><dd>{{ activeMentor.lab.location || activeMentor.lab.contact || '—' }}</dd>
        </div>
        <p class="small muted">{{ activeMentor.mentor.profile }}</p>
        <div class="notice notice--info">
          <Icon name="spark" :size="16" />
          <span>平行意向不设先后，最多可选择 3 位导师。提交前请确认自己的兴趣方向与投入时间。</span>
        </div>
      </div>
      <template #footer>
        <button class="btn btn-ghost" type="button" @click="activeMentor = null">关闭</button>
        <router-link :to="ctaRoute()" class="btn btn-primary">{{ auth.isLoggedIn ? '去填报' : '登录后填报' }}</router-link>
      </template>
    </ModalDialog>

    <ModalDialog :open="Boolean(activeAnnouncement)" :title="activeAnnouncement?.title || ''" :subtitle="fmt(activeAnnouncement?.publishStart)" @close="activeAnnouncement = null">
      <p class="small" style="white-space: pre-wrap; line-height: 1.9">{{ activeAnnouncement?.content }}</p>
    </ModalDialog>
  </div>
</template>
