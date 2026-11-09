<script setup>
import { computed, onMounted, ref } from 'vue'
import { publicApi } from '../../api'
import { useAuthStore } from '../../stores/auth'
import { notify } from '../../composables/useToast'
import { assignmentSource, batchStatus, publicityStatus, meta as resolveMeta } from '../../utils/labels'
import { fmt, monthDay, quotaPercent } from '../../utils/format'
import Icon from '../../components/Icon.vue'
import StatusPill from '../../components/StatusPill.vue'
import ModalDialog from '../../components/ModalDialog.vue'
import EmptyState from '../../components/EmptyState.vue'

const auth = useAuthStore()
const loading = ref(true)
const error = ref('')
const home = ref(null)
const activeLab = ref(null)
const activeAnnouncement = ref(null)

const batch = computed(() => home.value?.batch || {})
const labs = computed(() => home.value?.labs || [])
const stages = computed(() => home.value?.stages || [])
const announcements = computed(() => home.value?.announcements || [])
const assignments = computed(() => home.value?.publicAssignments || [])
const stats = computed(() => home.value?.stats || {})
const statusMeta = computed(() => resolveMeta(batchStatus, batch.value.status))

async function load() {
  loading.value = true
  error.value = ''
  try {
    home.value = await publicApi.home()
  } catch (err) {
    error.value = err.message
    notify(err.message, 'error')
  } finally {
    loading.value = false
  }
}

onMounted(load)

function ctaRoute() {
  return auth.isLoggedIn ? auth.homePath : '/login'
}
</script>

