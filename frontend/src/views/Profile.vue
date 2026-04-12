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
            name="file"
          >
            <img v-if="form.avatar" :src="getImageUrl(form.avatar)" class="avatar-preview" alt="" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <span class="upload-hint">点击上传，建议正方形图片</span>
        </el-form-item>

        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" maxlength="32" show-word-limit placeholder="在集市里展示的名字" />
        </el-form-item>

        <el-form-item label="面交地址">
          <el-input v-model="form.campusAddress" type="textarea" :rows="2" placeholder="例如：东区宿舍楼下 / 图书馆门口" />
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
  campusAddress: ''
})

const localToken = localStorage.getItem('token') || ''
const uploadHeaders = { token: localToken, Authorization: localToken }

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

const loadProfile = async () => {
  loading.value = true
  try {
    const data = await request.get('/user/profile')
    form.username = data.username || ''
    form.nickname = data.nickname || ''
    form.avatar = data.avatar || ''
    form.campusAddress = data.campusAddress || ''
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
  saving.value = true
  try {
    const updated = await request.put('/user/profile', {
      nickname: form.nickname.trim(),
      avatar: form.avatar?.trim() || '',
      campusAddress: form.campusAddress?.trim() || ''
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
  background: #f4f4f4;
  padding: 24px;
  box-sizing: border-box;
}
.profile-card {
  max-width: 560px;
  margin: 0 auto;
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.profile-form {
  padding-top: 8px;
}
.upload-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #909399;
}
:deep(.avatar-uploader .el-upload) {
  border: 1px dashed var(--el-border-color);
  border-radius: 8px;
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
