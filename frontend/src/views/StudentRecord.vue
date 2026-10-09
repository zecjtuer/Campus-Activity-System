<template>
  <div class="record-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 我的报名</span>
      </div>
      <div class="header-right">
        <el-badge :value="unread" :hidden="unread === 0">
          <el-button text @click="$router.push({ name: 'Notifications' })">
            <el-icon><Bell /></el-icon> 消息
          </el-button>
        </el-badge>
        <el-button text @click="$router.push({ name: 'Student' })">返回活动列表</el-button>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <main class="page-container">
      <h2 class="section-title">我的报名记录</h2>

      <!-- 状态筛选 -->
      <el-form :inline="true" class="search-bar" @submit.prevent="loadData">
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="loadData"
          >
            <el-option label="已报名" value="ENROLLED" />
            <el-option label="候补中" value="WAITING" />
            <el-option label="已退选" value="CANCELLED" />
            <el-option label="已失效" value="INVALID" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="records" stripe>
        <el-table-column prop="activityId" label="活动ID" width="80" />
        <el-table-column prop="title" label="活动名称" min-width="160" />
        <el-table-column label="活动时间" width="170">
          <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatTime(row.signupTime) }}</template>
        </el-table-column>
        <el-table-column label="候补位次" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'WAITING'" size="small" type="warning">
              第 {{ row.waitOrder }} 位
            </el-tag>
            <el-text v-else size="small" type="info">-</el-text>
          </template>
        </el-table-column>
        <el-table-column label="展示状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.displayStatus)">
              {{ displayLabel(row.displayStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.displayStatus === 'ONGOING'"
              size="small"
              type="warning"
              @click="handleWithdraw(row)"
            >
              退选
            </el-button>
            <el-text v-else size="small" type="info">-</el-text>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import { myRegistrations, withdrawActivity, unreadCount } from '@/api/student'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const records = ref([])
const total = ref(0)
const unread = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  status: ''
})

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function tagType(status) {
  switch (status) {
    case 'ONGOING': return 'success'
    case 'FINISHED': return 'info'
    case 'ENROLLED': return 'primary'
    case 'WAITING': return 'warning'
    case 'CANCELLED': return 'warning'
    case 'INVALID': return 'danger'
    default: return 'info'
  }
}

function displayLabel(status) {
  return {
    ENROLLED: '已报名',
    ONGOING: '进行中',
    FINISHED: '已完成',
    WAITING: '候补中',
    CANCELLED: '已退选',
    INVALID: '已失效'
  }[status] || status
}

async function loadData() {
  loading.value = true
  try {
    const data = await myRegistrations({
      current: query.current,
      size: query.size,
      status: query.status || undefined
    })
    records.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.status = ''
  query.current = 1
  loadData()
}

async function handleWithdraw(row) {
  try {
    await ElMessageBox.confirm(
      `确认退选「${row.title}」? 退选后名额将释放并自动递补候补, 活动开始后不可退选。`,
      '提示',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await withdrawActivity(row.activityId)
    ElMessage.success('退选成功')
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
.record-wrap {
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