<template>
  <div class="public-page">
    <nav class="public-nav">
      <div class="public-nav-inner">
        <router-link to="/" class="brand" style="padding: 0">
          <span class="brand-mark">选</span>
          <span class="brand-text">
            <strong>实训室进组选拔系统</strong>
            <span>Lab Selection</span>
          </span>
        </router-link>
        <div class="public-nav-links">
          <a href="#process">选拔流程</a>
          <a href="#labs">实训室</a>
          <a href="#news">通知公告</a>
          <a v-if="assignments.length" href="#results">公示结果</a>
        </div>
        <div class="spacer" />
        <router-link v-if="auth.isLoggedIn" :to="auth.homePath" class="btn btn-primary btn-sm">
          进入工作台 <Icon name="arrow" :size="14" />
        </router-link>
        <router-link v-else to="/login" class="btn btn-primary btn-sm">
          登录 <Icon name="arrow" :size="14" />
        </router-link>
      </div>
    </nav>

    <main class="public-main">
      <div v-if="error" class="notice notice--danger">
        <Icon name="alert" :size="16" />{{ error }}
        <button class="link-btn" style="margin-left: auto" @click="load">重试</button>
      </div>

      <template v-if="loading">
        <div class="hero" style="min-height: 300px">
          <div class="skeleton" style="height: 22px; width: 210px; background: rgba(255,255,255,0.18)" />
          <div class="skeleton mt-16" style="height: 46px; width: min(560px, 80%); background: rgba(255,255,255,0.16)" />
          <div class="skeleton mt-12" style="height: 16px; width: min(430px, 70%); background: rgba(255,255,255,0.12)" />
        </div>
      </template>

      <template v-else-if="home">
        <section class="hero">
          <div style="position: relative; z-index: 1">
            <div class="flex flex-wrap" style="gap: 9px">
              <span class="hero-badge"><Icon name="calendar" :size="14" /> {{ batch.name }}</span>
              <span class="hero-badge">{{ statusMeta.label }}</span>
            </div>
            <h1 class="mt-16">在真实的项目里<br />找到属于你的方向</h1>
            <p>{{ batch.description || '学院面向指定年级学生开展实训室、项目组或导师团队进组选拔。' }}</p>
            <div class="hero-actions">
              <router-link :to="ctaRoute()" class="btn btn-lg" style="background: #fff; color: var(--primary-dark)">
                {{ auth.isLoggedIn ? '进入我的工作台' : '登录参与选拔' }}
                <Icon name="arrow" :size="16" />
              </router-link>
              <a href="#process" class="btn btn-ghost btn-lg">查看选拔流程</a>
            </div>
            <div class="hero-stats">
              <div class="hero-stat"><strong>{{ stats.labCount ?? labs.length }}</strong><span>实训室</span></div>
              <div class="hero-stat"><strong>{{ stats.mentorCount ?? '—' }}</strong><span>导师</span></div>
              <div class="hero-stat"><strong>{{ stats.totalQuota ?? '—' }}</strong><span>招生名额</span></div>
              <div class="hero-stat"><strong>{{ stats.admittedCount ?? 0 }}</strong><span>已录取</span></div>
            </div>
          </div>
        </section>

        <section id="process" class="section-block">
          <div class="section-block-head">
            <div>
              <h2>八步走完一次双向选择</h2>
              <p>线上记录流程与结果，线下开展宣讲、开放日与面试。</p>
            </div>
            <span class="pill pill--blue">{{ statusMeta.label }}</span>
          </div>
          <div class="process-grid">
            <div v-for="stage in stages" :key="stage.no" class="process-card" :class="stage.state">
              <div class="process-no">STEP {{ String(stage.no).padStart(2, '0') }}</div>
              <div class="process-title">{{ stage.title }}</div>
              <div class="process-desc">{{ stage.desc }}</div>
              <div class="process-time">{{ stage.time }}</div>
            </div>
          </div>
        </section>

        <section id="labs" class="section-block">
          <div class="section-block-head">
            <div>
              <h2>实训室与导师</h2>
              <p>了解研究方向、培养方式与名额，选择真正适合你的团队。</p>
            </div>
            <span class="small muted">{{ labs.length }} 个实训室</span>
          </div>
          <div class="lab-grid">
            <article v-for="lab in labs" :key="lab.id" class="card lab-card card-hover">
              <div class="lab-top">
                <div>
                  <div class="lab-name">{{ lab.name }}</div>
                  <div class="lab-code">{{ lab.code }}</div>
                </div>
                <span class="pill" :class="lab.remainingQuota > 0 ? 'pill--green' : 'pill--gray'">
                  {{ lab.remainingQuota > 0 ? `余 ${lab.remainingQuota}` : '名额已满' }}
                </span>
              </div>
              <div class="lab-desc">{{ lab.direction }}</div>
              <div class="lab-meta">
                <span><Icon name="users" :size="13" /> {{ lab.mentorCount }} 位导师</span>
                <span><Icon name="award" :size="13" /> 总名额 {{ lab.totalQuota }}</span>
                <span v-if="lab.applicantCount"><Icon name="form" :size="13" /> {{ lab.applicantCount }} 人报名</span>
              </div>
              <div>
                <div class="flex" style="justify-content: space-between; font-size: 11px; color: var(--muted-2)">
                  <span>名额使用</span><span>{{ lab.confirmedCount || 0 }} / {{ lab.totalQuota }}</span>
                </div>
                <div class="quota-bar mt-4">
                  <span :style="{ width: `${quotaPercent(lab.confirmedCount || 0, lab.totalQuota)}%` }" />
                </div>
              </div>
              <div class="mentor-list">
                <div v-for="mentor in (lab.mentors || []).slice(0, 3)" :key="mentor.id" class="mentor-row">
                  <span><span class="name">{{ mentor.name }}</span> <span class="muted tiny">{{ mentor.title }}</span></span>
                  <span class="tiny muted">{{ mentor.researchDirection }}</span>
                </div>
              </div>
              <button class="btn btn-soft btn-sm btn-block" type="button" @click="activeLab = lab">
                查看实训室详情 <Icon name="arrow" :size="13" />
              </button>
            </article>
          </div>
        </section>

        <section id="news" class="grid-2">
          <div class="card card-pad">
            <div class="section-head">
              <div class="section-title">通知公告</div>
              <span class="pill pill--blue">{{ announcements.length }} 条</span>
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
              <div class="notice notice--info" style="background: #f3f0ff; border-color: #e1d9ff; color: #5a42c9">
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
      学生实训室进组选拔系统 · 双向选择 · 分轮录取 · 统筹调剂
      <span style="margin: 0 8px">|</span> 数据仅用于教学演示
    </footer>

    <ModalDialog :open="Boolean(activeLab)" :title="activeLab?.name || ''" :subtitle="activeLab?.code" wide @close="activeLab = null">
      <div v-if="activeLab" class="stack">
        <div class="grid-2">
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
    </ModalDialog>

    <ModalDialog :open="Boolean(activeAnnouncement)" :title="activeAnnouncement?.title || ''" :subtitle="fmt(activeAnnouncement?.publishStart)" @close="activeAnnouncement = null">
      <p class="small" style="white-space: pre-wrap; line-height: 1.9">{{ activeAnnouncement?.content }}</p>
    </ModalDialog>
  </div>
</template>
