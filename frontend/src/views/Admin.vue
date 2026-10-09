<template>
  <div class="admin-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 管理员端</span>
      </div>
      <div class="header-right">
        <el-text>{{ userStore.name }} ({{ userStore.account }})</el-text>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <main class="page-container">
      <h2 class="section-title">账号管理</h2>

      <!-- 筛选 -->
      <el-form :inline="true" class="search-bar" @submit.prevent="loadData">
        <el-form-item label="角色">
          <el-select
            v-model="query.role"
            placeholder="全部"
            clearable
            style="width: 140px"
            @change="loadData"
          >
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 140px"
            @change="loadData"
          >
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="userList" stripe>
        <el-table-column prop="account" label="账号" width="140" />
        <el-table-column prop="userNo" label="学号/工号" width="140" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="roleTag(row.role)">{{ roleLabel(row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              size="small"
              type="danger"
              :disabled="row.role === 'ADMIN'"
              @click="handleDisable(row)"
            >
              停用
            </el-button>
            <el-button
              v-else
              size="small"
              type="success"
              @click="handleEnable(row)"
            >
              启用
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { pageUsers, disableUser, enableUser } from '@/api/admin'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const userList = ref([])
const total = ref(0)
const query = reactive({
  current: 1,
  size: 10,
  role: '',
  status: undefined
})

function roleLabel(role) {
  return { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[role] || role
}

function roleTag(role) {
  if (role === 'ADMIN') return 'danger'
  if (role === 'TEACHER') return 'warning'
  return 'success'
}

async function loadData() {
  loading.value = true
  try {
    const data = await pageUsers({
      current: query.current,
      size: query.size,
      role: query.role || undefined,
      status: query.status
    })
    userList.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.role = ''
  query.status = undefined
  query.current = 1
  loadData()
}

async function handleDisable(row) {
  try {
    await ElMessageBox.confirm(
      `确认停用账号「${row.name} (${row.account})」? 停用后该账号将无法登录, 历史数据保留。`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确认停用', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    await disableUser(row.account)
    ElMessage.success('已停用')
    loadData()
  } catch (e) {
    // ignore
  }
}

async function handleEnable(row) {
  try {
    await enableUser(row.account)
    ElMessage.success('已启用')
    loadData()
  } catch (e) {
    // ignore
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

onMounted(loadData)
</script>

<style scoped>
.admin-wrap {
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
