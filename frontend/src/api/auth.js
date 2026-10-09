import http from './http'

/**
 * 鉴权 API
 */

// 登录: POST /api/auth/login
export function login(data) {
  return http.post('/auth/login', data)
}

// 公共活动列表 (用于首页总览, 不需要登录态调学生端接口)
// 实际由学生端接口承担, 但首页展示也走同一接口, 由后端拦截器控制
export function publicActivities(params) {
  return http.get('/student/activities', { params })
}
