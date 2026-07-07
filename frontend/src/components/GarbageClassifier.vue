<template>
  <div class="classifier-container">
    <h1>🌍 垃圾分类助手</h1>
    <p class="subtitle">上传垃圾图片，AI帮你分类</p>

    <!-- 压缩质量选择 -->
    <div class="quality-selector">
      <span class="quality-label">压缩质量：</span>
      <el-radio-group v-model="compressionQuality" size="small">
        <el-radio-button label="high">高（清晰）</el-radio-button>
        <el-radio-button label="medium">中（推荐）</el-radio-button>
        <el-radio-button label="low">低（快速）</el-radio-button>
      </el-radio-group>
    </div>

    <!-- 上传区域 -->
    <div class="upload-area">
      <!-- 单张上传 -->
      <div class="upload-box" @click="triggerFileUpload">
        <div v-if="!previewImage" class="upload-placeholder">
          <div class="upload-icon">📸</div>
          <div class="upload-text">点击上传单张图片</div>
          <div class="upload-tip">支持 JPG、PNG 格式，文件不超过 10MB</div>
        </div>
        <div v-else class="preview-container">
          <img :src="previewImage" class="preview-image" />
        </div>
      </div>
      <input
          ref="fileInput"
          type="file"
          accept="image/jpeg,image/png,image/jpg"
          style="display: none"
          @change="handleImageChange"
      />

      <!-- 批量上传 -->
      <div class="batch-upload-area">
        <el-button type="success" @click="triggerBatchUpload" :loading="batchLoading">
          📤 批量上传 (最多10张)
        </el-button>
        <input
            ref="batchFileInput"
            type="file"
            accept="image/jpeg,image/png,image/jpg"
            multiple
            style="display: none"
            @change="handleBatchImageChange"
        />
        <span v-if="batchFiles.length > 0" class="batch-count">
          已选 {{ batchFiles.length }} 张图片
          <el-button type="danger" size="small" plain @click="clearBatchFiles">清除</el-button>
        </span>
      </div>

      <!-- 批量预览 -->
      <div v-if="batchFiles.length > 0" class="batch-preview">
        <div v-for="(file, index) in batchFiles" :key="index" class="batch-item">
          <img :src="file.preview" class="batch-thumb" />
          <span class="batch-name">{{ file.name }}</span>
          <el-button type="danger" size="small" plain circle @click="removeBatchFile(index)">×</el-button>
        </div>
      </div>

      <div v-if="previewImage" class="preview-actions">
        <el-button type="primary" @click="classifyImage" :loading="loading">
          🔍 开始识别
        </el-button>
        <el-button @click="clearImage">
          🔄 重新上传
        </el-button>
      </div>

      <!-- 批量识别按钮 -->
      <div v-if="batchFiles.length > 0" class="batch-actions">
        <el-button type="primary" @click="batchClassify" :loading="batchLoading" size="large">
          🚀 批量识别 ({{ batchFiles.length }}张)
        </el-button>
        <el-button @click="clearBatchFiles" size="large">
          🔄 清空全部
        </el-button>
      </div>
    </div>

    <!-- 单张识别结果 -->
    <div v-if="result" class="result-card" :class="resultCardClass">
      <h3>📋 识别结果</h3>
      <div class="result-item">
        <span class="label">物品名称：</span>
        <span class="value">{{ result.itemName }}</span>
      </div>
      <div class="result-item">
        <span class="label">垃圾分类：</span>
        <span class="category-badge" :style="categoryStyle">
          {{ result.category }}
        </span>
      </div>
      <div class="result-item">
        <span class="label">置信度：</span>
        <span class="value">{{ (result.confidence * 100).toFixed(2) }}%</span>
        <el-progress :percentage="result.confidence * 100" :stroke-width="8" />
      </div>
      <div class="tips">
        <span class="tip-icon">💡</span>
        <span class="tip-text">{{ getTips(result.category) }}</span>
      </div>
      <div class="favorite-action">
        <el-button
            :type="isFavorited ? 'danger' : 'primary'"
            :plain="!isFavorited"
            size="small"
            @click="toggleFavorite"
            :loading="favoriteLoading"
        >
          {{ isFavorited ? '❤️ 已收藏' : '🤍 收藏' }}
        </el-button>
      </div>
    </div>

    <!-- 批量识别结果 -->
    <div v-if="batchResults.length > 0" class="batch-result-card">
      <h3>📊 批量识别结果</h3>
      <div class="batch-stats">
        <span>总数: {{ batchResults.length }}</span>
        <span style="color: #67C23A;">成功: {{ batchResults.filter(r => r.success).length }}</span>
        <span style="color: #F56C6C;">失败: {{ batchResults.filter(r => !r.success).length }}</span>
      </div>
      <el-table :data="batchResults" stripe size="small" style="width: 100%; margin-top: 12px;">
        <el-table-column prop="fileName" label="文件名" width="150" />
        <el-table-column prop="itemName" label="物品名称" width="120" />
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.success" :type="getTagType(row.category)" size="small">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="confidence" label="置信度" width="120">
          <template #default="{ row }">
            <span v-if="row.success">{{ (row.confidence * 100).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="errorMsg" label="状态" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.success" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">{{ row.errorMsg || '失败' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-button type="primary" size="small" @click="clearBatchResults" style="margin-top: 12px;">
        清除结果
      </el-button>
    </div>

    <!-- 最近识别记录（完善版） -->
    <div v-if="recentRecords.length > 0" class="recent-records">
      <div class="recent-header">
        <h3>📊 最近识别</h3>
        <div class="recent-actions">
          <el-button
              type="danger"
              size="small"
              plain
              @click="batchDeleteRecent"
              :disabled="selectedRecentRecords.length === 0"
          >
            🗑️ 删除选中 ({{ selectedRecentRecords.length }})
          </el-button>
          <el-button
              type="danger"
              size="small"
              plain
              @click="clearAllRecent"
          >
            🗑️ 清空全部
          </el-button>
        </div>
      </div>
      <div class="record-list">
        <div
            v-for="record in recentRecords"
            :key="record.id"
            class="record-item"
            :class="{ 'selected': selectedRecentRecords.includes(record.id) }"
            @click="toggleRecentSelection(record.id)"
        >
          <el-checkbox
              :model-value="selectedRecentRecords.includes(record.id)"
              @click.stop
              @change="toggleRecentSelection(record.id)"
          />
          <el-image
              v-if="record.imageUrl"
              :src="`http://192.168.58.128:8080${record.imageUrl}`"
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 4px; cursor: pointer;"
              :preview-src-list="[`http://192.168.58.128:8080${record.imageUrl}`]"
              :preview-teleported="true"
              @click.stop
          />
          <span v-else style="width: 40px; height: 40px; background: #f0f0f0; border-radius: 4px; display: inline-block;"></span>
          <span class="record-name" @click="goToHistory(record.id)">{{ record.itemName }}</span>
          <el-tag :type="getTagType(record.category)" size="small">
            {{ record.category }}
          </el-tag>
          <span class="record-confidence">{{ (record.confidence * 100).toFixed(1) }}%</span>
          <span class="record-time">{{ formatTime(record.createdAt) }}</span>
          <el-button
              type="primary"
              size="small"
              plain
              @click.stop="goToHistory(record.id)"
          >
            查看详情
          </el-button>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>AI正在识别中，请稍候...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'
import { useUserStore } from '../store/userStore'

const router = useRouter()
const previewImage = ref(null)
const imageFile = ref(null)
const loading = ref(false)
const result = ref(null)
const fileInput = ref(null)
const recentRecords = ref([])
const isFavorited = ref(false)
const favoriteLoading = ref(false)
const selectedRecentRecords = ref([])

// 压缩质量
const compressionQuality = ref('medium')

// 批量上传
const batchFileInput = ref(null)
const batchFiles = ref([])
const batchLoading = ref(false)
const batchResults = ref([])

const userStore = useUserStore()
const API_BASE_URL = 'http://192.168.58.128:8080/api'

const triggerFileUpload = () => {
  fileInput.value.click()
}

// 批量上传触发
const triggerBatchUpload = () => {
  batchFileInput.value.click()
}

const handleBatchImageChange = (event) => {
  const files = event.target.files
  if (files.length === 0) return

  if (batchFiles.value.length + files.length > 10) {
    ElMessage.warning('一次最多上传10张图片')
    return
  }

  for (const file of files) {
    if (file.size > 10 * 1024 * 1024) {
      ElMessage.warning(`文件 ${file.name} 超过10MB`)
      continue
    }
    if (!file.type.startsWith('image/')) {
      ElMessage.warning(`文件 ${file.name} 不是图片`)
      continue
    }
    const reader = new FileReader()
    reader.onload = (e) => {
      batchFiles.value.push({
        file: file,
        name: file.name,
        preview: e.target.result
      })
    }
    reader.readAsDataURL(file)
  }
  event.target.value = ''
}

const removeBatchFile = (index) => {
  batchFiles.value.splice(index, 1)
}

const clearBatchFiles = () => {
  batchFiles.value = []
  batchResults.value = []
}

// 批量识别
const batchClassify = async () => {
  if (batchFiles.value.length === 0) {
    ElMessage.warning('请先选择图片')
    return
  }

  batchLoading.value = true
  const formData = new FormData()
  for (const item of batchFiles.value) {
    formData.append('files', item.file)
  }

  try {
    const response = await axios.post(`${API_BASE_URL}/classify/batch`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${userStore.token}`
      },
      timeout: 60000
    })

    if (response.data.code === 200) {
      batchResults.value = response.data.data.results || []
      ElMessage.success(`识别完成！成功 ${response.data.data.successCount} 张`)
      loadRecentRecords()
    } else {
      ElMessage.error(response.data.message || '批量识别失败')
    }
  } catch (error) {
    console.error('批量识别错误:', error)
    ElMessage.error('批量识别失败，请稍后重试')
  } finally {
    batchLoading.value = false
  }
}

const clearBatchResults = () => {
  batchResults.value = []
}

const handleImageChange = (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('图片不能超过10MB')
    return
  }

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  imageFile.value = file
  const reader = new FileReader()
  reader.onload = (e) => {
    previewImage.value = e.target.result
  }
  reader.readAsDataURL(file)
  result.value = null
  event.target.value = ''
}

const clearImage = () => {
  previewImage.value = null
  imageFile.value = null
  result.value = null
}

const checkFavorite = async (recordId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/favorites/check/${recordId}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      isFavorited.value = response.data.data.isFavorited || false
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

const toggleFavorite = async () => {
  if (!result.value) return
  favoriteLoading.value = true
  try {
    const method = isFavorited.value ? 'delete' : 'post'
    const response = await axios({
      method: method,
      url: `${API_BASE_URL}/favorites`,
      data: { recordId: result.value.id },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      isFavorited.value = !isFavorited.value
      ElMessage.success(isFavorited.value ? '收藏成功！' : '已取消收藏')
    } else {
      ElMessage.error(response.data.message || '操作失败')
    }
  } catch (error) {
    console.error('收藏操作失败:', error)
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    favoriteLoading.value = false
  }
}

const classifyImage = async () => {
  if (!imageFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }

  loading.value = true
  const formData = new FormData()
  formData.append('file', imageFile.value)

  try {
    const response = await axios.post(`${API_BASE_URL}/classify`, formData, {
      params: { quality: compressionQuality.value },
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${userStore.token}`
      },
      timeout: 30000
    })

    if (response.data.code === 200) {
      result.value = response.data.data
      if (result.value.id) {
        await checkFavorite(result.value.id)
      }
      ElMessage.success('识别成功！')
      loadRecentRecords()
    } else {
      ElMessage.error(response.data.message || '识别失败')
    }
  } catch (error) {
    console.error('识别错误:', error)
    ElMessage.error('网络错误，请检查后端服务是否启动')
  } finally {
    loading.value = false
  }
}

// ===== 最近识别相关方法 =====

const loadRecentRecords = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/history/recent`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      recentRecords.value = response.data.data || []
      selectedRecentRecords.value = []
    }
  } catch (error) {
    console.error('加载最近记录失败:', error)
  }
}

const toggleRecentSelection = (id) => {
  const index = selectedRecentRecords.value.indexOf(id)
  if (index > -1) {
    selectedRecentRecords.value.splice(index, 1)
  } else {
    selectedRecentRecords.value.push(id)
  }
}

const goToHistory = (id) => {
  // 跳转到识别历史页面，并传递记录ID
  router.push({
    path: '/history',
    query: { highlight: id }
  })
}

// 批量删除最近记录
const batchDeleteRecent = async () => {
  if (selectedRecentRecords.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRecentRecords.value.length} 条记录吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    const response = await axios.delete(`${API_BASE_URL}/history/batch`, {
      data: { ids: selectedRecentRecords.value },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success(`成功删除 ${selectedRecentRecords.value.length} 条记录！`)
      loadRecentRecords()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除失败:', error)
    ElMessage.error('删除失败，请稍后重试')
  }
}

// 清空全部最近记录
const clearAllRecent = async () => {
  if (recentRecords.value.length === 0) {
    ElMessage.warning('没有记录可删除')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要清空所有 ${recentRecords.value.length} 条最近识别记录吗？此操作不可恢复！`,
        '确认清空',
        { confirmButtonText: '确定清空', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    const ids = recentRecords.value.map(r => r.id)
    const response = await axios.delete(`${API_BASE_URL}/history/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success(`成功清空 ${ids.length} 条记录！`)
      loadRecentRecords()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('清空失败:', error)
    ElMessage.error('清空失败，请稍后重试')
  }
}

const getTagType = (category) => {
  const map = {
    '可回收物': 'primary',
    '有害垃圾': 'danger',
    '厨余垃圾': 'success',
    '其他垃圾': 'info'
  }
  return map[category] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const resultCardClass = () => {
  if (!result.value) return ''
  const category = result.value.category
  const map = {
    '可回收物': 'recyclable',
    '有害垃圾': 'hazardous',
    '厨余垃圾': 'kitchen',
    '其他垃圾': 'other'
  }
  return map[category] || ''
}

const categoryStyle = () => {
  if (!result.value) return {}
  const colors = {
    '可回收物': { backgroundColor: '#2563eb' },
    '有害垃圾': { backgroundColor: '#dc2626' },
    '厨余垃圾': { backgroundColor: '#16a34a' },
    '其他垃圾': { backgroundColor: '#6b7280' }
  }
  return colors[result.value.category] || { backgroundColor: '#6b7280' }
}

const getTips = (category) => {
  const tips = {
    '可回收物': '请清空容器、洗净后投放至蓝色回收箱 ♻️',
    '有害垃圾': '请小心投放至红色有害垃圾桶，避免破损 ⚠️',
    '厨余垃圾': '请沥干水分后投放至绿色厨余垃圾桶 🍽️',
    '其他垃圾': '请投放至灰色其他垃圾桶 🗑️'
  }
  return tips[category] || '请按照当地垃圾分类标准投放'
}

onMounted(() => {
  loadRecentRecords()
})
</script>

<style scoped>
.classifier-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 20px;
}

h1 {
  text-align: center;
  color: #333;
  margin-bottom: 10px;
}

.subtitle {
  text-align: center;
  color: #666;
  margin-bottom: 20px;
}

.quality-selector {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.quality-label {
  font-weight: 500;
  color: #333;
}

.upload-area {
  margin-bottom: 30px;
}

.upload-box {
  border: 2px dashed #ddd;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
  background: #fafafa;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-box:hover {
  border-color: #409EFF;
  background: #f0f7ff;
}

.upload-placeholder {
  text-align: center;
  padding: 40px;
}

.upload-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.upload-text {
  font-size: 16px;
  color: #666;
  margin-bottom: 8px;
}

.upload-tip {
  font-size: 12px;
  color: #999;
}

.preview-container {
  text-align: center;
  padding: 20px;
}

.preview-image {
  max-width: 100%;
  max-height: 300px;
  border-radius: 8px;
}

.batch-upload-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 12px;
  background: #f0f9eb;
  border-radius: 8px;
  flex-wrap: wrap;
}

.batch-count {
  color: #666;
  font-size: 14px;
}

.batch-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.batch-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  position: relative;
  width: 80px;
}

.batch-thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
}

.batch-name {
  font-size: 10px;
  color: #666;
  text-align: center;
  word-break: break-all;
  max-width: 70px;
}

.batch-item .el-button {
  position: absolute;
  top: -8px;
  right: -8px;
  padding: 2px 6px;
  font-size: 12px;
}

.batch-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 16px;
}

.preview-actions {
  display: flex;
  gap: 20px;
  justify-content: center;
  margin-top: 20px;
}

.result-card {
  margin-top: 30px;
  padding: 20px;
  border-radius: 12px;
  background: #f5f7fa;
  border-left: 4px solid #409EFF;
}

.result-card h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
}

.result-card.recyclable {
  border-left-color: #2563eb;
  background: #eff6ff;
}

.result-card.hazardous {
  border-left-color: #dc2626;
  background: #fef2f2;
}

.result-card.kitchen {
  border-left-color: #16a34a;
  background: #f0fdf4;
}

.result-card.other {
  border-left-color: #6b7280;
  background: #f3f4f6;
}

.result-item {
  margin-bottom: 15px;
}

.label {
  font-weight: bold;
  color: #333;
  font-size: 15px;
  display: inline-block;
  width: 80px;
}

.value {
  color: #222;
  font-size: 15px;
  font-weight: 500;
}

.category-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 24px;
  color: white !important;
  font-size: 16px;
  font-weight: bold;
  text-shadow: 1px 1px 2px rgba(0,0,0,0.2);
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.tips {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip-icon {
  font-size: 20px;
}

.tip-text {
  color: #606266;
  font-size: 13px;
}

.favorite-action {
  margin-top: 15px;
  text-align: center;
}

.batch-result-card {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
}

.batch-result-card h3 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.batch-stats {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

/* 最近识别样式 */
.recent-records {
  margin-top: 30px;
  padding: 20px;
  background: #f5f7fa;
  border-radius: 12px;
}

.recent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.recent-header h3 {
  margin: 0;
  color: #333;
}

.recent-actions {
  display: flex;
  gap: 8px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 400px;
  overflow-y: auto;
}

.record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.record-item:hover {
  background: #f0f7ff;
  box-shadow: 0 2px 8px rgba(64,158,255,0.15);
}

.record-item.selected {
  background: #ecf5ff;
  border: 1px solid #409EFF;
}

.record-item .el-checkbox {
  margin-right: 0;
}

.record-name {
  font-weight: 500;
  flex: 1;
  cursor: pointer;
  color: #409EFF;
}

.record-name:hover {
  text-decoration: underline;
}

.record-confidence {
  color: #67C23A;
  font-weight: 600;
  font-size: 13px;
  min-width: 60px;
}

.record-time {
  color: #999;
  font-size: 12px;
  min-width: 80px;
}

.loading-container {
  text-align: center;
  padding: 40px;
  color: #409EFF;
}
</style>