<template>
  <div class="admin-layout">
    <el-container style="height: 100vh;">
      
      <el-header class="admin-header">
        <div class="logo">
          <el-icon size="24" color="#fff"><DataBoard /></el-icon>
          <span>JieJie 集市后台管理系统</span>
        </div>
        
        <div class="header-right">
          <el-badge :value="12" class="msg-badge">
            <el-icon size="22" class="header-icon"><Bell /></el-icon>
          </el-badge>
          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <span class="nickname">{{ userInfo.nickname || '超级管理员' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/')">返回前台商城</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-container>
        <el-aside width="220px" class="admin-sidebar">
          <el-menu
            :default-active="activeMenu"
            class="sidebar-menu"
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409EFF"
            @select="handleMenuSelect"
          >
            <el-menu-item index="products">
              <el-icon><Goods /></el-icon>
              <span>全站商品管控</span>
            </el-menu-item>
            <el-menu-item index="users">
              <el-icon><User /></el-icon>
              <span>平台账号管理</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <el-main class="admin-main">
          
          <div v-if="activeMenu === 'products'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>📦 平台闲置物品大盘</span>
                  <el-button type="success" :icon="Refresh" circle @click="fetchAdminProducts" />
                </div>
              </template>

              <el-table :data="adminProducts" stripe style="width: 100%" v-loading="loading">
                <el-table-column prop="id" label="ID" width="80" align="center" />
                <el-table-column label="主图" width="100" align="center">
                  <template #default="{ row }">
                    <el-image :src="getImageUrl(row.image || row.imageUrl)" style="width: 50px; height: 50px; border-radius: 4px;" fit="cover">
                      <template #error><el-icon><Picture /></el-icon></template>
                    </el-image>
                  </template>
                </el-table-column>
                <el-table-column prop="name" label="商品名称" width="180" show-overflow-tooltip />
                <el-table-column label="价格" width="100">
                  <template #default="{ row }"><span style="color: #f56c6c; font-weight: bold;">¥{{ row.price }}</span></template>
                </el-table-column>
                <el-table-column prop="stock" label="剩余库存" width="100" align="center" />
                <el-table-column label="综合状态" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="getStatusTheme(row)" effect="dark">{{ getStatusText(row) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="发布人" width="120" align="center">
                  <template #default="{ row }"><span>{{ row.sellerName || row.sellerId }}</span></template>
                </el-table-column>
                <el-table-column label="操作" min-width="200" align="center">
                  <template #default="{ row }">
                    <el-button v-if="getStatusText(row) === '售卖中'" type="danger" size="small" @click="handleStatusChange(row, 2, 0)">违规下架</el-button>
                    <el-button v-if="getStatusText(row) === '违规下架'" type="success" size="small" @click="handleStatusChange(row, 1, 1)">恢复上架</el-button>
                    <el-button disabled size="small" v-if="getStatusText(row) === '已售罄'">商品已售出</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

          <div v-if="activeMenu === 'users'">
            <el-card class="box-card">
              <template #header>
                <div class="card-header">
                  <span>👥 平台用户管理</span>
                  <el-button type="success" :icon="Refresh" circle @click="fetchAdminUsers" />
                </div>
              </template>

              <el-table :data="adminUsers" stripe style="width: 100%" v-loading="userLoading">
                <el-table-column prop="id" label="用户ID" width="100" align="center" />
                
                <el-table-column label="头像" width="100" align="center">
                  <template #default="{ row }">
                    <el-avatar :size="40" :src="row.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                  </template>
                </el-table-column>

                <el-table-column prop="nickname" label="昵称" width="150" />
                <el-table-column prop="username" label="登录账号" width="150" />
                
                <el-table-column label="身份角色" width="120" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
                      {{ row.role === 'ADMIN' ? '管理员' : '学生卖家' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="账号状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="dark">
                      {{ row.status === 1 ? '正常' : '已封禁' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column label="操作" min-width="250" align="center">
                  <template #default="{ row }">
                    <el-button 
                      v-if="row.status === 1" 
                      type="danger" size="small" plain 
                      @click="handleUserStatus(row, 0)">
                      封禁账号
                    </el-button>
                    <el-button 
                      v-else 
                      type="success" size="small" plain 
                      @click="handleUserStatus(row, 1)">
                      解封账号
                    </el-button>

                    <el-button 
                      v-if="row.role !== 'ADMIN'" 
                      type="warning" size="small" 
                      @click="handleUserRole(row, 'ADMIN')">
                      设为管理员
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>

        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataBoard, Refresh, Picture, Bell, User, Goods } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

const activeMenu = ref('products')
const handleMenuSelect = (index) => {
  activeMenu.value = index
  if (index === 'products') fetchAdminProducts()
  if (index === 'users') fetchAdminUsers()
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出管理员后台吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.clear() 
    router.push('/login')
    ElMessage.success('已退出登录')
  }).catch(() => {})
}

// --- 商品管理逻辑 ---
const adminProducts = ref([])
const loading = ref(false)

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.includes('localhost:8080') || url.includes('localhost:8081')) {
    return url.replace(/8080|8081/g, '8082');
  }
  if (url.startsWith('http')) return url;
  return 'http://localhost:8082' + url;
}

const fetchAdminProducts = async () => {
  loading.value = true
  try {
    const res = await request.get('/product/admin/all')
    adminProducts.value = res || []
  } catch (error) {
    console.error("获取商品失败:", error)
  } finally {
    loading.value = false
  }
}

const getStatusText = (row) => {
  if (row.stock <= 0 || row.publishStatus === 2) return '已售罄'
  if (row.auditStatus === 0) return '待审核'
  if (row.auditStatus === 2 || row.publishStatus === 0) return '违规下架'
  return '售卖中'
}
const getStatusTheme = (row) => {
  if (row.stock <= 0 || row.publishStatus === 2) return 'info'
  if (row.auditStatus === 0) return 'warning'
  if (row.auditStatus === 2 || row.publishStatus === 0) return 'danger'
  return 'success'
}

const handleStatusChange = async (row, auditTarget, publishTarget) => {
  try {
    await request.post(`/product/admin/audit?id=${row.id}&status=${auditTarget}`)
    await request.post(`/product/admin/status?id=${row.id}&status=${publishTarget}`)
    ElMessage.success('商品状态更新成功')
    fetchAdminProducts() 
  } catch (error) {
    console.error("更新失败", error)
  }
}

// --- 👉 新增：账号管理逻辑 ---
const adminUsers = ref([])
const userLoading = ref(false)

// 获取全量用户
const fetchAdminUsers = async () => {
  userLoading.value = true
  try {
    // ⚠️ 注意：这里默认你后端有个 /user/admin/list 的接口，如果没有会报错
    const res = await request.get('/user/admin/list')
    adminUsers.value = res || []
  } catch (error) {
    console.error("获取用户数据失败:", error)
    // 兜底测试数据（防止你后端接口没写好时页面报错空白）
    if (adminUsers.value.length === 0) {
      ElMessage.warning('未能连接到用户接口，显示模拟数据')
      adminUsers.value = [
        { id: 1, username: 'admin', nickname: '系统管理员', role: 'ADMIN', status: 1 },
        { id: 2, username: 'student1', nickname: '张同学', role: 'USER', status: 1 },
        { id: 3, username: 'badguy', nickname: '违规小号', role: 'USER', status: 0 }
      ]
    }
  } finally {
    userLoading.value = false
  }
}

// 封号 / 解封
const handleUserStatus = async (row, targetStatus) => {
  try {
    // ⚠️ 注意：需要后端配合提供此接口
    await request.post(`/user/admin/status?id=${row.id}&status=${targetStatus}`)
    ElMessage.success(targetStatus === 1 ? '账号已解封' : '账号已封禁')
    fetchAdminUsers()
  } catch (error) {
    console.error("操作失败", error)
    row.status = targetStatus // 模拟前端变化
  }
}

// 提升管理员
const handleUserRole = async (row, targetRole) => {
  try {
    // ⚠️ 注意：需要后端配合提供此接口
    await request.post(`/user/admin/role?id=${row.id}&role=${targetRole}`)
    ElMessage.success('权限设置成功')
    fetchAdminUsers()
  } catch (error) {
    console.error("操作失败", error)
    row.role = targetRole // 模拟前端变化
  }
}

onMounted(() => {
  fetchAdminProducts()
})
</script>

<style scoped>
.admin-layout { height: 100vh; overflow: hidden; background-color: #f0f2f5; }

/* 顶栏样式 */
.admin-header { background-color: #2b3643; color: white; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); z-index: 10; }
.logo { display: flex; align-items: center; gap: 10px; font-size: 18px; font-weight: bold; letter-spacing: 1px; }
.header-right { display: flex; align-items: center; gap: 25px; }
.header-icon { color: #fff; cursor: pointer; transition: color 0.3s; }
.header-icon:hover { color: #409EFF; }
.msg-badge :deep(.el-badge__content) { top: 5px; right: 5px; }

/* 用户信息下拉菜单 */
.user-profile { display: flex; align-items: center; gap: 8px; cursor: pointer; color: #fff; }
.user-profile:hover { opacity: 0.8; }
.nickname { font-size: 14px; }

/* 侧边栏样式 */
.admin-sidebar { background-color: #304156; transition: width 0.3s; }
.sidebar-menu { border-right: none; height: calc(100vh - 60px); }

/* 主内容区 */
.admin-main { padding: 20px; height: calc(100vh - 60px); overflow-y: auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-size: 16px; font-weight: bold; color: #333; }
</style>