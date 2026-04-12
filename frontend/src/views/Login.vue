<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-logo">
        <el-icon size="45" color="#409EFF"><ShoppingCartFull /></el-icon>
        <h1>校园二手交易平台</h1>
        <p>Campus Second-hand Market System</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="学号 / 管理员账号" :prefix-icon="User" size="large" />
            </el-form-item>

            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="登录密码" :prefix-icon="Lock" show-password size="large" @keyup.enter="handleLogin" />
            </el-form-item>

            <div class="login-options">
              <el-checkbox v-model="loginForm.remember">记住登录状态</el-checkbox>
              <el-link type="primary" underline="never" @click="ElMessage.info('请联系系统管理员 @JieJie 重置')">忘记密码？</el-link>
            </div>

            <el-button type="primary" class="submit-btn" :loading="loading" @click="handleLogin">安全登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="设置登录账号（学号或自定义）" :prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（不填则与账号相同）" :prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="phone">
              <el-input v-model="registerForm.phone" placeholder="手机号（必填）" size="large" />
            </el-form-item>
            <el-form-item prop="campusAddress">
              <el-input v-model="registerForm.campusAddress" placeholder="常用面交地址（可选）" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码（至少 6 位）" :prefix-icon="Lock" show-password size="large" />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" :prefix-icon="Lock" show-password size="large" @keyup.enter="handleRegister" />
            </el-form-item>
            <el-button type="primary" class="submit-btn" :loading="registerLoading" @click="handleRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <div class="login-footer">
        <p class="copyright">© 2026 <strong>@JieJie</strong> · All Rights Reserved</p>
        <p class="project-tag">校园二手交易平台 | 个人作品集项目</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, ShoppingCartFull } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref(null)
const registerFormRef = ref(null)
const activeTab = ref('login')

const loginForm = reactive({ username: '', password: '', remember: false })

const registerForm = reactive({
  username: '',
  nickname: '',
  phone: '',
  campusAddress: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^\\d{7,20}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

onMounted(() => {
  const savedUser = localStorage.getItem('jiejie_saved_user')
  if (savedUser) {
    try {
      const { username, password } = JSON.parse(savedUser)
      loginForm.username = username
      loginForm.password = password
      loginForm.remember = true
    } catch (e) {
      localStorage.removeItem('jiejie_saved_user')
    }
  }
})

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      // 发送 JSON 格式数据
      const res = await request.post('/user/login', {
        username: loginForm.username,
        password: loginForm.password
      })

      handleRememberMe()
      persistSession(res.token, res.userInfo)
    } catch (error) {
      console.error("登录链路异常:", error)
    } finally {
      loading.value = false
    }
  })
}

const handleRememberMe = () => {
  if (loginForm.remember) {
    localStorage.setItem('jiejie_saved_user', JSON.stringify({
      username: loginForm.username,
      password: loginForm.password
    }))
  } else {
    localStorage.removeItem('jiejie_saved_user')
  }
}

const persistSession = (token, userInfo) => {
  const role = userInfo.role
  const nickname = userInfo.nickname
  localStorage.setItem('token', token)
  localStorage.setItem('user_role', role)
  localStorage.setItem('userInfo', JSON.stringify(userInfo))
  if (role === 'ADMIN') {
    ElMessage.success(`欢迎回来，管理员 ${nickname}`)
    router.push('/admin')
  } else {
    ElMessage.success(`登录成功，欢迎来到集市，${nickname}`)
    router.push('/')
  }
}

const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    registerLoading.value = true
    try {
      const res = await request.post('/user/register', {
        username: registerForm.username.trim(),
        password: registerForm.password,
        nickname: registerForm.nickname?.trim() || undefined,
        phone: registerForm.phone?.trim() || undefined,
        campusAddress: registerForm.campusAddress?.trim() || undefined
      })
      persistSession(res.token, res.userInfo)
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      registerLoading.value = false
    }
  })
}
</script>

<style scoped>
.login-wrapper { height: 100vh; display: flex; justify-content: center; align-items: center; background-color: #f5f7f9; background-image: radial-gradient(#d2d9e0 1px, transparent 1px); background-size: 25px 25px; }
.login-box { width: 400px; padding: 45px; background: #fff; border-radius: 12px; box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08); }
.login-tabs { margin-top: 8px; }
.login-tabs :deep(.el-tabs__header) { margin-bottom: 22px; }
.login-tabs :deep(.el-tabs__item) { font-size: 15px; }
.login-logo { text-align: center; margin-bottom: 35px; }
.login-logo h1 { font-size: 24px; color: #2c3e50; margin: 15px 0 8px; font-weight: 600; }
.login-logo p { font-size: 11px; color: #a0a0a0; text-transform: uppercase; letter-spacing: 1.5px; }
.login-options { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.submit-btn { width: 100%; height: 44px; font-size: 16px; letter-spacing: 1px; border-radius: 6px; }
.login-footer { margin-top: 40px; text-align: center; border-top: 1px solid #f0f2f5; padding-top: 20px; }
.copyright { color: #909399; font-size: 13px; margin: 0; }
.copyright strong { color: #409EFF; }
.project-tag { color: #bdc3c7; font-size: 11px; margin: 6px 0 0; }
</style>
