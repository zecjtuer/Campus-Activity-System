<template>
  <div class="record-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 我的报名</span>
      </div>
      <div class="header-right">
        <el-badge :value="unread" :hidden="unread === 0" class="msg-badge">
          <el-button text @click="$router.push({ name: 'Notifications' })">
            <el-icon class="bell-icon"><Bell /></el-icon> 消息
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

      <el-table
        v-loading="loading"
        :data="records"
        stripe
        :row-class-name="rowClassName"
      >
        <el-table-column prop="activityId" label="活动ID" width="80" />
        <el-table-column label="活动名称" min-width="160">
          <template #default="{ row }">
            <span>{{ row.title }}</span>
            <el-tag v-if="changedIds.has(row.recordId)" size="small" type="danger" class="new-tag">
              状态更新
            </el-tag>
          </template>
        </el-table-column>
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
              :loading="submitting === row.activityId"
              :disabled="submitting"
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
import { useNotify } from '@/composables/useNotify'
import { myRegistrations, withdrawActivity } from '@/api/student'

const router = useRouter()
const userStore = useUserStore()
const { unread, startPolling, refresh: refreshUnread } = useNotify()

const loading = ref(false)
const records = ref([])
const total = ref(0)
const submitting = ref(false)
const changedIds = ref(new Set()) // V3.0: 状态变化的记录ID集合, 用于高亮
let prevStatusMap = new Map() // 上次加载的 recordId -> status 快照
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

// V3.0: 状态变化行高亮 (对比上次快照)
function rowClassName({ row }) {
  return changedIds.value.has(row.recordId) ? 'row-changed' : ''
}

async function loadData() {
  loading.value = true
  try {
    const data = await myRegistrations({
      current: query.current,
      size: query.size,
      status: query.status || undefined
    })
    const newRecords = data.records || []
    // 计算状态变化的记录
    const changed = new Set()
    for (const r of newRecords) {
      const prev = prevStatusMap.get(r.recordId)
      if (prev !== undefined && prev !== r.displayStatus) {
        changed.add(r.recordId)
      }
    }
    changedIds.value = changed
    // 更新快照
    prevStatusMap = new Map(newRecords.map(r => [r.recordId, r.displayStatus]))
    records.value = newRecords
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
  submitting.value = row.activityId
  try {
    await withdrawActivity(row.activityId)
    ElMessage.success('退选成功')
    loadData()
    refreshUnread()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e?.message || '退选失败, 请重试')
  } finally {
    submitting.value = false
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

onMounted(() => {
  loadData()
  startPolling(30000)
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

/* V3.0: 状态变化行高亮 + 消息红点脉冲 */
:deep(.row-changed) {
  background-color: #fff7e6 !important;
  animation: row-flash 2s ease-in-out 3;
}
.new-tag {
  margin-left: 8px;
}
.msg-badge :deep(.el-badge__content) {
  animation: badge-pulse 1.5s ease-in-out infinite;
}
.bell-icon {
  transition: transform 0.3s;
}
@keyframes row-flash {
  0%, 100% { background-color: #fff7e6; }
  50% { background-color: #ffe4b5; }
}
@keyframes badge-pulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.25); opacity: 0.85; }
}
</style>
