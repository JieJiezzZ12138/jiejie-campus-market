<template>
  <div class="login-wrapper">
    <div class="login-box">
      <div class="login-logo">
        <el-icon size="45" color="#409EFF"><ShoppingCartFull /></el-icon>
        <h1>杰物 Jemall 电商平台</h1>
        <p>Jemall E-commerce Platform</p>
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
              <el-link type="primary" underline="never" @click="openResetDialog">忘记密码？</el-link>
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
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱（必填，用于接收验证码）" size="large" />
            </el-form-item>
            <el-form-item prop="phoneCode">
              <el-input v-model="registerForm.phoneCode" placeholder="邮箱验证码" size="large">
                <template #append>
                  <el-button :disabled="emailSending || emailCountdown > 0" @click="sendRegisterCode">
                    {{ emailCountdown > 0 ? `${emailCountdown}s` : '发送验证码' }}
                  </el-button>
                </template>
              </el-input>
            </el-form-item>
            <el-form-item prop="campusAddress">
              <el-input v-model="registerForm.campusAddress" placeholder="常用收货地址（可选）" size="large" />
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
        <p class="copyright">© 2026 <strong>Jemall</strong> · All Rights Reserved</p>
        <p class="project-tag">杰物电商平台 | Web 开发课程大作业</p>
      </div>
    </div>
    <el-dialog v-model="resetVisible" title="找回密码" width="460px">
      <el-form :model="resetForm">
        <el-form-item label="找回方式" label-width="80">
          <el-radio-group v-model="resetForm.mode" @change="onResetModeChange">
            <el-radio label="phone">手机号</el-radio>
            <el-radio label="email">邮箱</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="resetForm.mode === 'email' ? '邮箱' : '手机号'" label-width="80">
          <el-input
            v-model="resetForm.account"
            :placeholder="resetForm.mode === 'email' ? '请输入已注册邮箱' : '请输入已注册手机号'"
          >
            <template #append>
              <el-button :disabled="resetSending || resetCountdown > 0" @click="sendResetCode">
                {{ resetCountdown > 0 ? `${resetCountdown}s` : '发送验证码' }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" label-width="80"><el-input v-model="resetForm.phoneCode" /></el-form-item>
        <el-form-item label="新密码" label-width="80"><el-input v-model="resetForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible=false">取消</el-button>
        <el-button type="primary" :loading="resetSubmitting" @click="submitReset">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
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
  email: '',
  phoneCode: '',
  campusAddress: '',
  password: '',
  confirmPassword: ''
})
const resetVisible = ref(false)
const resetForm = reactive({ mode: 'phone', account: '', phoneCode: '', newPassword: '' })
const emailSending = ref(false)
const emailCountdown = ref(0)
const resetSending = ref(false)
const resetCountdown = ref(0)
const resetSubmitting = ref(false)

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
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/, message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phoneCode: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
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
  const savedUser = localStorage.getItem('jemall_saved_user') || localStorage.getItem('jiejie_saved_user')
  if (savedUser) {
    try {
      const { username, password } = JSON.parse(savedUser)
      loginForm.username = username
      loginForm.password = password
      loginForm.remember = true
    } catch (e) {
      localStorage.removeItem('jemall_saved_user')
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
      const res = await request.post('/auth/login', {
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
    localStorage.setItem('jemall_saved_user', JSON.stringify({
      username: loginForm.username,
      password: loginForm.password
    }))
  } else {
    localStorage.removeItem('jemall_saved_user')
    localStorage.removeItem('jiejie_saved_user')
  }
}

const persistSession = (token, userInfo) => {
  const role = userInfo.role
  const nickname = userInfo.nickname
  localStorage.setItem('token', token)
  localStorage.setItem('user_role', role)
  localStorage.setItem('userInfo', JSON.stringify(userInfo))
  if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
    ElMessage.success(`欢迎回来，管理员 ${nickname}`)
    router.push('/admin')
  } else {
    ElMessage.success(`登录成功，欢迎来到杰物，${nickname}`)
    router.push('/')
  }
}

const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    registerLoading.value = true
    try {
      const res = await request.post('/auth/register', {
        username: registerForm.username.trim(),
        password: registerForm.password,
        nickname: registerForm.nickname?.trim() || undefined,
        phone: registerForm.phone?.trim() || undefined,
        email: registerForm.email?.trim() || undefined,
        phoneCode: registerForm.phoneCode?.trim() || undefined,
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

const startCountdown = (targetRef) => {
  targetRef.value = 60
  const timer = setInterval(() => {
    targetRef.value -= 1
    if (targetRef.value <= 0) clearInterval(timer)
  }, 1000)
}

const sendRegisterCode = async () => {
  const email = registerForm.email?.trim() || ''
  if (!email) return ElMessage.warning('请先输入邮箱')
  if (!/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email)) return ElMessage.warning('邮箱格式不正确')
  emailSending.value = true
  try {
    await request.post('/auth/email/send-code', { email, bizType: 'REGISTER' })
    ElMessage.success('验证码已发送，请查看邮箱模拟日志')
    startCountdown(emailCountdown)
  } catch (e) {
    console.error(e)
  } finally {
    emailSending.value = false
  }
}

const openResetDialog = () => {
  resetVisible.value = true
  resetForm.mode = 'phone'
  resetForm.account = ''
  resetForm.phoneCode = ''
  resetForm.newPassword = ''
}

const onResetModeChange = () => {
  resetForm.account = ''
  resetForm.phoneCode = ''
}

const sendResetCode = async () => {
  const account = resetForm.account?.trim() || ''
  if (!account) return ElMessage.warning(`请先输入${resetForm.mode === 'email' ? '邮箱' : '手机号'}`)
  const isEmail = resetForm.mode === 'email'
  if (!isEmail && !/^1[3-9]\d{9}$/.test(account)) {
    return ElMessage.warning('手机号格式不正确')
  }
  if (isEmail && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(account)) {
    return ElMessage.warning('邮箱格式不正确')
  }
  resetSending.value = true
  try {
    await request.post('/auth/email/send-code', {
      mode: resetForm.mode,
      phone: isEmail ? undefined : account,
      email: isEmail ? account : undefined,
      bizType: 'RESET_PASSWORD'
    })
    ElMessage.success('验证码已发送，请查看后端日志')
    startCountdown(resetCountdown)
  } catch (e) {
    console.error(e)
  } finally {
    resetSending.value = false
  }
}

const submitReset = async () => {
  const account = resetForm.account?.trim() || ''
  if (!account || !resetForm.phoneCode || !resetForm.newPassword) {
    return ElMessage.warning('请填写完整信息')
  }
  const isEmail = resetForm.mode === 'email'
  if (!isEmail && !/^1[3-9]\d{9}$/.test(account)) {
    return ElMessage.warning('手机号格式不正确')
  }
  if (isEmail && !/^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(account)) {
    return ElMessage.warning('邮箱格式不正确')
  }
  resetSubmitting.value = true
  try {
    await request.post('/auth/reset-password', {
      mode: resetForm.mode,
      phone: isEmail ? undefined : account,
      email: isEmail ? account : undefined,
      phoneCode: resetForm.phoneCode.trim(),
      newPassword: resetForm.newPassword
    })
    ElMessage.success('密码重置成功，请登录')
    resetVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    resetSubmitting.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(700px 380px at 15% 0%, rgba(31, 122, 111, 0.18), transparent 60%),
    radial-gradient(600px 300px at 90% 10%, rgba(244, 162, 97, 0.2), transparent 60%),
    linear-gradient(180deg, #f8f4ee 0%, #eef4f8 100%);
}
.login-box {
  width: 420px;
  padding: 45px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  border: 1px solid rgba(31, 122, 111, 0.08);
  animation: floatIn 0.6s ease both;
}
.login-tabs { margin-top: 8px; }
.login-tabs :deep(.el-tabs__header) { margin-bottom: 22px; }
.login-tabs :deep(.el-tabs__item) { font-size: 15px; }
.login-logo { text-align: center; margin-bottom: 30px; }
.login-logo h1 {
  font-size: 24px;
  color: #1f2a37;
  margin: 15px 0 8px;
  font-weight: 700;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}
.login-logo p { font-size: 11px; color: #8d99a8; text-transform: uppercase; letter-spacing: 1.5px; }
.login-options { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
.submit-btn { width: 100%; height: 44px; font-size: 16px; letter-spacing: 1px; border-radius: 10px; }
.login-footer { margin-top: 36px; text-align: center; border-top: 1px solid #eef2f6; padding-top: 20px; }
.copyright { color: #7b8794; font-size: 13px; margin: 0; }
.copyright strong { color: var(--brand); }
.project-tag { color: #aab4c0; font-size: 11px; margin: 6px 0 0; }

@keyframes floatIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
