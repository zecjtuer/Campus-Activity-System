<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="login-header">
        <h1>校园活动管理系统</h1>
        <p>V2.0 · 勇闯天涯组</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="account">
          <el-input
            v-model="form.account"
            placeholder="请输入学号/工号"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="初始密码: 123456"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-tip">
        <el-text type="info" size="small">
          学生 S2024001~S2024006 / 教师 T2024001~T2024002 / 管理员 admin
        </el-text>
      </div>

      <div class="login-back">
        <router-link to="/">← 返回首页</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  account: '',
  password: '123456'
})

const rules = {
  account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const data = await userStore.login(form.account, form.password)
      ElMessage.success(`欢迎回来, ${data.name}`)
      // 跳到 redirect 路径或按角色跳对应主页
      const redirect = route.query.redirect
      if (redirect && typeof redirect === 'string') {
        router.push(redirect)
      } else if (data.role === 'TEACHER') {
        router.push({ name: 'Teacher' })
      } else if (data.role === 'ADMIN') {
        router.push({ name: 'Admin' })
      } else {
        router.push({ name: 'Student' })
      }
    } catch (e) {
      // 错误提示已在 http 拦截器统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 24px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 22px;
  color: #303133;
}

.login-header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}

.login-tip {
  text-align: center;
  margin-top: 8px;
}

.login-back {
  text-align: center;
  margin-top: 12px;
}
</style>
