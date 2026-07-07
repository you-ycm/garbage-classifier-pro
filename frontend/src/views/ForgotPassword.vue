<template>
  <div class="forgot-container">
    <div class="forgot-card">
      <div class="logo">
        <span class="icon">🔑</span>
        <h2>找回密码</h2>
      </div>
      <p class="subtitle">输入你绑定的邮箱，我们将发送重置链接</p>

      <form @submit.prevent="handleSendEmail">
        <div class="form-group">
          <label>邮箱</label>
          <input v-model="email" type="email" placeholder="请输入绑定的邮箱" required />
        </div>
        <button type="submit" :disabled="loading">{{ loading ? '发送中...' : '发送重置邮件' }}</button>
        <div class="back-link" @click="goToLogin">← 返回登录</div>
        <div v-if="message" class="message" :class="messageType">{{ message }}</div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const BASE_URL = 'http://192.168.58.128:8080/api'

const router = useRouter()
const email = ref('')
const loading = ref(false)
const message = ref('')
const messageType = ref('')

const goToLogin = () => {
  router.push('/login')
}

const handleSendEmail = async () => {
  if (!email.value) {
    message.value = '请输入邮箱'
    messageType.value = 'error'
    return
  }

  loading.value = true
  message.value = ''

  try {
    const response = await fetch(`${BASE_URL}/auth/forgot-password`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ email: email.value })
    })

    const data = await response.json()

    if (response.ok) {
      message.value = '重置邮件已发送，请检查邮箱'
      messageType.value = 'success'
    } else {
      message.value = data.message || '发送失败，请检查邮箱是否正确'
      messageType.value = 'error'
    }
  } catch (err) {
    console.error('发送邮件错误:', err)
    message.value = '网络错误，请稍后重试'
    messageType.value = 'error'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.forgot-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.forgot-card {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
.logo {
  text-align: center;
  margin-bottom: 10px;
}
.logo .icon {
  font-size: 32px;
}
.logo h2 {
  margin: 0;
  color: #333;
}
.subtitle {
  text-align: center;
  color: #666;
  font-size: 14px;
  margin-bottom: 30px;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  margin-bottom: 5px;
  color: #666;
}
.form-group input {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 5px;
  font-size: 16px;
}
button {
  width: 100%;
  padding: 12px;
  background: #667eea;
  color: white;
  border: none;
  border-radius: 5px;
  font-size: 16px;
  cursor: pointer;
}
button:hover {
  background: #5a6fd6;
}
button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
.back-link {
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
  color: #409EFF;
  cursor: pointer;
}
.back-link:hover {
  text-decoration: underline;
}
.message {
  margin-top: 15px;
  padding: 10px;
  border-radius: 5px;
  text-align: center;
  font-size: 14px;
}
.message.success {
  background: #f0f9eb;
  color: #67c23a;
}
.message.error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>