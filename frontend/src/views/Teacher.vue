<template>
  <div class="teacher-wrap">
    <el-header class="page-header">
      <div class="brand">
        <img src="/favicon.svg" class="logo" alt="logo" />
        <span>校园活动管理系统 · 教师端</span>
      </div>
      <div class="header-right">
        <el-text>{{ userStore.name }} ({{ userStore.account }})</el-text>
        <el-badge :value="unread" :hidden="unread === 0">
          <el-button text @click="$router.push({ name: 'Notifications' })">
            <el-icon><Bell /></el-icon> 消息
          </el-button>
        </el-badge>
        <el-button text type="danger" @click="handleLogout">退出</el-button>
      </div>
    </el-header>

    <main class="page-container">
      <div class="title-bar">
        <h2 class="section-title">本人发布的活动</h2>
        <el-button type="primary" @click="openCreate">
          <el-icon><Plus /></el-icon> 发布活动
        </el-button>
      </div>

      <el-table v-loading="loading" :data="activityList" stripe>
        <el-table-column prop="activityId" label="ID" width="70" />
        <el-table-column prop="title" label="活动名称" min-width="150" />
        <el-table-column prop="location" label="地点" width="130" />
        <el-table-column label="活动时间" width="170">
          <template #default="{ row }">{{ formatTime(row.activityTime) }}</template>
        </el-table-column>
        <el-table-column label="报名截止" width="170">
          <template #default="{ row }">{{ formatTime(row.signupDeadline) }}</template>
        </el-table-column>
        <el-table-column label="限定年级" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.targetGrade" size="small" type="warning">{{ row.targetGrade }}</el-tag>
            <el-text v-else size="small" type="info">不限</el-text>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="140">
          <template #default="{ row }">
            <el-text type="success">{{ row.enrolledCount }}</el-text>
            <el-text size="small" type="info"> / {{ row.maxParticipants }}</el-text>
            <el-text size="small" type="warning"> (剩 {{ row.remaining }})</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openStats(row)">统计</el-button>
            <el-button size="small" type="info" @click="openWaiting(row)">候补</el-button>
            <el-button size="small" @click="handleExport(row)">导出</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 发布/编辑弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'create' ? '发布活动' : '编辑活动'"
      width="640px"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="活动名称" prop="title">
          <el-input v-model="form.title" maxlength="100" show-word-limit placeholder="如: 校园篮球赛" />
        </el-form-item>
        <el-form-item label="活动时间" prop="activityTime">
          <el-date-picker
            v-model="form.activityTime"
            type="datetime"
            placeholder="选择活动举办时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="地点" prop="location">
          <el-input v-model="form.location" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="活动简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="人数上限" prop="maxParticipants">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="报名截止" prop="signupDeadline">
          <el-date-picker
            v-model="form.signupDeadline"
            type="datetime"
            placeholder="报名截止时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="限定年级" prop="targetGrade">
          <el-select
            v-model="form.targetGradeList"
            multiple
            placeholder="不选则不限年级"
            style="width: 100%"
          >
            <el-option v-for="y in yearOptions" :key="y" :label="y + ' 级'" :value="y" />
          </el-select>
          <div class="form-tip">不选 = 不限年级; 选择多个 = 仅这些年级可报名</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 统计弹窗 -->
    <el-dialog v-model="statsVisible" title="报名统计" width="480px">
      <el-descriptions v-if="stats" :column="1" border>
        <el-descriptions-item label="活动名称">{{ stats.title }}</el-descriptions-item>
        <el-descriptions-item label="人数上限">{{ stats.maxParticipants }}</el-descriptions-item>
        <el-descriptions-item label="已报人数">
          <el-text type="success">{{ stats.enrolledCount }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="剩余名额">
          <el-text type="warning">{{ stats.remaining }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="候补人数">
          <el-text type="primary">{{ stats.waitingCount }}</el-text>
        </el-descriptions-item>
        <el-descriptions-item label="已退选">{{ stats.cancelledCount }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="statsVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 候补队列弹窗 -->
    <el-dialog v-model="waitingVisible" :title="`候补队列 - ${waitingTitle}`" width="720px">
      <el-table v-loading="waitingLoading" :data="waitingList" stripe>
        <el-table-column label="序号" width="80">
          <template #default="{ row }">{{ row.waitOrder }}</template>
        </el-table-column>
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="studentName" label="姓名" width="100" />
        <el-table-column prop="grade" label="年级" width="80" />
        <el-table-column label="报名时间" width="170">
          <template #default="{ row }">{{ formatTime(row.signupTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        v-model:current-page="waitingQuery.current"
        v-model:page-size="waitingQuery.size"
        :total="waitingTotal"
        layout="total, prev, pager, next"
        @current-change="loadWaiting"
      />
      <template #footer>
        <el-button @click="waitingVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Bell } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useUserStore } from '@/stores/user'
import {
  pageMyActivities, publishActivity, updateActivity, deleteActivity,
  activityStats, waitingQueue, exportRoster, unreadCount
} from '@/api/teacher'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const activityList = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 10 })
const unread = ref(0)

// 年级选项 (近 6 届)
const yearOptions = ['2021', '2022', '2023', '2024', '2025', '2026']

const formVisible = ref(false)
const formMode = ref('create')
const submitting = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  title: '',
  activityTime: '',
  location: '',
  description: '',
  maxParticipants: 30,
  signupDeadline: '',
  targetGradeList: []
})

