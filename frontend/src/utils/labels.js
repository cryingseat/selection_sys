export const batchStatus = {
  DRAFT: { label: '草稿', tone: 'gray' },
  ANNOUNCED: { label: '宣讲与开放日', tone: 'blue' },
  INTENT_OPEN: { label: '意向填报中', tone: 'blue' },
  INTENT_CLOSED: { label: '填报已截止', tone: 'gray' },
  INTERVIEW: { label: '面试与初选', tone: 'violet' },
  CONFIRM: { label: '拟录取确认', tone: 'orange' },
  SUPPLEMENT: { label: '分轮补录', tone: 'amber' },
  ADJUSTMENT: { label: '统筹调剂', tone: 'teal' },
  PUBLICITY: { label: '结果公示', tone: 'green' },
  FINISHED: { label: '已完成', tone: 'gray' }
}

export const applicationStatus = {
  DRAFT: { label: '草稿', tone: 'gray' },
  SUBMITTED: { label: '已提交', tone: 'blue' },
  WITHDRAWN: { label: '已撤回', tone: 'gray' },
  LOCKED: { label: '已锁定', tone: 'teal' }
}

export const intentStatus = {
  PENDING: { label: '待处理', tone: 'gray' },
  OFFERED: { label: '拟录取', tone: 'blue' },
  WAITLISTED: { label: '候补', tone: 'amber' },
  REJECTED: { label: '未录取', tone: 'red' },
  CONFIRMED: { label: '已确认', tone: 'green' },
  RELEASED: { label: '已释放', tone: 'gray' },
  DECLINED: { label: '已放弃', tone: 'red' }
}

export const offerStatus = {
  OFFERED: { label: '待确认', tone: 'orange' },
  CONFIRMED: { label: '已确认', tone: 'green' },
  DECLINED: { label: '已放弃', tone: 'red' },
  RELEASED: { label: '已释放', tone: 'gray' },
  EXPIRED: { label: '已逾期', tone: 'red' }
}

export const offerSource = {
  INITIAL: { label: '首轮拟录取', tone: 'blue' },
  SUPPLEMENT: { label: '补录', tone: 'violet' },
  PROMOTION: { label: '候补递补', tone: 'amber' },
  ADJUSTMENT: { label: '调剂', tone: 'teal' }
}

export const interviewConclusion = {
  PENDING: { label: '待考核', tone: 'gray' },
  PASS: { label: '通过', tone: 'green' },
  FAIL: { label: '不通过', tone: 'red' }
}

export const interviewStatus = {
  SCHEDULED: { label: '待面试', tone: 'amber' },
  COMPLETED: { label: '已完成', tone: 'green' },
  CANCELED: { label: '已取消', tone: 'gray' }
}

export const adjustmentPreference = {
  INTERNAL: { label: '接受实训室内调剂', tone: 'blue' },
  CROSS: { label: '接受跨实训室调剂', tone: 'teal' },
  NONE: { label: '不接受调剂', tone: 'gray' }
}

export const supplementStatus = {
  SCHEDULED: { label: '未开始', tone: 'gray' },
  OPEN: { label: '进行中', tone: 'green' },
  CLOSED: { label: '已结束', tone: 'gray' }
}

export const supplementIntentStatus = {
  PENDING: { label: '待处理', tone: 'gray' },
  OFFERED: { label: '拟录取', tone: 'blue' },
  WAITLISTED: { label: '候补', tone: 'amber' },
  REJECTED: { label: '未录取', tone: 'red' },
  CONFIRMED: { label: '已确认', tone: 'green' },
  DECLINED: { label: '已放弃', tone: 'red' }
}

export const publicityStatus = {
  DRAFT: { label: '草稿', tone: 'gray' },
  PUBLISHED: { label: '公示中', tone: 'green' },
  CLOSED: { label: '已结束', tone: 'gray' },
  REVOKED: { label: '已撤回', tone: 'red' }
}

export const announcementStatus = {
  DRAFT: { label: '草稿', tone: 'gray' },
  PUBLISHED: { label: '已发布', tone: 'green' },
  CLOSED: { label: '已关闭', tone: 'gray' }
}

export const announcementType = {
  GENERAL: { label: '综合通知', tone: 'blue' },
  LECTURE: { label: '集中宣讲', tone: 'violet' },
  OPEN_DAY: { label: '开放日', tone: 'teal' },
  INTERVIEW: { label: '面试安排', tone: 'orange' },
  SUPPLEMENT: { label: '补录通知', tone: 'amber' },
  PUBLICITY: { label: '公示通知', tone: 'green' }
}

export const assignmentSource = {
  CONFIRM: { label: '首轮确认', tone: 'blue' },
  SUPPLEMENT: { label: '补录确认', tone: 'violet' },
  PROMOTION: { label: '候补递补', tone: 'amber' },
  INTERNAL_ADJUST: { label: '实训室内调剂', tone: 'teal' },
  CROSS_ADJUST: { label: '跨实训室调剂', tone: 'teal' },
  TALK: { label: '谈话安置', tone: 'gray' }
}

export const assignmentStatus = {
  CONFIRMED: { label: '已确认', tone: 'green' },
  PUBLICIZED: { label: '已公示', tone: 'blue' },
  LOCKED: { label: '已锁定', tone: 'gray' },
  REVOKED: { label: '已撤回', tone: 'red' }
}

export const roleLabel = {
  STUDENT: '学生',
  MENTOR: '导师',
  ADMIN: '学院管理员'
}

export const reviewLevel = {
  ERROR: { label: '必须处理', tone: 'red' },
  WARN: { label: '提醒', tone: 'orange' },
  INFO: { label: '提示', tone: 'blue' }
}

export function meta(map, value) {
  return map[value] || { label: value || '—', tone: 'gray' }
}
