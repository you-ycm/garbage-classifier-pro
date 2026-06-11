<template>
  <div class="history-container">
    <h2>📋 识别历史</h2>

    <el-table :data="records" stripe style="width: 100%">
      <el-table-column prop="itemName" label="物品名称" width="120" />
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }">
          <el-tag :type="getTagType(row.category)">
            {{ row.category }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="confidence" label="置信度" width="150">
        <template #default="{ row }">
          <el-progress :percentage="row.confidence * 100" :stroke-width="6" :show-text="false" />
          <span style="font-size: 12px">{{ (row.confidence * 100).toFixed(1) }}%</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="识别时间" width="180" />
    </el-table>

    <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadHistory"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/userStore'

const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const userStore = useUserStore()
const API_BASE_URL = 'http://localhost:8080/api'  // ✅ 修改为 localhost

const getTagType = (category) => {
  const map = {
    '可回收物': 'primary',
    '有害垃圾': 'danger',
    '厨余垃圾': 'success',
    '其他垃圾': 'info'
  }
  return map[category] || ''
}

const loadHistory = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/history`, {
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    if (response.data.code === 200) {
      records.value = response.data.data
      total.value = response.data.data.length
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败，请检查网络或后端')
  }
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.history-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
}
</style>