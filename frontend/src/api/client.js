import axios from 'axios'

export const TOKEN_KEY = 'selection_token'
export const USER_KEY = 'selection_user'

const client = axios.create({
  baseURL: '/api',
  timeout: 20000
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body?.data
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data
    let message = body?.message || error.message || '网络异常，请稍后重试'
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        message = '登录已过期，请重新登录'
        window.setTimeout(() => {
          window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
        }, 300)
      }
    } else if (status === 403) {
      message = body?.message || '没有权限执行该操作'
    }
    return Promise.reject(new Error(message))
  }
)

export default client