const rules = {
  title: [
    { required: true, message: '请输入活动名称', trigger: 'blur' },
    { max: 100, message: '长度不能超过 100', trigger: 'blur' }
  ],
  activityTime: [{ required: true, message: '请选择活动时间', trigger: 'change' }],
  location: [
    { required: true, message: '请输入地点', trigger: 'blur' },
    { max: 100, message: '长度不能超过 100', trigger: 'blur' }
  ],
  description: [{ max: 500, message: '长度不能超过 500', trigger: 'blur' }],
  maxParticipants: [
    { required: true, message: '请输入人数上限', trigger: 'blur' },
    { type: 'number', min: 1, message: '必须大于 0', trigger: 'blur' }
  ],
  signupDeadline: [{ required: true, message: '请选择报名截止时间', trigger: 'change' }]
}

const statsVisible = ref(false)
const stats = ref(null)

// 候补队列
const waitingVisible = ref(false)
const waitingLoading = ref(false)
const waitingList = ref([])
const waitingTotal = ref(0)
const waitingTitle = ref('')
const waitingQuery = reactive({ current: 1, size: 10 })
const waitingActivityId = ref(null)

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'
}

async function loadData() {
  loading.value = true
  try {
    const data = await pageMyActivities({ current: query.current, size: query.size })
    activityList.value = data.records || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadUnread() {
  try {
    unread.value = await unreadCount()
  } catch (e) {
    // ignore
  }
}

function openCreate() {
  formMode.value = 'create'
  resetForm()
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  editingId.value = row.activityId
  form.title = row.title
  form.activityTime = formatTime(row.activityTime)
  form.location = row.location
  form.description = row.description || ''
  form.maxParticipants = row.maxParticipants
  form.signupDeadline = formatTime(row.signupDeadline)
  form.targetGradeList = row.targetGrade ? row.targetGrade.split(',').map(s => s.trim()) : []
  formVisible.value = true
}

function resetForm() {
  editingId.value = null
  form.title = ''
  form.activityTime = ''
  form.location = ''
  form.description = ''
  form.maxParticipants = 30
  form.signupDeadline = ''
  form.targetGradeList = []
  formRef.value?.clearValidate?.()
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        title: form.title,
        activityTime: form.activityTime,
        location: form.location,
        description: form.description || undefined,
        maxParticipants: form.maxParticipants,
        signupDeadline: form.signupDeadline,
        targetGrade: form.targetGradeList.length ? form.targetGradeList.join(',') : undefined
      }
      if (formMode.value === 'create') {
        await publishActivity(payload)
        ElMessage.success('发布成功')
      } else {
        await updateActivity(editingId.value, payload)
        ElMessage.success('修改成功, 已通知已报名/候补学生')
      }
      formVisible.value = false
      loadData()
    } catch (e) {
      // ignore
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${row.title}」? 删除后该活动学生端不可见, 已报名/候补记录将置为失效, 并通知相关学生。`,
      '危险操作',
      { type: 'warning', confirmButtonText: '确认删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    await deleteActivity(row.activityId)
    ElMessage.success('删除成功, 已通知相关学生')
    loadData()
  } catch (e) {
    // ignore
  }
}

async function openStats(row) {
  stats.value = null
  statsVisible.value = true
  try {
    stats.value = await activityStats(row.activityId)
  } catch (e) {
    // ignore
  }
}

async function openWaiting(row) {
  waitingActivityId.value = row.activityId
  waitingTitle.value = row.title
  waitingQuery.current = 1
  waitingVisible.value = true
  loadWaiting()
}

async function loadWaiting() {
  waitingLoading.value = true
  try {
    const data = await waitingQueue(waitingActivityId.value, {
      current: waitingQuery.current,
      size: waitingQuery.size
    })
    waitingList.value = data.records || []
    waitingTotal.value = data.total || 0
  } finally {
    waitingLoading.value = false
  }
}

async function handleExport(row) {
  try {
    const blob = await exportRoster(row.activityId)
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'text/csv;charset=utf-8' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `activity_${row.activityId}_roster.csv`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    // ignore
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}

// 进入页面加载数据
loadData()
loadUnread()
</script>

<style scoped>
.teacher-wrap {
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

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
  margin-top: 4px;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
