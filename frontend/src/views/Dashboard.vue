<template>
  <div class="admin-container">
    <el-header class="admin-header">
      <div class="header-left">
        <el-icon size="24" color="#409EFF"><Monitor /></el-icon>
        <span class="title">JieJie 校园集市管理系统</span>
      </div>
      <div class="header-right">
        <el-tag type="success" effect="dark">管理员模式</el-tag>
        <el-button link @click="goHome">返回商城</el-button>
      </div>
    </el-header>

    <div class="admin-main">
      <el-row :gutter="20" class="stats-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card blue">
            <div class="stat-info">
              <div class="label">全站商品</div>
              <div class="value">{{ stats.total }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card orange">
            <div class="stat-info">
              <div class="label">待审核商品</div>
              <div class="value">{{ stats.pending }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card red">
            <div class="stat-info">
              <div class="label">已售罄/下架</div>
              <div class="value">{{ stats.soldOut }}</div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card green">
            <div class="stat-info">
              <div class="label">活跃用户</div>
              <div class="value">{{ userList.length }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="content-card">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          
          <el-tab-pane label="📦 商品审核" name="products">
            <el-table :data="productList" border stripe style="width: 100%">
              <el-table-column prop="name" label="商品名称" min-width="180" />
              <el-table-column prop="price" label="价格" width="100">
                <template #default="scope">¥ {{ scope.row.price }}</template>
              </el-table-column>
              <el-table-column prop="sellerId" label="卖家ID" width="100" />
              <el-table-column label="审核状态" width="120">
                <template #default="scope">
                  <el-tag :type="getAuditTag(scope.row.auditStatus)">
                    {{ getAuditText(scope.row.auditStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220">
                <template #default="scope">
                  <div v-if="scope.row.auditStatus === 0">
                    <el-button type="success" size="small" @click="handleProductAudit(scope.row.id, 1)">通过</el-button>
                    <el-button type="danger" size="small" @click="handleProductAudit(scope.row.id, 2)">拒绝</el-button>
                  </div>
                  <el-button v-else-if="scope.row.auditStatus === 1" type="warning" size="small" plain @click="handleProductAudit(scope.row.id, 2)">违规下架</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="👥 用户管理" name="users">
            <el-table :data="userList" border stripe style="width: 100%">
              <el-table-column prop="id" label="UID" width="80" />
              <el-table-column prop="username" label="账号" width="150" />
              <el-table-column prop="nickname" label="昵称" width="150" />
              <el-table-column prop="role" label="角色">
                <template #default="scope">
                  <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'info'">{{ scope.row.role }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="账号状态" width="120">
                <template #default="scope">
                  <el-badge is-dot :type="scope.row.auditStatus === 1 ? 'success' : 'danger'">
                    <span>{{ scope.row.auditStatus === 1 ? '正常' : '已封禁' }}</span>
                  </el-badge>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="scope">
                  <el-button 
                    v-if="scope.row.role !== 'ADMIN'"
                    :type="scope.row.auditStatus === 1 ? 'danger' : 'success'" 
                    size="small" 
                    @click="handleUserAudit(scope.row.id, scope.row.auditStatus === 1 ? 0 : 1)"
                  >
                    {{ scope.row.auditStatus === 1 ? '封禁' : '解封' }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="🔔 消息中心" name="msgs">
            <el-empty description="暂无全站系统通知" />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Monitor } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const activeTab = ref('products')

// 数据状态
const stats = ref({ total: 0, pending: 0, soldOut: 0 })
const productList = ref([])
const userList = ref([])

// --- 初始化与数据加载 ---
const loadStats = async () => {
  try {
    const res = await request.get('/product/admin/stats')
    stats.value = res
  } catch (e) { console.error(e) }
}

const loadProducts = async () => {
  try {
    const res = await request.get('/product/admin/all')
    productList.value = res
  } catch (e) { console.error(e) }
}

const loadUsers = async () => {
  try {
    const res = await request.get('/auth/admin/user/list')
    userList.value = res
  } catch (e) { console.error(e) }
}

const handleTabChange = (name) => {
  if (name === 'products') loadProducts()
  if (name === 'users') loadUsers()
}

// --- 业务操作：商品审核 ---
const handleProductAudit = async (id, status) => {
  const actionText = status === 1 ? '通过' : '下架/拒绝'
  try {
    await request.post(`/product/admin/audit?id=${id}&status=${status}`)
    ElMessage.success(`商品已${actionText}`)
    loadProducts()
    loadStats()
  } catch (e) { console.error(e) }
}

// --- 业务操作：用户封禁 (新增) ---
const handleUserAudit = async (id, status) => {
  const actionText = status === 1 ? '解封' : '封禁'
  ElMessageBox.confirm(`确定要${actionText}该用户吗？`, '安全警示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await request.post(`/auth/admin/user/audit?id=${id}&status=${status}`)
    ElMessage.success(`操作成功：${actionText}`)
    loadUsers()
  }).catch(() => {})
}

// 辅助工具
const getAuditTag = (s) => s === 1 ? 'success' : (s === 0 ? 'warning' : 'danger')
const getAuditText = (s) => s === 1 ? '已通过' : (s === 0 ? '待审核' : '已违规/拒绝')
const goHome = () => router.push('/')

onMounted(() => {
  loadStats()
  loadProducts()
  loadUsers()
})
</script>

<style scoped>
.admin-container { min-height: 100vh; background-color: #f0f2f5; }
.admin-header { background: #fff; height: 60px; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; box-shadow: 0 1px 4px rgba(0,21,41,.08); }
.header-left { display: flex; align-items: center; gap: 12px; }
.title { font-size: 18px; font-weight: bold; color: #303133; }
.admin-main { padding: 24px; max-width: 1400px; margin: 0 auto; }

.stats-cards { margin-bottom: 24px; }
.stat-card { border: none; color: #fff; height: 100px; display: flex; align-items: center; }
.stat-info { padding-left: 10px; }
.label { font-size: 14px; opacity: 0.9; }
.value { font-size: 28px; font-weight: bold; margin-top: 4px; }

.blue { background: linear-gradient(135deg, #1890ff 0%, #36cfc9 100%); }
.orange { background: linear-gradient(135deg, #ffa940 0%, #ff7a45 100%); }
.red { background: linear-gradient(135deg, #ff4d4f 0%, #ff7875 100%); }
.green { background: linear-gradient(135deg, #52c41a 0%, #95de64 100%); }

.content-card { border-radius: 8px; border: none; }
:deep(.el-tabs__item) { font-size: 16px; height: 50px; }
</style>