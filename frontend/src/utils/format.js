export function fmt(value, fallback = '—') {
  if (!value) return fallback
  return String(value).replace('T', ' ').slice(0, 16)
}

export function fmtDate(value, fallback = '—') {
  if (!value) return fallback
  return String(value).slice(0, 10)
}

export function fmtTime(value, fallback = '—') {
  if (!value) return fallback
  const text = String(value).replace('T', ' ')
  return text.slice(11, 16) || fallback
}

export function monthDay(value) {
  if (!value) return { day: '--', month: '待定' }
  const text = String(value)
  return { day: text.slice(8, 10), month: `${text.slice(5, 7)}月` }
}

export function toInput(value) {
  if (!value) return ''
  return String(value).replace(' ', 'T').slice(0, 16)
}

export function fromInput(value) {
  if (!value) return null
  let text = String(value).replace('T', ' ')
  if (text.length === 16) text += ':00'
  return text
}

export function initials(name) {
  if (!name) return '?'
  return String(name).slice(-2)
}

export function quotaPercent(used, total) {
  if (!total || total <= 0) return 0
  return Math.min(100, Math.round((used / total) * 100))
}

export function relativeDays(value) {
  if (!value) return ''
  const target = new Date(String(value).replace(' ', 'T'))
  if (Number.isNaN(target.getTime())) return ''
  const diff = Math.ceil((target.getTime() - Date.now()) / 86400000)
  if (diff === 0) return '今天'
  if (diff > 0) return `${diff} 天后`
  return `已过 ${Math.abs(diff)} 天`
}
