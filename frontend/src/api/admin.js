import http from './http'

/**
 * 管理员端 API (V2.0 新增)
 */

// 用户列表: GET /api/admin/users
export function pageUsers(params) {
  return http.get('/admin/users', { params })
}

// 停用账号: POST /api/admin/users/{account}/disable
export function disableUser(account) {
  return http.post(`/admin/users/${account}/disable`)
}

// 启用账号: POST /api/admin/users/{account}/enable
export function enableUser(account) {
  return http.post(`/admin/users/${account}/enable`)
}
