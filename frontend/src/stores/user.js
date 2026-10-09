import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi } from '@/api/auth'

/**
 * 用户 Store: 管理登录态、token、角色
 * V2.0: 新增 isAdmin
 */
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('campus_token') || '')
  const account = ref(localStorage.getItem('campus_account') || '')
  const name = ref(localStorage.getItem('campus_name') || '')
  const role = ref(localStorage.getItem('campus_role') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isStudent = computed(() => role.value === 'STUDENT')
  const isTeacher = computed(() => role.value === 'TEACHER')
  const isAdmin = computed(() => role.value === 'ADMIN')

  async function login(accountInput, password) {
    const data = await loginApi({ account: accountInput, password })
    token.value = data.token
    account.value = data.account
    name.value = data.name
    role.value = data.role

    localStorage.setItem('campus_token', data.token)
    localStorage.setItem('campus_account', data.account)
    localStorage.setItem('campus_name', data.name)
    localStorage.setItem('campus_role', data.role)
    return data
  }

  function logout() {
    token.value = ''
    account.value = ''
    name.value = ''
    role.value = ''
    localStorage.removeItem('campus_token')
    localStorage.removeItem('campus_account')
    localStorage.removeItem('campus_name')
    localStorage.removeItem('campus_role')
  }

  return {
    token, account, name, role,
    isLoggedIn, isStudent, isTeacher, isAdmin,
    login, logout
  }
})
