import client from './client'

export const authApi = {
  login: (payload) => client.post('/auth/login', payload),
  me: () => client.get('/auth/me'),
  logout: () => client.post('/auth/logout')
}

export const publicApi = {
  colleges: () => client.get('/public/colleges'),
  home: () => client.get('/public/home'),
  labs: () => client.get('/public/labs'),
  lab: (id) => client.get(`/public/labs/${id}`),
  announcements: () => client.get('/public/announcements'),
  publicity: (batchId) => client.get('/public/publicity', { params: { batchId } })
}

export const studentApi = {
  dashboard: () => client.get('/student/dashboard'),
  application: () => client.get('/student/application'),
  saveDraft: (payload) => client.post('/student/application/draft', payload),
  submit: (payload) => client.post('/student/application/submit', payload),
  withdraw: () => client.post('/student/application/withdraw'),
  interviews: () => client.get('/student/interviews'),
  offers: () => client.get('/student/offers'),
  confirmOffer: (id, reason) => client.post(`/student/offers/${id}/confirm`, null, { params: { reason } }),
  declineOffer: (id, reason) => client.post(`/student/offers/${id}/decline`, null, { params: { reason } }),
  declineAll: (reason) => client.post('/student/offers/decline-all', null, { params: { reason } }),
  result: () => client.get('/student/result'),
  history: () => client.get('/student/history'),
  supplement: () => client.get('/student/supplement'),
  applySupplement: (payload) => client.post('/student/supplement/intents', payload)
}

export const mentorApi = {
  dashboard: () => client.get('/mentor/dashboard'),
  applicants: (params) => client.get('/mentor/applicants', { params }),
  applicant: (applicationId) => client.get(`/mentor/applicants/${applicationId}`),
  interviews: () => client.get('/mentor/interviews'),
  saveInterview: (payload) => client.post('/mentor/interviews', payload),
  decisions: () => client.get('/mentor/decisions'),
  decide: (payload) => client.post('/mentor/decisions', payload),
  offers: () => client.get('/mentor/offers'),
  students: () => client.get('/mentor/students'),
  supplement: () => client.get('/mentor/supplement'),
  decideSupplement: (payload) => client.post('/mentor/supplement/decisions', payload)
}

export const adminApi = {
  dashboard: () => client.get('/admin/dashboard'),
  stateMachine: () => client.get('/admin/state-machine'),
  batches: () => client.get('/admin/batches'),
  batch: (id) => client.get(`/admin/batches/${id}`),
  createBatch: (payload) => client.post('/admin/batches', payload),
  updateBatch: (id, payload) => client.put(`/admin/batches/${id}`, payload),
  changeStage: (id, payload) => client.post(`/admin/batches/${id}/stage`, payload),
  review: (batchId) => client.get('/admin/review', { params: { batchId } }),
  autoConfirm: (batchId) => client.post('/admin/review/auto-confirm', null, { params: { batchId } }),
  expireOffers: (batchId, reason) => client.post('/admin/review/expire-offers', null, { params: { batchId, reason } }),
  applications: (params) => client.get('/admin/applications', { params }),
  application: (id) => client.get(`/admin/applications/${id}`),
  offers: (batchId) => client.get('/admin/offers', { params: { batchId } }),
  labs: () => client.get('/admin/labs'),
  createLab: (payload) => client.post('/admin/labs', payload),
  updateLab: (id, payload) => client.put(`/admin/labs/${id}`, payload),
  toggleLab: (id, enabled) => client.patch(`/admin/labs/${id}/status`, null, { params: { enabled } }),
  mentors: (params) => client.get('/admin/mentors', { params }),
  createMentor: (payload) => client.post('/admin/mentors', payload),
  updateMentor: (id, payload) => client.put(`/admin/mentors/${id}`, payload),
  toggleMentor: (id, enabled) => client.patch(`/admin/mentors/${id}/status`, null, { params: { enabled } }),
  resetMentorPassword: (id, password) => client.post(`/admin/mentors/${id}/password`, { password }),
  students: (params) => client.get('/admin/students', { params }),
  createStudent: (payload) => client.post('/admin/students', payload),
  updateStudent: (id, payload) => client.put(`/admin/students/${id}`, payload),
  toggleStudent: (id, enabled) => client.patch(`/admin/students/${id}/status`, null, { params: { enabled } }),
  resetStudentPassword: (id, password) => client.post(`/admin/students/${id}/password`, { password }),
  announcements: (batchId) => client.get('/admin/announcements', { params: { batchId } }),
  createAnnouncement: (payload) => client.post('/admin/announcements', payload),
  updateAnnouncement: (id, payload) => client.put(`/admin/announcements/${id}`, payload),
  publishAnnouncement: (id) => client.post(`/admin/announcements/${id}/publish`),
  deleteAnnouncement: (id) => client.delete(`/admin/announcements/${id}`),
  rounds: (batchId) => client.get('/admin/supplement/rounds', { params: { batchId } }),
  createRound: (payload) => client.post('/admin/supplement/rounds', payload),
  openRound: (id) => client.post(`/admin/supplement/rounds/${id}/open`),
  closeRound: (id) => client.post(`/admin/supplement/rounds/${id}/close`),
  promote: (id) => client.post(`/admin/supplement/rounds/${id}/promote`),
  waitlist: (batchId) => client.get('/admin/supplement/waitlist', { params: { batchId } }),
  adjustments: (batchId) => client.get('/admin/adjustments', { params: { batchId } }),
  adjustmentWorkspace: () => client.get('/admin/adjustments/workspace'),
  autoAdjust: () => client.post('/admin/adjustments/auto'),
  manualAdjust: (payload) => client.post('/admin/adjustments/manual', payload),
  talk: (studentId, reason) => client.post(`/admin/adjustments/${studentId}/talk`, null, { params: { reason } }),
  publicities: (batchId) => client.get('/admin/publicities', { params: { batchId } }),
  savePublicity: (payload) => client.post('/admin/publicities', payload),
  publicity: (id) => client.get(`/admin/publicities/${id}`),
  publishPublicity: (id) => client.post(`/admin/publicities/${id}/publish`),
  closePublicity: (id) => client.post(`/admin/publicities/${id}/close`),
  revokePublicity: (id, reason) => client.post(`/admin/publicities/${id}/revoke`, null, { params: { reason } }),
  auditLogs: (params) => client.get('/admin/audit-logs', { params })
}
