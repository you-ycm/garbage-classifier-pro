<template>
  <div class="profile-container">
    <h2>👤 个人信息与安全</h2>
    <el-card class="password-card">
      <template #header>
        <span>修改密码</span>
      </template>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleChangePassword">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/userStore'
import axios from 'axios'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const formRef = ref(null)
const API_BASE_URL = 'http://localhost:8080/api'

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== form.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [{ validator: validatePass2, trigger: 'blur' }]
}

const handleChangePassword = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/change-password`, {
          username: userStore.username,
          oldPassword: form.oldPassword,
          newPassword: form.newPassword
        }, {
          headers: { 'Authorization': `Bearer ${userStore.token}` }
        })
        if (response.data.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          userStore.logout()
          router.push('/login')
        } else {
          ElMessage.error(response.data.message || '修改失败')
        }
      } catch (error) {
        ElMessage.error('网络错误，请检查后端服务')
      }
    }
  })
}
</script>

<style scoped>
.profile-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 40px 20px;
}
.password-card {
  margin-top: 20px;
}
</style>