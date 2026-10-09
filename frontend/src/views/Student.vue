<template>
  <div class="student-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 学生端</span>
      </div>
      <div class="header-right">
        <el-text>{{ userStore.name }} ({{ userStore.account }})</el-text>
        <el-badge :value="unread" :hidden="unread === 0" class="msg-badge">
          <el-button text @click="$router.push({ name: 'Notifications' })">
            <el-icon><Bell /></el-icon> 消息
          </el-button>
        </el-badge>
        <el-button text @click="$router.push({ name: 'StudentRecord' })">我的报名</el-button>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <main class="page-container">
      <h2 class="section-title">浏览活动</h2>

      <!-- 搜索框 -->
      <el-form :inline="true" class="search-bar" @submit.prevent="loadData">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="活动名称 / 地点 / 简介"
            clearable
            style="width: 260px"
            @keyup.enter="loadData"
            @clear="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 活动列表 -->
      <el-table v-loading="loading" :data="activityList" stripe style="width: 100%">
        <el-table-column prop="activityId" label="ID" width="70" />
        <el-table-column prop="title" label="活动名称" min-width="160" />
        <el-table-column prop="location" label="地点" width="140" />
        <el-table-column label="活动时间" width="170">
          <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column label="报名截止" width="170">
          <template #default="{ row }">
            <el-text :type="isDeadlineNear(row.signupDeadline) ? 'danger' : 'default'">
              {{ formatTime(row.signupDeadline) }}
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="限定年级" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.targetGrade" size="small" type="warning">{{ row.targetGrade }}</el-tag>
            <el-text v-else size="small" type="info">不限</el-text>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="140">
          <template #default="{ row }">
            <el-text :type="row.remaining === 0 ? 'danger' : 'success'">
              {{ row.enrolledCount }} / {{ row.maxParticipants }}
            </el-text>
            <el-text size="small" type="info"> (剩 {{ row.remaining }})</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button
              size="small"
              type="primary"
              :disabled="!canRegister(row)"
              @click="handleRegister(row)"
            >
              {{ row.remaining === 0 ? '候补' : '报名' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="活动详情" width="600px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="活动名称">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="活动时间">{{ formatTime(detail.activityTime) }}</el-descriptions-item>
        <el-descriptions-item label="地点">{{ detail.location }}</el-descriptions-item>
        <el-descriptions-item label="活动简介">{{ detail.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="发布教师">{{ detail.teacherAccount }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatTime(detail.publishTime) }}</el-descriptions-item>
        <el-descriptions-item label="限定年级">
          <el-tag v-if="detail.targetGrade" type="warning">{{ detail.targetGrade }}</el-tag>
          <el-text v-else type="info">不限</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="名额">
          <el-text type="success">已报 {{ detail.enrolledCount }}</el-text>
          <el-divider direction="vertical" />
          <el-text type="warning">剩余 {{ detail.remaining }}</el-text>
          <el-divider direction="vertical" />
          <el-text>上限 {{ detail.maxParticipants }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="报名截止">{{ formatTime(detail.signupDeadline) }}</el-descriptions-item>
        <el-descriptions-item label="我的报名">
          <el-tag v-if="detail.myStatus === 'ENROLLED'" type="success">已报名</el-tag>
          <el-tag v-else-if="detail.myStatus === 'WAITING'" type="warning">
            候补中 (位次 {{ detail.myWaitOrder }})
          </el-tag>
          <el-tag v-else type="info">未报名</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="!detail?.myStatus"
          type="primary"
          :disabled="!canRegisterDetail"
          @click="handleRegisterFromDetail"
        >
          {{ detail?.remaining === 0 ? '候补登记' : '报名' }}
        </el-button>
        <el-button
          v-else-if="detail?.myStatus === 'ENROLLED'"
          type="warning"
          @click="handleWithdrawFromDetail"
        >
          退选
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import {
  pageActivities, getActivityDetail, registerActivity, withdrawActivity, unreadCount
} from '@/api/student'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activityList = ref([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  keyword: ''
})

const detailVisible = ref(false)
const detail = ref(null)
const unread = ref(0)

// 从学号推导年级: S2024001 -> 2024
const myGrade = userStore.account.length >= 5 ? userStore.account.substring(1, 5) : ''

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function isDeadlineNear(t) {
  if (!t) return false
  return dayjs(t).diff(dayjs(), 'hour') < 24
}

// 是否可报名: 截止未到 + 年级符合
function canRegister(row) {
  if (!row) return false
  if (!dayjs(row.signupDeadline).isAfter(dayjs())) return false
  return gradeAllowed(row.targetGrade)
}

function gradeAllowed(targetGrade) {
  if (!targetGrade) return true
  return targetGrade.split(',').map(s => s.trim()).includes(myGrade)
}

const canRegisterDetail = ref(false)

async function loadData() {
  loading.value = true
  try {
    const data = await pageActivities({
      current: query.current,
      size: query.size,
      keyword: query.keyword || undefined
    })
    activityList.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.current = 1
  loadData()
}

async function openDetail(row) {
  detail.value = null
  detailVisible.value = true
  canRegisterDetail.value = false
  try {
    detail.value = await getActivityDetail(row.activityId)
    canRegisterDetail.value = canRegister(detail.value)
  } catch (e) {
    // ignore
  }
}

async function handleRegister(row) {
  const isWait = row.remaining === 0
  try {
    await ElMessageBox.confirm(
      isWait ? `名额已满, 确认候补登记「${row.title}」?` : `确认报名「${row.title}」?`,
      '提示',
      { type: 'info' }
    )
  } catch {
    return
  }
  try {
    const result = await registerActivity(row.activityId)
    ElMessage.success(result.message || '操作成功')
    loadData()
    loadUnread()
  } catch (e) {
    // ignore
  }
}

async function handleRegisterFromDetail() {
  if (!detail.value) return
  try {
    const result = await registerActivity(detail.value.activityId)
    ElMessage.success(result.message || '操作成功')
    openDetail({ activityId: detail.value.activityId })
    loadData()
    loadUnread()
  } catch (e) {
    // ignore
  }
}

async function handleWithdrawFromDetail() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm(
      `确认退选「${detail.value.title}」? 退选后名额将释放并自动递补候补。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await withdrawActivity(detail.value.activityId)
    ElMessage.success('退选成功')
    openDetail({ activityId: detail.value.activityId })
    loadData()
    loadUnread()
  } catch (e) {
    // ignore
  }
}

async function loadUnread() {
  try {
    unread.value = await unreadCount()
  } catch (e) {
    // ignore
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

onMounted(() => {
  loadData()
  loadUnread()
})
</script>

<style scoped>
.student-wrap {
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

.search-bar {
  margin-bottom: 16px;
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
