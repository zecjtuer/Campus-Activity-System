<template>
  <div class="not-found-wrap">
    <el-result
      icon="warning"
      title="404"
      sub-title="抱歉, 您访问的页面不存在"
    >
      <template #extra>
        <el-button type="primary" @click="$router.push({ name: 'Home' })">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
// 404 页面组件: 访问不存在路由时跳转
</script>

<style scoped>
.not-found-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
</style>
