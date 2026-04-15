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
            action="http://localhost:8080/product/upload"
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
          <el-input v-model="form.nickname" maxlength="32" show-word-limit placeholder="在集市里展示的名字" />
        </el-form-item>

        <el-form-item label="面交地址">
          <el-input v-model="form.campusAddress" type="textarea" :rows="2" placeholder="例如：东区宿舍楼下 / 图书馆门口" />
        </el-form-item>

        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import request from '../utils/request'

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
  if (!url) return ''
  if (url.includes('localhost:8080') || url.includes('localhost:8081')) {
    return url.replace(/8080|8081/g, '8082')
  }
  if (url.startsWith('http')) return url
  return 'http://localhost:8082' + url
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
  if (!form.phone?.trim() || !/^\d{7,20}$/.test(form.phone.trim())) {
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
})
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
</style>
