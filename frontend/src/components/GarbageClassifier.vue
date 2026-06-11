<template>
  <div class="classifier-container">
    <h1>🌍 垃圾分类助手</h1>
    <p class="subtitle">上传垃圾图片，AI帮你分类</p>

    <!-- 上传区域 -->
    <div class="upload-area">
      <div class="upload-box" @click="triggerFileUpload">
        <div v-if="!previewImage" class="upload-placeholder">
          <div class="upload-icon">📸</div>
          <div class="upload-text">点击上传图片</div>
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

      <div v-if="previewImage" class="preview-actions">
        <el-button type="primary" @click="classifyImage" :loading="loading">
          🔍 开始识别
        </el-button>
        <el-button @click="clearImage">
          🔄 重新上传
        </el-button>
      </div>
    </div>

    <!-- 识别结果 -->
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
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>AI正在识别中，请稍候...</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import axios from 'axios'
import { useUserStore } from '../store/userStore' // 【新增：引入 userStore】

const previewImage = ref(null)
const imageFile = ref(null)
const loading = ref(false)
const result = ref(null)
const fileInput = ref(null)

const userStore = useUserStore() // 【新增：获取用户状态】
const API_BASE_URL = 'http://localhost:8080/api'

const triggerFileUpload = () => {
  fileInput.value.click()
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
      headers: {
        'Content-Type': 'multipart/form-data',
        'Authorization': `Bearer ${userStore.token}` // 【新增：添加 Token】
      },
      timeout: 30000
    })

    if (response.data.code === 200) {
      result.value = response.data.data
      ElMessage.success('识别成功！')
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
  margin-bottom: 40px;
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

.loading-container {
  text-align: center;
  padding: 40px;
  color: #409EFF;
}
</style>