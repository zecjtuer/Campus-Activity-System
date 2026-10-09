import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true }
  },
  {
    path: '/student',
    name: 'Student',
    component: () => import('@/views/Student.vue'),
    meta: { title: '学生端', requireAuth: true, role: 'STUDENT' }
  },
  {
    path: '/student/registrations',
    name: 'StudentRecord',
    component: () => import('@/views/StudentRecord.vue'),
    meta: { title: '我的报名', requireAuth: true, role: 'STUDENT' }
  },
  {
    path: '/teacher',
    name: 'Teacher',
    component: () => import('@/views/Teacher.vue'),
    meta: { title: '教师端', requireAuth: true, role: 'TEACHER' }
  },
  {
    // V2.0: 站内消息 (学生 + 教师共用组件, 按角色调不同 API)
    path: '/notifications',
    name: 'Notifications',
    component: () => import('@/views/Notifications.vue'),
    meta: { title: '站内消息', requireAuth: true }
  },
  {
    // V2.0: 管理员端
    path: '/admin',
    name: 'Admin',
    component: () => import('@/views/Admin.vue'),
    meta: { title: '管理员端', requireAuth: true, role: 'ADMIN' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { title: '页面不存在', public: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

// 路由守卫: 未登录跳 /login, 角色不匹配跳回首页
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 校园活动管理系统` : '校园活动管理系统'

  const userStore = useUserStore()

  if (to.meta.public) {
    if (to.name === 'Login' && userStore.isLoggedIn) {
      return next({ name: 'Home' })
    }
    return next()
  }

  if (to.meta.requireAuth && !userStore.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } })
  }

  if (to.meta.role && to.meta.role !== userStore.role) {
    return next({ name: 'Home' })
  }

  next()
})

export default router
