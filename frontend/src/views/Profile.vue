<template>
  <div class="profile-container">
    <h2>👤 个人信息与安全</h2>

    <!-- 用户信息卡片 -->
    <el-card class="info-card">
      <template #header>
        <span>个人信息</span>
      </template>
      <div class="info-item">
        <span class="label">用户名：</span>
        <span class="value" v-if="!showEditUsername">{{ userStore.username }}</span>
        <el-input
            v-else
            v-model="editUsername"
            size="small"
            style="width: 200px;"
            placeholder="请输入新用户名"
        />
        <el-button size="small" type="primary" @click="toggleEditUsername">
          {{ showEditUsername ? '取消' : '修改' }}
        </el-button>
        <el-button
            v-if="showEditUsername"
            size="small"
            type="success"
            @click="handleUpdateUsername"
            :loading="usernameLoading"
        >
          保存
        </el-button>
      </div>
      <div class="info-item">
        <span class="label">角色：</span>
        <el-tag :type="userStore.role === 'ROLE_ADMIN' ? 'danger' : 'success'">
          {{ userStore.role === 'ROLE_ADMIN' ? '管理员' : '普通用户' }}
        </el-tag>
      </div>
      <div class="info-item">
        <span class="label">邮箱：</span>
        <span class="value">{{ userStore.email || '未绑定' }}</span>
        <el-button size="small" type="primary" @click="showBindEmail = !showBindEmail">
          {{ userStore.email ? '修改' : '绑定' }}
        </el-button>
      </div>
    </el-card>

    <!-- 绑定邮箱 -->
    <el-card class="bind-card" v-if="showBindEmail">
      <template #header>
        <span>📧 {{ userStore.email ? '修改邮箱' : '绑定邮箱' }}</span>
      </template>
      <el-form @submit.prevent="handleBindEmail">
        <el-form-item label="邮箱">
          <el-input v-model="bindEmail" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleBindEmail" :loading="bindLoading">保存</el-button>
          <el-button @click="showBindEmail = false">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码 -->
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
const showBindEmail = ref(false)
const bindEmail = ref('')
const bindLoading = ref(false)

// 修改用户名相关
const showEditUsername = ref(false)
const editUsername = ref('')
const usernameLoading = ref(false)

const API_BASE_URL = 'http://192.168.58.128:8080/api'

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

// ===== 修改用户名 =====
const toggleEditUsername = () => {
  if (showEditUsername.value) {
    // 取消编辑
    showEditUsername.value = false
    editUsername.value = ''
  } else {
    // 开启编辑
    editUsername.value = userStore.username
    showEditUsername.value = true
  }
}

const handleUpdateUsername = async () => {
  const newUsername = editUsername.value.trim()
  if (!newUsername) {
    ElMessage.warning('用户名不能为空')
    return
  }
  if (newUsername === userStore.username) {
    ElMessage.info('用户名未变更')
    showEditUsername.value = false
    return
  }
  if (newUsername.length < 2 || newUsername.length > 20) {
    ElMessage.warning('用户名长度应在 2-20 个字符之间')
    return
  }

  usernameLoading.value = true
  try {
    const response = await axios.put(`${API_BASE_URL}/auth/update-username`, {
      username: userStore.username,
      newUsername: newUsername
    }, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('用户名修改成功，请重新登录')
      // 更新本地存储的用户名
      userStore.setUserInfo(
          userStore.token,
          userStore.role,
          newUsername,
          userStore.email
      )
      showEditUsername.value = false
      // 可选：刷新页面或提示重新登录
      setTimeout(() => {
        userStore.logout()
        router.push('/login')
      }, 1500)
    } else {
      ElMessage.error(response.data.message || '修改失败')
    }
  } catch (error) {
    console.error('修改用户名错误:', error)
    if (error.response && error.response.data) {
      ElMessage.error(error.response.data.message || '修改失败，请稍后重试')
    } else {
      ElMessage.error('网络错误，请检查后端服务')
    }
  } finally {
    usernameLoading.value = false
  }
}

// ===== 绑定邮箱 =====
const handleBindEmail = async () => {
  if (!bindEmail.value) {
    ElMessage.warning('请输入邮箱')
    return
  }
  bindLoading.value = true
  try {
    const response = await axios.post(`${API_BASE_URL}/auth/bind-email`, {
      username: userStore.username,
      email: bindEmail.value
    }, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      ElMessage.success('邮箱绑定成功')
      userStore.setUserInfo(
          userStore.token,
          userStore.role,
          userStore.username,
          bindEmail.value
      )
      showBindEmail.value = false
    } else {
      ElMessage.error(response.data.message || '绑定失败')
    }
  } catch (error) {
    console.error('绑定邮箱错误:', error)
    ElMessage.error('网络错误，请检查后端服务')
  } finally {
    bindLoading.value = false
  }
}

// ===== 修改密码 =====
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
        console.error('修改密码错误:', error)
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
.info-card, .password-card, .bind-card {
  margin-top: 20px;
}
.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
  flex-wrap: wrap;
}
.info-item .label {
  font-weight: bold;
  color: #333;
  width: 80px;
  flex-shrink: 0;
}
.info-item .value {
  color: #666;
}
</style>