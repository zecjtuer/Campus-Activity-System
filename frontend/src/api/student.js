import http from './http'

/**
 * 学生端 API (V2.0)
 */

// 活动分页搜索: GET /api/student/activities
export function pageActivities(params) {
  return http.get('/student/activities', { params })
}

// 活动详情: GET /api/student/activities/{id}
export function getActivityDetail(id) {
  return http.get(`/student/activities/${id}`)
}

// 报名: POST /api/student/activities/{id}/register  (V2.0 三分支, 可能候补)
export function registerActivity(id) {
  return http.post(`/student/activities/${id}/register`)
}

// 退选: POST /api/student/activities/{id}/withdraw  (V2.0 含同事务递补)
export function withdrawActivity(id) {
  return http.post(`/student/activities/${id}/withdraw`)
}

// 我的报名记录: GET /api/student/registrations  (V2.0 四态)
export function myRegistrations(params) {
  return http.get('/student/registrations', { params })
}

// ---------- V2.0 站内消息 ----------

// 未读消息数
export function unreadCount() {
  return http.get('/student/notifications/unread-count')
}

// 消息列表
export function pageNotifications(params) {
  return http.get('/student/notifications', { params })
}

// 单条已读
export function markNotificationRead(id) {
  return http.post(`/student/notifications/${id}/read`)
}

// 全部已读
export function markAllNotificationsRead() {
  return http.post('/student/notifications/read-all')
}
