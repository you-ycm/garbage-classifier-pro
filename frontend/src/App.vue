<!-- ===== App.vue ===== -->

<template>
  <div id="app">
    <!-- 未登录：显示登录/注册/找回密码页 -->
    <div v-if="!userStore.token">
      <router-view />
    </div>

    <!-- 已登录：显示完整功能 -->
    <div v-else>
      <!-- 导航栏 -->
      <el-menu mode="horizontal" :default-active="activeIndex" @select="handleSelect">
        <el-menu-item index="/classify">🌍 垃圾分类</el-menu-item>
        <el-menu-item index="/history">📋 识别历史</el-menu-item>
        <!-- 只有管理员能看到后台管理 -->
        <el-menu-item index="/admin" v-if="userStore.role === 'ROLE_ADMIN'">⚙ 后台管理</el-menu-item>
        <!-- 个人信息 -->
        <el-menu-item index="/profile">👤 个人信息</el-menu-item>
        <!-- 我的收藏 -->
        <el-menu-item index="/favorites">⭐ 我的收藏</el-menu-item>
        <!-- 垃圾分类知识 -->
        <el-menu-item index="/knowledge">📚 垃圾分类知识</el-menu-item>

        <!-- 右侧用户信息 -->
        <div style="float: right; display: flex; align-items: center; gap: 16px; padding: 0 20px; height: 60px;">
          <span style="font-weight: 600; color: #333;">
            👤 {{ userStore.username }}
          </span>
          <span style="color: #409EFF; font-size: 14px; background: #ecf5ff; padding: 4px 12px; border-radius: 12px;">
            {{ userStore.email || '未绑定邮箱' }}
          </span>
        </div>
        <el-menu-item index="logout" style="float: right;">🚪 退出</el-menu-item>
      </el-menu>

      <!-- 🔥 核心修改：这里使用 router-view，路由跳转后自动切换页面！ -->
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from './store/userStore'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 默认高亮当前的菜单项
const activeIndex = ref(route.path)

// 监听路由变化，同步更新菜单高亮
watch(() => route.path, (newPath) => {
  activeIndex.value = newPath
})

const handleSelect = (key) => {
  if (key === 'logout') {
    logout()
    return
  }
  // 导航栏直接跳转路由
  router.push(key)
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
  display: flex;
  align-items: center;
}

.el-menu-item {
  border-bottom: 2px solid transparent;
}

.content {
  min-height: calc(100vh - 60px);
  padding: 20px; /* 增加一点内边距让页面更好看 */
}
</style>