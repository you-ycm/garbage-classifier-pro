<template>
  <div class="admin-container">
    <h2>⚙️ 管理员后台</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 1. 系统概览 -->
      <el-tab-pane label="📊 系统概览" name="overview">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">👤 用户总数</div>
              </template>
              <div class="stat-number">{{ stats.userCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">📄 识别记录总数</div>
              </template>
              <div class="stat-number">{{ stats.recordCount }}</div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 2. 用户管理 -->
      <el-tab-pane label="👤 用户管理" name="users">
        <el-table :data="users" style="width: 100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="role" label="角色">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ROLE_ADMIN' ? 'danger' : 'success'">
                {{ row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="deleteUser(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 3. 识别记录管理 -->
      <el-tab-pane label="📄 识别记录管理" name="records">
        <el-table :data="records" style="width: 100%">
          <el-table-column prop="itemName" label="物品名称" width="150" />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="150">
            <template #default="{ row }">
              {{ (row.confidence * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="识别时间" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="deleteRecord(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '../store/userStore'

const userStore = useUserStore()
const API_BASE_URL = 'http://localhost:8080/api'

const activeTab = ref('overview')
const stats = ref({ userCount: 0, recordCount: 0 })
const users = ref([])
const records = ref([])

// 获取统计数据
const loadStats = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/admin/stats`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      stats.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  }
}

// 获取用户列表
const loadUsers = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/admin/users`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      users.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('加载用户列表失败')
  }
}

// 删除用户
const deleteUser = async (id) => {
  if (!confirm('确定要删除该用户吗？')) return
  try {
    const response = await axios.delete(`${API_BASE_URL}/admin/users/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('用户删除成功')
      loadUsers()
    }
  } catch (error) {
    ElMessage.error('删除用户失败')
  }
}

// 获取识别记录
const loadRecords = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/admin/records`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      records.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('加载识别记录失败')
  }
}

// 删除识别记录
const deleteRecord = async (id) => {
  if (!confirm('确定要删除该记录吗？')) return
  try {
    const response = await axios.delete(`${API_BASE_URL}/admin/records/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('记录删除成功')
      loadRecords()
    }
  } catch (error) {
    ElMessage.error('删除记录失败')
  }
}

const getCategoryType = (category) => {
  const map = {
    '可回收物': 'success',
    '有害垃圾': 'danger',
    '厨余垃圾': 'warning',
    '其他垃圾': 'info'
  }
  return map[category] || 'info'
}

onMounted(() => {
  loadStats()
  loadUsers()
  loadRecords()
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.stat-header {
  font-size: 16px;
  color: #666;
}
.stat-number {
  font-size: 36px;
  font-weight: bold;
  color: #333;
  text-align: center;
  padding: 20px 0;
}
</style>