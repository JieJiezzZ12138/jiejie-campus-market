<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-logo">
        <el-icon size="45" color="#409EFF"><ShoppingCartFull /></el-icon>
        <h1>校园二手交易平台</h1>
        <p>Campus Second-hand Market System</p>
      </div>

      <el-form :model="loginForm" :rules="rules" ref="loginFormRef">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="学号 / 管理员账号" 
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="登录密码" 
            :prefix-icon="Lock" 
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="login-options">
          <el-checkbox v-model="loginForm.remember">记住登录状态</el-checkbox>
          <el-link type="primary" :underline="false" @click="ElMessage.info('请联系系统管理员 @JieJie 重置')">忘记密码？</el-link>
        </div>

        <el-button 
          type="primary" 
          class="submit-btn" 
          :loading="loading" 
          @click="handleLogin"
        >
          安全登录
        </el-button>
      </el-form>

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
const loginFormRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
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

// 👉 核心：修正后的登录逻辑
const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      // 这里的 res 拿到的就是后端 Result 里的 data 对象
      const res = await request.post('/auth/login', {
        username: loginForm.username,
        password: loginForm.password
      })

      handleRememberMe()

      // 🔍 根据你提供的 JSON 结构进行精准提取
      const token = res.token
      const userInfo = res.userInfo
      const role = userInfo.role      // 从 userInfo 里取 role
      const nickname = userInfo.nickname // 从 userInfo 里取 nickname

      // 1. 存储通行证
      localStorage.setItem('token', token)
      localStorage.setItem('user_role', role) 
      localStorage.setItem('userInfo', JSON.stringify(userInfo))

      // 2. 根据角色分流跳转
      if (role === 'ADMIN') {
        ElMessage.success(`欢迎回来，管理员 ${nickname}`)
        router.push('/admin') 
      } else {
        ElMessage.success(`登录成功，欢迎来到集市，${nickname}`)
        router.push('/') // 普通学生跳转首页
      }
      
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
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f5f7f9;
  background-image: radial-gradient(#d2d9e0 1px, transparent 1px);
  background-size: 25px 25px;
}
.login-box {
  width: 380px;
  padding: 45px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.08);
}
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