import { reactive } from 'vue'

export const toasts = reactive([])
let seq = 0

export function notify(message, type = 'info', duration = 3200) {
  const id = ++seq
  toasts.push({ id, message: String(message || ''), type })
  window.setTimeout(() => {
    const index = toasts.findIndex((item) => item.id === id)
    if (index >= 0) toasts.splice(index, 1)
  }, duration)
}

export function dismissToast(id) {
  const index = toasts.findIndex((item) => item.id === id)
  if (index >= 0) toasts.splice(index, 1)
}

export function useToast() {
  return {
    success: (message) => notify(message, 'success'),
    error: (message) => notify(message, 'error', 4200),
    warn: (message) => notify(message, 'warn'),
    info: (message) => notify(message, 'info')
  }
}
