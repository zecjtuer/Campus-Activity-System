<template>
  <div class="noti-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 站内消息</span>
      </div>
      <div class="header-right">
        <el-button text @click="goBack">返回</el-button>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <main class="page-container">
      <div class="title-bar">
        <h2 class="section-title">{{ roleLabel }}消息</h2>
        <el-space>
          <el-checkbox v-model="onlyUnread" @change="loadData">仅未读</el-checkbox>
          <el-button type="primary" :disabled="total === 0" @click="handleMarkAllRead">
            全部已读
          </el-button>
        </el-space>
      </div>

      <div v-loading="loading" class="noti-list">
        <el-empty v-if="!loading && records.length === 0" description="暂无消息" />

        <el-card
          v-for="n in records"
          :key="n.id"
          class="noti-item"
          :class="{ 'not-read': n.isRead === 0 }"
          shadow="hover"
          @click="handleRead(n)"
        >
          <div class="noti-row">
            <el-tag :type="typeTag(n.type)" size="small">{{ typeLabel(n.type) }}</el-tag>
            <el-text v-if="n.isRead === 0" type="danger" size="small">未读</el-text>
            <el-text v-else type="info" size="small">已读</el-text>
          </div>
          <div class="noti-content">{{ n.content }}</div>
          <div class="noti-meta">
            <el-text size="small" type="info">
              {{ n.activityTitle ? `活动: ${n.activityTitle}` : '系统通知' }}
            </el-text>
            <el-text size="small" type="info">{{ formatTime(n.createdAt) }}</el-text>
          </div>
        </el-card>
      </div>

      <el-pagination
        class="pager"
        v-model:current-page="query.current"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import * as studentApi from '@/api/student'
import * as teacherApi from '@/api/teacher'

const router = useRouter()
const userStore = useUserStore()

// 按角色选择 API
const api = computed(() => userStore.isTeacher ? teacherApi : studentApi)

const loading = ref(false)
const records = ref([])
const total = ref(0)
const onlyUnread = ref(false)
const query = reactive({ current: 1, size: 10 })

const roleLabel = computed(() => userStore.isTeacher ? '教师' : '学生')

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function typeLabel(type) {
  return {
    ENROLLED: '报名成功',
    CANCELLED: '取消报名',
    PROMOTED: '候补递补',
    ACTIVITY_CANCELLED: '活动取消',
    ACTIVITY_UPDATED: '活动变更'
  }[type] || type
}

function typeTag(type) {
  switch (type) {
    case 'ENROLLED': return 'success'
    case 'CANCELLED': return 'warning'
    case 'PROMOTED': return 'primary'
    case 'ACTIVITY_CANCELLED': return 'danger'
    case 'ACTIVITY_UPDATED': return 'info'
    default: return 'info'
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await api.value.pageNotifications({
      current: query.current,
      size: query.size,
      onlyUnread: onlyUnread.value || undefined
    })
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function handleRead(n) {
  if (n.isRead === 1) return
  try {
    await api.value.markNotificationRead(n.id)
    n.isRead = 1
  } catch (e) {
    // ignore
  }
}

async function handleMarkAllRead() {
  try {
    await api.value.markAllNotificationsRead()
    ElMessage.success('已全部标记为已读')
    loadData()
  } catch (e) {
    // ignore
  }
}

function goBack() {
  if (userStore.isTeacher) router.push({ name: 'Teacher' })
  else if (userStore.isAdmin) router.push({ name: 'Admin' })
  else router.push({ name: 'Student' })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

onMounted(loadData)
</script>

<style scoped>
.noti-wrap {
  min-height: 100vh;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 60px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.brand .logo {
  width: 28px;
  height: 28px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.noti-list {
  min-height: 200px;
}

.noti-item {
  margin-bottom: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.noti-item.not-read {
  border-left: 3px solid #409eff;
}

.noti-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.noti-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 8px;
}

.noti-meta {
  display: flex;
  justify-content: space-between;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
