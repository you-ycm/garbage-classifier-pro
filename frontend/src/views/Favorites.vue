<template>
  <div class="favorites-container">
    <h2>⭐ 我的收藏</h2>

    <!-- 筛选区域 -->
    <div class="filter-area">
      <div class="filter-left">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索物品名称"
            clearable
            prefix-icon="Search"
            @input="loadFavorites"
            style="width: 180px;"
        />

        <el-select
            v-model="filterCategory"
            placeholder="全部类别"
            clearable
            @change="loadFavorites"
            style="width: 150px;"
        >
          <el-option label="全部类别" value="" />
          <el-option label="可回收物" value="可回收物" />
          <el-option label="有害垃圾" value="有害垃圾" />
          <el-option label="厨余垃圾" value="厨余垃圾" />
          <el-option label="其他垃圾" value="其他垃圾" />
        </el-select>

        <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            @change="loadFavorites"
            style="width: 180px;"
            clearable
        />

        <el-radio-group v-model="sortOrder" @change="loadFavorites" size="small">
          <el-radio-button label="desc">最新优先</el-radio-button>
          <el-radio-button label="asc">最早优先</el-radio-button>
        </el-radio-group>

        <span style="color: #999; font-size: 14px; margin-left: 8px;">共 {{ total }} 条记录</span>
      </div>
      <div class="filter-right">
        <el-button
            type="primary"
            @click="shareFavorites"
            :disabled="selectedRecords.length === 0 || shareLoading"
            :loading="shareLoading"
        >
          📧 分享 ({{ selectedRecords.length }})
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
            @click="batchUnfavorite"
            :disabled="selectedRecords.length === 0 || favoriteLoading"
            :loading="favoriteLoading"
        >
          ★ 取消收藏 ({{ selectedRecords.length }})
        </el-button>
        <el-button
            type="danger"
            plain
            size="small"
            @click="clearSelection"
            :disabled="selectedRecords.length === 0"
        >
          取消选择
        </el-button>
      </div>
    </div>

    <el-table
        :data="records"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        ref="tableRef"
    >
      <el-table-column type="selection" width="55" />

      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <el-image
              v-if="row.imageUrl"
              :src="`http://192.168.58.128:8080${row.imageUrl}`"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px; cursor: pointer;"
              :preview-src-list="[`http://192.168.58.128:8080${row.imageUrl}`]"
              :preview-teleported="true"
          />
          <span v-else style="color: #999; font-size: 12px;">无图片</span>
        </template>
      </el-table-column>

      <el-table-column prop="itemName" label="物品名称" width="150" />
      <el-table-column prop="category" label="分类" width="120">
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

      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="removeFavorite(row.id)">取消收藏</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="loadFavorites"
        @size-change="loadFavorites"
        style="margin-top: 16px;"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/userStore'

const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRecords = ref([])
const favoriteLoading = ref(false)
const shareLoading = ref(false)
const exportLoading = ref(false)
const tableRef = ref(null)

// 搜索筛选参数
const searchKeyword = ref('')
const filterCategory = ref('')
const selectedDate = ref('')
const sortOrder = ref('desc')

const userStore = useUserStore()
const API_BASE_URL = 'http://192.168.58.128:8080/api'

const getTagType = (category) => {
  const map = {
    '可回收物': 'primary',
    '有害垃圾': 'danger',
    '厨余垃圾': 'success',
    '其他垃圾': 'info'
  }
  return map[category] || ''
}

// ===== 加载收藏列表（支持搜索/筛选/排序） =====
const loadFavorites = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortOrder: sortOrder.value
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value

    const response = await axios.get(`${API_BASE_URL}/favorites`, {
      params: params,
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    if (response.data.code === 200) {
      const result = response.data.data
      records.value = result.data || []
      total.value = result.total || 0
      selectedRecords.value = []
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载收藏列表失败')
  }
}

// ===== 表格选中事件 =====
const handleSelectionChange = (selection) => {
  selectedRecords.value = selection
}

const clearSelection = () => {
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  selectedRecords.value = []
}

// ===== 单个取消收藏 =====
const removeFavorite = async (recordId) => {
  try {
    const response = await axios.delete(`${API_BASE_URL}/favorites`, {
      data: { recordId: recordId },
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    if (response.data.code === 200) {
      ElMessage.success('取消收藏成功')
      loadFavorites()
    } else {
      ElMessage.error(response.data.message || '取消收藏失败')
    }
  } catch (error) {
    console.error('取消收藏失败:', error)
    ElMessage.error('取消收藏失败')
  }
}

// ===== 批量取消收藏 =====
const batchUnfavorite = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要取消收藏的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要取消收藏选中的 ${selectedRecords.value.length} 条记录吗？`,
        '确认取消收藏',
        { confirmButtonText: '确定取消', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  favoriteLoading.value = true
  try {
    const ids = selectedRecords.value.map(item => item.id)
    const response = await axios.delete(`${API_BASE_URL}/favorites/batch`, {
      data: { recordIds: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success(`成功取消收藏 ${selectedRecords.value.length} 条记录`)
      clearSelection()
      loadFavorites()
    } else {
      ElMessage.error(response.data.message || '批量取消收藏失败')
    }
  } catch (error) {
    console.error('批量取消收藏失败:', error)
    ElMessage.error('批量取消收藏失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

// ===== 邮箱分享 =====
const shareFavorites = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要分享的收藏记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedRecords.value.length} 条收藏记录发送到邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    const ids = selectedRecords.value.map(item => item.id)
    const response = await axios.post(`${API_BASE_URL}/favorites/share`,
        { recordIds: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    if (response.data.code === 200) {
      ElMessage.success(`成功发送 ${selectedRecords.value.length} 条收藏记录到您的邮箱！`)
      clearSelection()
    } else {
      ElMessage.error(response.data.message || '分享失败')
    }
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败，请检查是否已绑定邮箱')
  } finally {
    shareLoading.value = false
  }
}

// ===== 导出CSV（支持搜索/筛选/勾选） =====
const exportCSV = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value
    params.sortOrder = sortOrder.value

    if (selectedRecords.value.length > 0) {
      params.ids = selectedRecords.value.map(item => item.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/favorites/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

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
    link.setAttribute('download', `我的收藏_${new Date().toISOString().slice(0,10)}.csv`)
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

onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
  background: white;
  border-radius: 12px;
}

.favorites-container h2 {
  margin-top: 0;
  margin-bottom: 20px;
  color: #333;
}

.filter-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>