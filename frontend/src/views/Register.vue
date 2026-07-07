<template>
  <div class="register-container">
    <div class="register-card">
      <h2>🌍 垃圾分类助手</h2>
      <h3>注册新账号</h3>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="0">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" prefix-icon="Lock" show-password />
        </el-form-item>
        <!-- 🟢【新增】邮箱选填 -->
        <el-form-item prop="email">
          <el-input v-model="form.email" placeholder="邮箱 (选填，用于找回密码)" prefix-icon="Message" />
        </el-form-item>
        <el-button type="primary" @click="handleRegister" :loading="loading" class="register-btn">注册</el-button>
        <div class="login-link">
          已有账号？<span @click="router.push('/login')">去登录</span>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const API_BASE_URL = 'http://192.168.58.128:8080/api'

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  email: '' // 🟢 新增邮箱字段
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }],
  // 🟢 邮箱选填，仅做格式验证，不做必填校验
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 🟢 将邮箱字段一起发送给后端
        const response = await axios.post(`${API_BASE_URL}/auth/register`, {
          username: form.username,
          password: form.password,
          email: form.email || null // 如果为空字符串，则传 null
        })
        if (response.data.code === 200) {
          ElMessage.success('注册成功，请登录')
          router.push('/login')
        } else {
          ElMessage.error(response.data.message || '注册失败')
        }
      } catch (error) {
        console.error('注册错误:', error)
        ElMessage.error('注册失败，用户名可能已存在或后端服务异常')
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  background: white;
  padding: 40px;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
  width: 100%;
  max-width: 400px;
}
h2, h3 {
  text-align: center;
  color: #333;
  margin-bottom: 10px;
}
.register-btn {
  width: 100%;
  margin-top: 20px;
}
.login-link {
  text-align: center;
  margin-top: 15px;
  font-size: 14px;
  color: #666;
}
.login-link span {
  color: #409EFF;
  cursor: pointer;
}
</style>