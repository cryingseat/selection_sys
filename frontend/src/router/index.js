import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  {
    path: '/',
    name: 'public-home',
    component: () => import('../views/public/PublicHome.vue'),
    meta: { title: '选拔首页' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/public/LoginView.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/student',
    component: () => import('../components/AppShell.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      { path: '', name: 'student-home', component: () => import('../views/student/StudentDashboard.vue'), meta: { title: '我的选拔' } },
      { path: 'labs', name: 'student-labs', component: () => import('../views/student/StudentLabs.vue'), meta: { title: '实训室与导师' } },
      { path: 'application', name: 'student-application', component: () => import('../views/student/StudentApplication.vue'), meta: { title: '意向填报' } },
      { path: 'offers', name: 'student-offers', component: () => import('../views/student/StudentOffers.vue'), meta: { title: '拟录取确认' } },
      { path: 'supplement', name: 'student-supplement', component: () => import('../views/student/StudentSupplement.vue'), meta: { title: '补录填报' } },
      { path: 'result', name: 'student-result', component: () => import('../views/student/StudentResult.vue'), meta: { title: '我的结果' } }
    ]
  },
  {
    path: '/mentor',
    component: () => import('../components/AppShell.vue'),
    meta: { requiresAuth: true, role: 'MENTOR' },
    children: [
      { path: '', name: 'mentor-home', component: () => import('../views/mentor/MentorDashboard.vue'), meta: { title: '导师工作台' } },
      { path: 'applicants', name: 'mentor-applicants', component: () => import('../views/mentor/MentorApplicants.vue'), meta: { title: '报名学生' } },
      { path: 'interviews', name: 'mentor-interviews', component: () => import('../views/mentor/MentorInterviews.vue'), meta: { title: '面试考核' } },
      { path: 'decisions', name: 'mentor-decisions', component: () => import('../views/mentor/MentorDecisions.vue'), meta: { title: '初选建议' } },
      { path: 'students', name: 'mentor-students', component: () => import('../views/mentor/MentorStudents.vue'), meta: { title: '我的学生' } },
      { path: 'supplement', name: 'mentor-supplement', component: () => import('../views/mentor/MentorSupplement.vue'), meta: { title: '补录管理' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('../components/AppShell.vue'),
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', name: 'admin-home', component: () => import('../views/admin/AdminDashboard.vue'), meta: { title: '学院总览' } },
      { path: 'batches', name: 'admin-batches', component: () => import('../views/admin/AdminBatches.vue'), meta: { title: '选拔批次' } },
      { path: 'review', name: 'admin-review', component: () => import('../views/admin/AdminReview.vue'), meta: { title: '初选审核' } },
      { path: 'labs', name: 'admin-labs', component: () => import('../views/admin/AdminLabs.vue'), meta: { title: '实训室管理' } },
      { path: 'mentors', name: 'admin-mentors', component: () => import('../views/admin/AdminMentors.vue'), meta: { title: '导师管理' } },
      { path: 'students', name: 'admin-students', component: () => import('../views/admin/AdminStudents.vue'), meta: { title: '学生管理' } },
      { path: 'applications', name: 'admin-applications', component: () => import('../views/admin/AdminApplications.vue'), meta: { title: '报名数据' } },
      { path: 'supplement', name: 'admin-supplement', component: () => import('../views/admin/AdminSupplement.vue'), meta: { title: '分轮补录' } },
      { path: 'adjustment', name: 'admin-adjustment', component: () => import('../views/admin/AdminAdjustment.vue'), meta: { title: '统筹调剂' } },
      { path: 'publicity', name: 'admin-publicity', component: () => import('../views/admin/AdminPublicity.vue'), meta: { title: '结果公示' } },
      { path: 'announcements', name: 'admin-announcements', component: () => import('../views/admin/AdminAnnouncements.vue'), meta: { title: '公告管理' } },
      { path: 'audit', name: 'admin-audit', component: () => import('../views/admin/AdminAudit.vue'), meta: { title: '操作审计' } }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/public/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      auth.clear()
    }
  }
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.role && auth.user && auth.user.role !== to.meta.role) {
    return auth.homePath
  }
  if (to.name === 'login' && auth.token && auth.user) {
    return auth.homePath
  }
  document.title = to.meta.title ? `${to.meta.title} · 实训室进组选拔` : '实训室进组选拔系统'
  return true
})

export default router
