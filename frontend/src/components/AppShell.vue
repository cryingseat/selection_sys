<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { notify } from '../composables/useToast'
import Icon from './Icon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const groups = computed(() => {
  if (auth.role === 'STUDENT') {
    return [
      {
        label: '选拔流程',
        items: [
          { to: '/student', label: '我的选拔', icon: 'home', exact: true },
          { to: '/student/application', label: '意向填报', icon: 'form' },
          { to: '/student/offers', label: '拟录取确认', icon: 'check' },
          { to: '/student/supplement', label: '补录填报', icon: 'refresh' },
          { to: '/student/result', label: '我的结果', icon: 'award' }
        ]
      },
      {
        label: '了解项目',
        items: [{ to: '/student/labs', label: '实训室与导师', icon: 'flask' }]
      }
    ]
  }
  if (auth.role === 'MENTOR') {
    return [
      {
        label: '选拔工作',
        items: [
          { to: '/mentor', label: '导师工作台', icon: 'home', exact: true },
          { to: '/mentor/applicants', label: '报名学生', icon: 'users' },
          { to: '/mentor/interviews', label: '面试考核', icon: 'calendar' },
          { to: '/mentor/decisions', label: '初选建议', icon: 'check' }
        ]
      },
      {
        label: '结果管理',
        items: [
          { to: '/mentor/students', label: '我的学生', icon: 'award' },
          { to: '/mentor/supplement', label: '补录管理', icon: 'refresh' }
        ]
      }
    ]
  }
  return [
    {
      label: '总览',
      items: [
        { to: '/admin', label: '学院总览', icon: 'home', exact: true },
        { to: '/admin/batches', label: '选拔批次', icon: 'calendar' },
        { to: '/admin/review', label: '初选审核', icon: 'shield' }
      ]
    },
    {
      label: '基础数据',
      items: [
        { to: '/admin/labs', label: '实训室', icon: 'flask' },
        { to: '/admin/mentors', label: '导师', icon: 'users' },
        { to: '/admin/students', label: '学生', icon: 'book' }
      ]
    },
    {
      label: '录取流程',
      items: [
        { to: '/admin/applications', label: '报名数据', icon: 'form' },
        { to: '/admin/supplement', label: '分轮补录', icon: 'refresh' },
        { to: '/admin/adjustment', label: '统筹调剂', icon: 'target' },
        { to: '/admin/publicity', label: '结果公示', icon: 'award' }
      ]
    },
    {
      label: '其他',
      items: [
        { to: '/admin/announcements', label: '公告管理', icon: 'bell' },
        { to: '/admin/audit', label: '操作审计', icon: 'chart' }
      ]
    }
  ]
})

const roleLabel = computed(() => ({ STUDENT: '学生', MENTOR: '导师', ADMIN: '学院管理员' }[auth.role] || '访客'))
const currentTitle = computed(() => route.meta.title || '实训室进组选拔')
const avatarText = computed(() => (auth.displayName || '访客').slice(0, 1))

async function logout() {
  await auth.logout()
  notify('已退出登录', 'success')
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <router-link to="/" class="brand">
        <span class="brand-mark">选</span>
        <span class="brand-text">
          <strong>河北师范大学</strong>
          <span>实训室进组选拔系统</span>
        </span>
      </router-link>

      <div class="nav-scroll">
        <div v-for="group in groups" :key="group.label" class="nav-group">
          <div class="nav-label">{{ group.label }}</div>
          <router-link
            v-for="item in group.items"
            :key="item.to"
            :to="item.to"
            class="nav-link"
            :class="{ active: item.exact ? route.path === item.to : route.path.startsWith(item.to) }"
          >
            <Icon :name="item.icon" :size="17" />
            <span>{{ item.label }}</span>
          </router-link>
        </div>
      </div>

      <div class="sidebar-foot">
        <div class="flex" style="justify-content: space-between; gap: 8px">
          <span class="tag">所属学院</span>
          <strong class="tiny" style="text-align: right; color: var(--ink)">{{ auth.user?.collegeName || '—' }}</strong>
        </div>
        <div class="flex mt-4" style="justify-content: space-between">
          <span class="tag">当前身份</span>
          <strong class="tiny" style="color: var(--ink)">{{ roleLabel }}</strong>
        </div>
        <div class="tiny mt-8" style="color: var(--muted-2)">选拔流程公开透明 · 结果全程留痕</div>
      </div>
    </aside>

    <main class="main">
      <header class="topbar">
        <div class="topbar-left">
          <router-link to="/" class="btn btn-icon btn-ghost" title="返回门户">
            <Icon name="home" :size="16" />
          </router-link>
          <div>
            <div class="topbar-title">{{ currentTitle }}</div>
            <div class="topbar-sub">河北师范大学 · 学生实训室进组选拔系统</div>
          </div>
        </div>
        <div class="topbar-right">
          <span class="pill pill--gray">{{ auth.user?.collegeName || '全校' }}</span>
          <span class="pill pill--blue">{{ roleLabel }}</span>
          <div class="flex" style="gap: 8px">
            <div class="avatar">{{ avatarText }}</div>
            <div class="topbar-sub" style="line-height: 1.3">
              <strong style="display: block; color: var(--ink-2)">{{ auth.displayName }}</strong>
              <span>{{ auth.user?.account }}</span>
            </div>
          </div>
          <button class="btn btn-ghost btn-sm" type="button" @click="logout">
            <Icon name="logout" :size="14" /> 退出
          </button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.nav-scroll { flex: 1; overflow-y: auto; margin: 0 -4px; padding: 0 4px; }
.nav-scroll::-webkit-scrollbar { width: 4px; }
.nav-scroll::-webkit-scrollbar-thumb { background: var(--line-strong); border-radius: 4px; }
.tag { font-size: 11px; color: var(--muted); }
</style>
