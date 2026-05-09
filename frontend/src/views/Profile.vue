<template>
  <div class="profile-page">
    <el-card class="profile-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <el-button :icon="ArrowLeft" circle @click="router.push('/')" />
          <span class="title">个人资料</span>
        </div>
      </template>

      <el-form v-loading="loading" :model="form" label-width="100px" class="profile-form">
        <el-form-item label="登录账号">
          <el-input :model-value="form.username" disabled />
        </el-form-item>

        <el-form-item label="头像">
          <el-upload
            class="avatar-uploader"
            :action="uploadActionUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="onAvatarSuccess"
            :on-error="onAvatarError"
            :before-upload="beforeImageUpload"
            accept="image/jpeg,image/jpg,image/png,image/webp,image/gif"
            name="file"
          >
            <img v-if="form.avatar" :src="getImageUrl(form.avatar)" class="avatar-preview" alt="" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <span class="upload-hint">点击上传，支持 JPG/PNG/WEBP/GIF，大小不超过 5MB</span>
        </el-form-item>

        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" maxlength="32" show-word-limit placeholder="在杰物展示的昵称" />
        </el-form-item>

        <el-form-item label="收货地址">
          <el-input v-model="form.campusAddress" type="textarea" :rows="2" placeholder="例如：辽宁省沈阳市浑南区 xx 路 xx 号" />
        </el-form-item>

        <el-divider content-position="left">地址簿</el-divider>
        <div style="display:flex;flex-direction:column;gap:8px;margin-bottom:10px;">
          <div v-for="a in addressList" :key="a.id" style="border:1px solid #ebeef5;border-radius:8px;padding:8px;">
            <div>{{ a.receiver }} {{ a.phone }} <el-tag size="small" v-if="Number(a.isDefault)===1">默认</el-tag></div>
            <div style="font-size:12px;color:#606266;">{{ a.province }}{{ a.city }}{{ a.district }} {{ a.detailAddress }}</div>
            <div style="margin-top:6px;">
              <el-button size="small" @click="editAddress(a)">编辑</el-button>
              <el-button size="small" type="danger" @click="removeAddress(a)">删除</el-button>
            </div>
          </div>
        </div>
        <el-button size="small" type="primary" plain @click="editAddress()">新增地址</el-button>

        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>

        <el-divider content-position="left">修改密码</el-divider>
        <el-form-item label="旧密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（至少6位）" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="pwdLoading" @click="changeMyPassword">更新密码</el-button>
        </el-form-item>

        <el-divider content-position="left">意见反馈</el-divider>
        <el-form-item label="反馈内容">
          <el-input v-model="feedbackText" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="提交你对平台的建议或问题" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" @click="submitUserFeedback">提交反馈</el-button>
          <el-button @click="loadMyFeedback">刷新记录</el-button>
        </el-form-item>
        <div style="display:flex;flex-direction:column;gap:8px;margin-top:8px;">
          <div v-for="f in myFeedbackList" :key="f.id" style="border:1px solid #ebeef5;border-radius:8px;padding:8px;">
            <div style="font-size:12px;color:#606266;">{{ f.createTime ? new Date(f.createTime).toLocaleString() : '-' }}</div>
            <div style="margin-top:4px;">{{ f.content }}</div>
            <div style="margin-top:6px;color:#909399;">状态：{{ Number(f.status)===0 ? '待处理' : (Number(f.status)===1 ? '已处理' : '驳回') }}</div>
            <div v-if="f.replyContent" style="margin-top:6px;color:#303133;">管理员回复：{{ f.replyContent }}</div>
          </div>
        </div>
      </el-form>
    </el-card>
    <el-dialog v-model="addressVisible" title="地址编辑" width="560px">
      <el-form :model="addressForm" label-width="90px">
        <el-form-item label="收货人"><el-input v-model="addressForm.receiver" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="addressForm.phone" /></el-form-item>
        <el-form-item label="省市区"><el-input v-model="addressForm.province" placeholder="省" style="width:30%;margin-right:1%;" /><el-input v-model="addressForm.city" placeholder="市" style="width:30%;margin-right:1%;" /><el-input v-model="addressForm.district" placeholder="区" style="width:38%;" /></el-form-item>
        <el-form-item label="详细地址"><el-input v-model="addressForm.detailAddress" /></el-form-item>
        <el-form-item label="默认地址"><el-switch v-model="addressForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="addressVisible=false">取消</el-button><el-button type="primary" @click="saveAddress">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveImageUrl, uploadActionUrl } from '../utils/env'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  avatar: '',
  campusAddress: '',
  phone: ''
})
const addressList = ref<any[]>([])
const addressVisible = ref(false)
const addressForm = ref<any>({ id: null, receiver: '', phone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
const feedbackText = ref('')
const myFeedbackList = ref<any[]>([])
const pwdForm = ref({ oldPassword: '', newPassword: '' })
const pwdLoading = ref(false)

const localToken = localStorage.getItem('token') || ''
const uploadHeaders = localToken ? { Authorization: `Bearer ${localToken}` } : {}
const MAX_IMAGE_MB = 5

const beforeImageUpload = (rawFile) => {
  const isImage = rawFile.type?.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  const isLtLimit = rawFile.size / 1024 / 1024 <= MAX_IMAGE_MB
  if (!isLtLimit) {
    ElMessage.error(`图片大小不能超过 ${MAX_IMAGE_MB}MB`)
    return false
  }
  return true
}

const getImageUrl = (url) => {
  return resolveImageUrl(url)
}

const onAvatarSuccess = (res) => {
  if (res.code === 200) {
    form.avatar = res.data
    ElMessage.success('头像已更新（记得点保存）')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const onAvatarError = () => {
  ElMessage.error(`上传失败，请确认格式正确且大小不超过 ${MAX_IMAGE_MB}MB`)
}

const loadProfile = async () => {
  loading.value = true
  try {
    const data = await request.get('/user/profile')
    form.username = data.username || ''
    form.nickname = data.nickname || ''
    form.avatar = data.avatar || ''
    form.campusAddress = data.campusAddress || ''
    form.phone = data.phone || ''
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSave = async () => {
  if (!form.nickname?.trim()) {
    return ElMessage.warning('请填写昵称')
  }
  if (!form.phone?.trim() || !/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    return ElMessage.warning('请输入正确手机号')
  }
  saving.value = true
  try {
    const updated = await request.put('/user/profile', {
      nickname: form.nickname.trim(),
      avatar: form.avatar?.trim() || '',
      campusAddress: form.campusAddress?.trim() || '',
      phone: form.phone.trim()
    })
    localStorage.setItem('userInfo', JSON.stringify(updated))
    ElMessage.success('已保存')
  } catch (e) {
    console.error(e)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
  loadAddressList()
  loadMyFeedback()
})

const loadAddressList = async () => {
  try {
    addressList.value = await request.get('/user/address/list') || []
  } catch (e) {
    console.error(e)
  }
}

const editAddress = (row?: any) => {
  addressForm.value = row ? { ...row } : { id: null, receiver: '', phone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 }
  addressVisible.value = true
}

const saveAddress = async () => {
  await request.post('/user/address/save', addressForm.value)
  addressVisible.value = false
  loadAddressList()
}

const removeAddress = async (row) => {
  await request.post(`/user/address/delete?id=${row.id}`)
  loadAddressList()
}

const changeMyPassword = async () => {
  const oldPassword = pwdForm.value.oldPassword.trim()
  const newPassword = pwdForm.value.newPassword.trim()
  if (!oldPassword || !newPassword) return ElMessage.warning('请填写完整密码信息')
  if (newPassword.length < 6) return ElMessage.warning('新密码至少 6 位')
  pwdLoading.value = true
  try {
    await request.post('/auth/change-password', { oldPassword, newPassword })
    pwdForm.value.oldPassword = ''
    pwdForm.value.newPassword = ''
    ElMessage.success('密码修改成功，请重新登录')
    localStorage.clear()
    router.push('/login')
  } catch (e) {
    console.error(e)
  } finally {
    pwdLoading.value = false
  }
}

const submitUserFeedback = async () => {
  const t = feedbackText.value.trim()
  if (!t) return ElMessage.warning('请填写反馈内容')
  try {
    await request.post('/user/feedback/submit', { content: t })
    feedbackText.value = ''
    ElMessage.success('反馈已提交')
    loadMyFeedback()
  } catch (e) {
    console.error(e)
  }
}

const loadMyFeedback = async () => {
  try {
    myFeedbackList.value = await request.get('/user/feedback/mine') || []
  } catch (e) {
    console.error(e)
    myFeedbackList.value = []
  }
}
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background:
    radial-gradient(800px 380px at 10% 0%, rgba(31, 122, 111, 0.16), transparent 60%),
    linear-gradient(180deg, #f8f4ee 0%, #eef4f8 100%);
  padding: 24px;
  box-sizing: border-box;
}
.profile-card {
  max-width: 600px;
  margin: 0 auto;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-1);
  border: 1px solid rgba(31, 122, 111, 0.08);
  background: rgba(255, 255, 255, 0.95);
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}
.profile-form { padding-top: 8px; }
.upload-hint { margin-left: 12px; font-size: 12px; color: #8d99a8; }
:deep(.avatar-uploader .el-upload) {
  border: 1px dashed rgba(31, 122, 111, 0.4);
  border-radius: 10px;
  cursor: pointer;
  overflow: hidden;
}
.avatar-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  display: block;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .profile-page { padding: 12px; }
  .profile-card { max-width: 100%; }
  :deep(.el-form-item__label) { width: 78px !important; }
  :deep(.el-form-item__content) { margin-left: 78px !important; }
  :deep(.el-dialog) { width: 94vw !important; }
}
</style>
