<!-- History.vue（完整修改版，支持高亮跳转） -->
<template>
  <div class="history-container">
    <h2>📋 识别历史</h2>

    <!-- 筛选和操作区域 -->
    <div class="filter-area">
      <div class="filter-left">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索物品名称"
            clearable
            prefix-icon="Search"
            @input="loadHistory"
            style="width: 180px;"
        />

        <el-select
            v-model="filterCategory"
            placeholder="全部类别"
            clearable
            @change="loadHistory"
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
            @change="loadHistory"
            style="width: 180px;"
            clearable
        />

        <el-radio-group v-model="sortOrder" @change="loadHistory" size="small">
          <el-radio-button label="desc">最新优先</el-radio-button>
          <el-radio-button label="asc">最早优先</el-radio-button>
        </el-radio-group>

        <span style="color: #999; font-size: 14px;">共 {{ total }} 条记录</span>
      </div>
      <div class="filter-right">
        <el-button
            type="primary"
            @click="shareToEmail"
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
            type="warning"
            @click="batchFavorite"
            :disabled="selectedRecords.length === 0 || favoriteLoading"
            :loading="favoriteLoading"
        >
          ⭐ 收藏 ({{ selectedRecords.length }})
        </el-button>
        <el-button
            type="info"
            @click="batchUnfavorite"
            :disabled="selectedRecords.length === 0 || favoriteLoading"
            :loading="favoriteLoading"
        >
          ★ 取消收藏 ({{ selectedRecords.length }})
        </el-button>
        <el-button
            type="danger"
            @click="batchDeleteRecords"
            :disabled="selectedRecords.length === 0 || deleteLoading"
            :loading="deleteLoading"
        >
          🗑️ 删除 ({{ selectedRecords.length }})
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
        row-class-name="history-row"
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

      <el-table-column prop="itemName" label="物品名称" width="120" />
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }">
          <el-tag :type="getTagType(row.category)">
            {{ row.category }}
          </el-tag>
        </template>
      </el-table-column>

      <!-- 收藏标记列 -->
      <el-table-column label="⭐" width="60" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.isFavorited" type="warning" size="small">⭐</el-tag>
          <span v-else style="color: #ccc;">-</span>
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
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[5, 10, 20, 50]"
        @current-change="loadHistory"
        @size-change="loadHistory"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../store/userStore'

const route = useRoute()

const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const filterCategory = ref('')
const sortOrder = ref('desc')
const selectedDate = ref('')
const searchKeyword = ref('')
const selectedRecords = ref([])
const shareLoading = ref(false)
const deleteLoading = ref(false)
const exportLoading = ref(false)
const favoriteLoading = ref(false)
const tableRef = ref(null)

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

const loadHistory = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortOrder: sortOrder.value
    }
    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value
    if (searchKeyword.value) params.keyword = searchKeyword.value

    const response = await axios.get(`${API_BASE_URL}/history`, {
      params: params,
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      }
    })

    if (response.data.code === 200) {
      const data = response.data.data.data || []
      total.value = response.data.data.total || 0

      // 批量检查收藏状态
      if (data.length > 0) {
        const recordIds = data.map(r => r.id)
        try {
          const favResponse = await axios.post(`${API_BASE_URL}/favorites/check/batch`,
              { recordIds: recordIds },
              { headers: { 'Authorization': `Bearer ${userStore.token}` } }
          )
          if (favResponse.data.code === 200) {
            const favMap = favResponse.data.data || {}
            data.forEach(r => {
              r.isFavorited = favMap[r.id] || false
            })
          }
        } catch (error) {
          console.error('检查收藏状态失败:', error)
        }
      }

      records.value = data
      selectedRecords.value = []

      // 高亮跳转（从最近识别点击过来）
      await nextTick()
      const highlightId = route.query.highlight
      if (highlightId) {
        highlightRow(highlightId)
      }
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    ElMessage.error('加载历史记录失败，请检查网络或后端')
  }
}

