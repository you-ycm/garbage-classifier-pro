<template>
  <div class="knowledge-container">
    <h2>📚 垃圾分类知识库</h2>
    <div class="subtitle">了解垃圾分类标准，共建美好环境</div>

    <!-- 顶部统计卡片 -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card"><span class="emoji">♻️</span> 可回收物 <span class="count">{{ counts.recyclable }}</span> 种</div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card"><span class="emoji">☣️</span> 有害垃圾 <span class="count">{{ counts.hazardous }}</span> 种</div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card"><span class="emoji">🍃</span> 厨余垃圾 <span class="count">{{ counts.kitchen }}</span> 种</div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card"><span class="emoji">🗑️</span> 其他垃圾 <span class="count">{{ counts.other }}</span> 种</div>
      </el-col>
    </el-row>

    <!-- 搜索框 + 批量操作按钮 -->
    <div class="search-area">
      <el-input
          v-model="searchKey"
          placeholder="搜索垃圾分类知识..."
          prefix-icon="Search"
          clearable
          @input="fetchData"
          style="width: 350px;"
      />
      <div class="action-buttons">
        <el-button
            type="primary"
            @click="shareKnowledge"
            :disabled="selectedItems.length === 0 || shareLoading"
            :loading="shareLoading"
        >
          📧 分享 ({{ selectedItems.length }})
        </el-button>
        <el-button
            type="success"
            @click="exportCSV"
            :loading="exportLoading"
        >
          📥 导出CSV
        </el-button>
        <el-button
            type="danger"
            plain
            size="small"
            @click="clearSelection"
            :disabled="selectedItems.length === 0"
        >
          取消选择
        </el-button>
      </div>
    </div>

    <!-- 知识列表（新增多选） -->
    <el-table
        :data="tableData"
        stripe
        style="width: 100%; margin-top: 20px;"
        @selection-change="handleSelectionChange"
        ref="tableRef"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="name" label="物品名称" width="180" />
      <el-table-column label="分类" width="150">
        <template #default="scope">
          <el-tag :type="getCategoryTag(scope.row.category)">{{ scope.row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="分类说明" />
      <el-table-column prop="suggestion" label="投放建议" />
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="tableData.length === 0 && !loading" description="暂无相关分类知识" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '../store/userStore'

const userStore = useUserStore()
const API_BASE_URL = 'http://192.168.58.128:8080/api'

// 数据
const tableData = ref([])
const searchKey = ref('')
const loading = ref(false)

// 选中相关
const selectedItems = ref([])
const tableRef = ref(null)
const shareLoading = ref(false)
const exportLoading = ref(false)

// 统计各类别数量
const counts = computed(() => {
  const map = { recyclable: 0, hazardous: 0, kitchen: 0, other: 0 }
  tableData.value.forEach(item => {
    if (item.category === '可回收物') map.recyclable++
    else if (item.category === '有害垃圾') map.hazardous++
    else if (item.category === '厨余垃圾') map.kitchen++
    else if (item.category === '其他垃圾') map.other++
  })
  return map
})

// ===== 获取数据 =====
const fetchData = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE_URL}/knowledge/public/list`, {
      params: { keyword: searchKey.value || undefined }
    })
    if (res.status === 200) {
      tableData.value = res.data
      // 清空选中状态
      selectedItems.value = []
    }
  } catch (err) {
    console.error('加载知识库失败', err)
    ElMessage.error('加载知识库失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// ===== 表格选中事件 =====
const handleSelectionChange = (selection) => {
  selectedItems.value = selection
}

const clearSelection = () => {
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  selectedItems.value = []
}

// ===== 分类标签颜色 =====
const getCategoryTag = (category) => {
  const map = {
    '可回收物': 'success',
    '有害垃圾': 'danger',
    '厨余垃圾': 'warning',
    '其他垃圾': 'info'
  }
  return map[category] || ''
}

// ===== 邮箱分享 =====
const shareKnowledge = async () => {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要分享的知识条目')
    return
  }

  // 检查是否已登录
  if (!userStore.token) {
    ElMessage.warning('请先登录后再分享')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedItems.value.length} 条知识条目发送到您的绑定邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    const ids = selectedItems.value.map(item => item.id)
    const response = await axios.post(`${API_BASE_URL}/knowledge/share`,
        { ids: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    if (response.data.code === 200) {
      ElMessage.success(`成功发送 ${selectedItems.value.length} 条知识条目到您的邮箱！`)
      clearSelection()
    } else {
      ElMessage.error(response.data.message || '分享失败')
    }
  } catch (error) {
    console.error('分享失败:', error)
    if (error.response && error.response.status === 401) {
      ElMessage.error('请先登录后再分享')
    } else {
      ElMessage.error('分享失败，请检查是否已绑定邮箱')
    }
  } finally {
    shareLoading.value = false
  }
}

// ===== 导出CSV（支持搜索/筛选/勾选） =====
const exportCSV = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (searchKey.value) params.keyword = searchKey.value

    if (selectedItems.value.length > 0) {
      params.ids = selectedItems.value.map(item => item.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/knowledge/export`, {
      params: params,
      responseType: 'blob'
    })

    // 检查是否是 JSON 错误响应
    if (response.data.type === 'application/json') {
      const reader = new FileReader()
      reader.onload = () => {
        try {
          const errorData = JSON.parse(reader.result)
          ElMessage.error(errorData.message || '导出失败，请重试')
        } catch {
          ElMessage.error('导出失败，响应格式异常')
        }
      }
      reader.readAsText(response.data)
      return
    }

    const url = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv;charset=utf-8;' }))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `垃圾分类知识库_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    if (error.response) {
      ElMessage.error(`导出失败：${error.response.status} ${error.response.statusText}`)
    } else if (error.request) {
      ElMessage.error('导出失败：网络请求未收到响应')
    } else {
      ElMessage.error(`导出失败：${error.message}`)
    }
  } finally {
    exportLoading.value = false
  }
}

// ===== 生命周期 =====
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.knowledge-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.subtitle {
  color: #666;
  margin-bottom: 20px;
}
.stat-cards {
  margin-bottom: 24px;
}
.stat-card {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  font-weight: bold;
  color: #333;
}
.stat-card .emoji {
  font-size: 20px;
  margin-right: 8px;
}
.stat-card .count {
  color: #409EFF;
  font-size: 18px;
  margin-left: 8px;
}
.search-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}
.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>