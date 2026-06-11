<template>
  <div id="app">
    <!-- 未登录：显示登录页面 -->
    <div v-if="!userStore.token">
      <router-view />
    </div>

    <!-- 已登录：显示完整功能 -->
    <div v-else>
      <!-- 导航栏 -->
      <el-menu mode="horizontal" :default-active="activeIndex" @select="handleSelect">
        <el-menu-item index="classify">🌍 垃圾分类</el-menu-item>
        <el-menu-item index="history">📋 识别历史</el-menu-item>
        <!-- 只有管理员能看到后台管理 -->
        <el-menu-item index="admin" v-if="userStore.role === 'ROLE_ADMIN'">⚙️ 后台管理</el-menu-item>
        <el-menu-item index="profile" v-if="userStore.role === 'ROLE_USER'">👤 个人信息</el-menu-item>
        <el-menu-item index="logout">🚪 退出</el-menu-item>
      </el-menu>

      <!-- 内容区域 -->
      <div class="content">
        <GarbageClassifier v-if="activeIndex === 'classify'" />
        <History v-else-if="activeIndex === 'history'" />
        <AdminPanel v-else-if="activeIndex === 'admin'" />
        <Profile v-else-if="activeIndex === 'profile'" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './store/userStore'
import GarbageClassifier from './components/GarbageClassifier.vue'
import History from './components/History.vue'
import AdminPanel from './components/AdminPanel.vue'
import Profile from './views/Profile.vue'

const router = useRouter()
const userStore = useUserStore()
const activeIndex = ref('classify')

const handleSelect = (key) => {
  if (key === 'logout') {
    logout()
    return
  }
  activeIndex.value = key
}

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  background: #f5f7fa;
}

.el-menu {
  background: white;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.content {
  min-height: calc(100vh - 60px);
}
</style>