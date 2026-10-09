import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// axios 实例: 统一 baseURL、超时、请求/响应拦截器
const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器: 自动附加 Authorization: Bearer <token>
http.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器: 拆解统一返回体 {code, message, data}
// 业务错误 (code != 200) 弹提示并 reject; 401 跳登录
http.interceptors.response.use(
  (response) => {
    const result = response.data
    // 非标准返回体 (如直接下载二进制) 直接放行
    if (!result || typeof result.code === 'undefined') {
      return result
    }
    if (result.code === 200) {
      return result.data
    }
    // 业务错误
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(result)
  },
  (error) => {
    const status = error.response?.status
    const data = error.response?.data

    // 401 未登录 / Token 失效 → 清登录态跳 /login
    if (status === 401 || data?.code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.warning(data?.message || '登录已失效, 请重新登录')
      router.push({
        name: 'Login',
        query: { redirect: router.currentRoute.value.fullPath }
      })
      return Promise.reject(error)
    }

    // 403 越权
    if (status === 403 || data?.code === 403) {
      ElMessage.error(data?.message || '无权限访问')
      return Promise.reject(error)
    }

    // 400 参数错误 (统一返回体已带 message)
    if (data?.code && data?.message) {
      ElMessage.error(data.message)
      return Promise.reject(error)
    }

    // 网络或服务器错误
    ElMessage.error(error.message || '网络异常, 请稍后重试')
    return Promise.reject(error)
  }
)

export default http
