<template>
  <div class="home-wrap">
    <el-header class="home-header">
      <div class="brand" @click="goHome">
        <img src="/favicon.svg" alt="logo" class="logo" />
        <span>校园活动管理系统</span>
      </div>
      <div class="header-right">
        <template v-if="userStore.isLoggedIn">
          <el-tag :type="roleTagType">
            {{ roleLabel }}
          </el-tag>
          <el-text>{{ userStore.name }}</el-text>
          <el-button text @click="goDashboard">进入 {{ dashboardName }}</el-button>
          <el-button text type="danger" @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="$router.push({ name: 'Login' })">登录</el-button>
        </template>
      </div>
    </el-header>

    <main class="page-container home-main">
      <h2 class="section-title">活动总览</h2>
      <p class="home-sub">
        解决校园活动信息分散、报名流程混乱、教师组织效率低的问题。
      </p>

      <el-row :gutter="16" class="feature-cards">
        <el-col :xs="24" :sm="8">
          <el-card class="feature-card">
            <el-icon size="32" color="#409eff"><Search /></el-icon>
            <h3>浏览搜索</h3>
            <p>按发布时间倒序展示, 关键词匹配活动名称、地点、简介</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="feature-card">
            <el-icon size="32" color="#67c23a"><EditPen /></el-icon>
            <h3>报名退选</h3>
            <p>限定时间内可报名与退选, 名额自动释放, 历史可追溯</p>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="feature-card">
            <el-icon size="32" color="#e6a23c"><DataAnalysis /></el-icon>
            <h3>统计管理</h3>
            <p>教师发布/修改/删除活动, 实时统计报名人数与剩余名额</p>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="role-card" style="margin-top: 24px;">
        <template #header>
          <span>按角色进入</span>
        </template>
        <el-space wrap>
          <el-button type="success" @click="requireLoginThen('Student')">
            <el-icon><User /></el-icon> 学生端
          </el-button>
          <el-button type="warning" @click="requireLoginThen('Teacher')">
            <el-icon><Avatar /></el-icon> 教师端
          </el-button>
          <el-button type="danger" @click="requireLoginThen('Admin')">
            <el-icon><Setting /></el-icon> 管理员端
          </el-button>
          <el-button @click="requireLoginThen('StudentRecord')">
            <el-icon><Tickets /></el-icon> 我的报名
          </el-button>
          <el-button @click="requireLoginThen('Notifications')">
            <el-icon><Bell /></el-icon> 站内消息
          </el-button>
        </el-space>
      </el-card>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search, EditPen, DataAnalysis, User, Avatar, Tickets, Setting, Bell
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// V2.0: 角色展示
const roleLabel = computed(() =>
  userStore.isAdmin ? '管理员' : (userStore.isTeacher ? '教师' : '学生')
)
const roleTagType = computed(() =>
  userStore.isAdmin ? 'danger' : (userStore.isTeacher ? 'warning' : 'success')
)
const dashboardName = computed(() =>
  userStore.isAdmin ? '管理员端' : (userStore.isTeacher ? '教师端' : '学生端')
)

function goHome() {
  router.push({ name: 'Home' })
}

function requireLoginThen(name) {
  if (!userStore.isLoggedIn) {
    ElMessage.info('请先登录')
    router.push({ name: 'Login', query: { redirect: `/${name.toLowerCase()}` } })
    return
  }
  // 角色限制
  if (name === 'Teacher' && !userStore.isTeacher) {
    ElMessage.warning('当前账号不是教师, 无法访问教师端')
    return
  }
  if (name === 'Admin' && !userStore.isAdmin) {
    ElMessage.warning('当前账号不是管理员, 无法访问管理员端')
    return
  }
  if ((name === 'Student' || name === 'StudentRecord') && !userStore.isStudent) {
    ElMessage.warning('当前账号不是学生, 无法访问学生端')
    return
  }
  router.push({ name })
}

function goDashboard() {
  if (userStore.isAdmin) router.push({ name: 'Admin' })
  else if (userStore.isTeacher) router.push({ name: 'Teacher' })
  else router.push({ name: 'Student' })
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push({ name: 'Home' })
}
</script>

<style scoped>
.home-wrap {
  min-height: 100vh;
  background: #f5f7fa;
}

.home-header {
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
  cursor: pointer;
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

.home-main {
  padding-top: 32px;
}

.home-sub {
  color: #606266;
  margin: 0 0 24px;
  font-size: 14px;
}

.feature-card {
  text-align: center;
  padding: 24px 16px;
  height: 100%;
}

.feature-card h3 {
  margin: 12px 0 8px;
  font-size: 16px;
}

.feature-card p {
  margin: 0;
  color: #909399;
  font-size: 13px;
  line-height: 1.6;
}

.role-card .el-space {
  width: 100%;
}
</style>
