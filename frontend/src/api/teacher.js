import http from './http'

/**
 * 教师端 API (V2.0)
 */

// 发布活动: POST /api/teacher/activities
export function publishActivity(data) {
  return http.post('/teacher/activities', data)
}

// 本人活动列表: GET /api/teacher/activities
export function pageMyActivities(params) {
  return http.get('/teacher/activities', { params })
}

// 修改活动: PUT /api/teacher/activities/{id}
export function updateActivity(id, data) {
  return http.put(`/teacher/activities/${id}`, data)
}

// 删除活动: DELETE /api/teacher/activities/{id}
export function deleteActivity(id) {
  return http.delete(`/teacher/activities/${id}`)
}

// 报名统计: GET /api/teacher/activities/{id}/stats
export function activityStats(id) {
  return http.get(`/teacher/activities/${id}/stats`)
}

// ---------- V2.0 候补队列 / 名单导出 ----------

// 候补队列: GET /api/teacher/activities/{id}/waiting
export function waitingQueue(id, params) {
  return http.get(`/teacher/activities/${id}/waiting`, { params })
}

// 导出报名名单 (CSV, 返回 blob)
export function exportRoster(id) {
  return http.get(`/teacher/activities/${id}/export`, {
    responseType: 'blob'
  })
}

// ---------- V2.0 站内消息 ----------

export function unreadCount() {
  return http.get('/teacher/notifications/unread-count')
}

export function pageNotifications(params) {
  return http.get('/teacher/notifications', { params })
}

export function markNotificationRead(id) {
  return http.post(`/teacher/notifications/${id}/read`)
}

export function markAllNotificationsRead() {
  return http.post('/teacher/notifications/read-all')
}