// 高亮指定的行
const highlightRow = (id) => {
  setTimeout(() => {
    const rows = document.querySelectorAll('.history-row')
    rows.forEach(row => {
      // 移除之前的高亮
      row.style.backgroundColor = ''
      row.style.transition = 'background-color 0.3s'
    })
    // 找到匹配的行
    const record = records.value.find(r => r.id === Number(id))
    if (record) {
      const index = records.value.indexOf(record)
      const rows = document.querySelectorAll('.history-row')
      if (rows[index]) {
        rows[index].style.backgroundColor = '#ecf5ff'
        rows[index].scrollIntoView({ behavior: 'smooth', block: 'center' })
        // 3秒后取消高亮
        setTimeout(() => {
          rows[index].style.backgroundColor = ''
        }, 3000)
      }
    }
  }, 300)
}

const handleSelectionChange = (selection) => {
  selectedRecords.value = selection
}

const clearSelection = () => {
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  selectedRecords.value = []
}

const shareToEmail = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要分享的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedRecords.value.length} 条记录发送到邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    const ids = selectedRecords.value.map(r => r.id)
    const response = await axios.post(`${API_BASE_URL}/history/share`,
        { ids: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )

    if (response.data.code === 200) {
      ElMessage.success(`成功发送 ${selectedRecords.value.length} 条记录到您的邮箱！`)
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

const batchDeleteRecords = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRecords.value.length} 条记录吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  deleteLoading.value = true
  try {
    const ids = selectedRecords.value.map(r => r.id)
    const response = await axios.delete(`${API_BASE_URL}/history/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })

    if (response.data.code === 200) {
      ElMessage.success(`成功删除 ${selectedRecords.value.length} 条记录！`)
      clearSelection()
      loadHistory()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  } finally {
    deleteLoading.value = false
  }
}

const exportCSV = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    params.sortOrder = sortOrder.value

    // 如果勾选了记录，将 ID 数组转换为逗号分隔的字符串传给后端
    if (selectedRecords.value.length > 0) {
      params.ids = selectedRecords.value.map(r => r.id).join(',');
    }

    const response = await axios.get(`${API_BASE_URL}/history/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `识别历史_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

const batchFavorite = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要收藏的记录')
    return
  }

  favoriteLoading.value = true
  let successCount = 0
  let failCount = 0

  for (const record of selectedRecords.value) {
    try {
      const response = await axios.post(`${API_BASE_URL}/favorites`,
          { recordId: record.id },
          { headers: { 'Authorization': `Bearer ${userStore.token}` } }
      )
      if (response.data.code === 200) {
        successCount++
      } else {
        failCount++
      }
    } catch (error) {
      if (error.response?.data?.message === '已收藏') {
        successCount++
      } else {
        failCount++
      }
    }
  }

  favoriteLoading.value = false
  ElMessage.success(`成功收藏 ${successCount} 条记录${failCount > 0 ? `，${failCount} 条失败` : ''}`)
  clearSelection()
  loadHistory()
}

const batchUnfavorite = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要取消收藏的记录')
    return
  }

  favoriteLoading.value = true
  let successCount = 0
  let failCount = 0

  for (const record of selectedRecords.value) {
    try {
      const response = await axios.delete(`${API_BASE_URL}/favorites`, {
        data: { recordId: record.id },
        headers: { 'Authorization': `Bearer ${userStore.token}` }
      })
      if (response.data.code === 200) {
        successCount++
      } else {
        failCount++
      }
    } catch (error) {
      failCount++
    }
  }

  favoriteLoading.value = false
  ElMessage.success(`成功取消收藏 ${successCount} 条记录${failCount > 0 ? `，${failCount} 条失败` : ''}`)
  clearSelection()
  loadHistory()
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

/* 高亮行样式 */
:deep(.history-row.highlight-row) {
  background-color: #ecf5ff !important;
  transition: background-color 0.3s;
}
</style>