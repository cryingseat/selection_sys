import { defineStore } from 'pinia'
import { authApi } from '../api'
import { TOKEN_KEY, USER_KEY } from '../api/client'

function readUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: readUser(),
    loading: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token && state.user),
    role: (state) => state.user?.role || '',
    displayName: (state) => state.user?.displayName || '未登录',
    profile: (state) => state.user?.profile || null,
    homePath: (state) => {
      if (state.user?.role === 'STUDENT') return '/student'
      if (state.user?.role === 'MENTOR') return '/mentor'
      if (state.user?.role === 'ADMIN') return '/admin'
      return '/'
    }
  },
  actions: {
    setSession(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem(TOKEN_KEY, token)
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
    async login(account, password) {
      this.loading = true
      try {
        const data = await authApi.login({ account, password })
        this.setSession(data.token, data.user)
        return data.user
      } finally {
        this.loading = false
      }
    },
    async fetchMe() {
      if (!this.token) return null
      const user = await authApi.me()
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
      return user
    },
    async logout() {
      try {
        if (this.token) await authApi.logout()
      } catch {
        // 即使服务端令牌已失效，前端也继续清理本地登录态
      } finally {
        this.clear()
      }
    },
    clear() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      // 重新登录后允许再次执行引导弹窗检测
      sessionStorage.removeItem('selection_guide_checked')
    }
  }
})
