<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo">
        <span class="icon">🌍</span>
        <h2>垃圾分类助手</h2>
      </div>
      <p class="slogan">AI智能识别，环保从我做起</p>
      <h3>用户登录</h3>
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>用户名 / 邮箱</label>
          <input v-model="form.username" type="text" placeholder="请输入用户名或邮箱" required />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input v-model="form.password" type="password" placeholder="请输入密码" required />
        </div>
        <button type="submit" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <div class="links">
          <span class="register-link" @click="goToRegister">还没有账号？立即注册</span>
          <span class="forgot-link" @click="goToForgotPassword">忘记密码？</span>
        </div>
        <div v-if="error" class="error">{{ error }}</div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../store/userStore'

const BASE_URL = 'http://192.168.58.128:8080/api'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const error = ref('')

const form = ref({
  username: '',
  password: ''
})

const goToRegister = () => {
  router.push('/register')
}

const goToForgotPassword = () => {
  router.push('/forgot-password')
}

const handleLogin = async () => {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch(`${BASE_URL}/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(form.value)
    })

    const data = await response.json()

    // 🟢【核心修改】判断后端返回的 code 是否为 200
    if (data && data.code === 200) {
      userStore.setUserInfo(data.token, data.role, data.username, data.email || '')
      router.push('/')
    } else {
      // 如果后端返回了自定义的 message，直接显示
      error.value = data.message || '登录失败，请检查用户名/邮箱和密码'
    }
  } catch (err) {
    console.error('登录错误:', err)
    error.value = '网络错误，请检查后端是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
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
.slogan {
  text-align: center;
  color: #667eea;
  font-size: 14px;
  margin-bottom: 20px;
  font-weight: 500;
  letter-spacing: 1px;
}
h3 {
  text-align: center;
  color: #666;
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
.links {
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
  font-size: 14px;
}
.register-link {
  color: #409EFF;
  cursor: pointer;
}
.register-link:hover {
  text-decoration: underline;
}
.forgot-link {
  color: #909399;
  cursor: pointer;
}
.forgot-link:hover {
  text-decoration: underline;
}
.error {
  color: red;
  margin-top: 10px;
  text-align: center;
}
</style>