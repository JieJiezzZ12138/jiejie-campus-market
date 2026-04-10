<template>
  <div class="order-container">
    <el-card class="box-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button 
              :icon="ArrowLeft" 
              circle 
              @click="router.push('/')" 
              style="margin-right: 15px;" 
            />
            <span class="title">我的订单中心</span>
          </div>
          <div class="header-right">
            <el-button type="primary" link @click="fetchOrders">刷新列表</el-button>
          </div>
        </div>
      </template>

      <el-table :data="orderList" v-loading="loading" style="width: 100%" border stripe>
        <el-table-column label="订单编号" prop="orderNo" width="180" />
        
        <el-table-column label="商品信息">
          <template #default="scope">
            <div class="product-info">
              <el-icon v-if="!scope.row.productImage"><Picture /></el-icon>
              <img v-else :src="scope.row.productImage" class="p-img" />
              <span class="p-name">{{ scope.row.productName || '二手商品' }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="金额" width="120">
          <template #default="scope">
            <span class="price">￥{{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="下单时间" prop="createTime" width="180">
          <template #default="scope">
            {{ scope.row.createTime ? new Date(scope.row.createTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.orderStatus === 1 ? 'success' : 'danger'">
              {{ scope.row.orderStatus === 1 ? '已支付' : '待支付' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150">
          <template #default="scope">
            <el-button 
              v-if="scope.row.orderStatus === 0" 
              type="warning" 
              size="small" 
              @click="handlePay(scope.row)"
            >
              立即支付
            </el-button>
            <span v-else style="color: #909399; font-size: 12px;">交易已完成</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="payDialogVisible" title="安全支付确认" width="30%" center>
      <div style="text-align: center" v-if="currentOrder">
        <el-icon size="50" color="#E6A23C"><Wallet /></el-icon>
        <h3>确认支付 ￥{{ currentOrder.totalAmount }} 吗？</h3>
        <p style="color: #909399">支付完成后，商品将正式归属于您</p>
      </div>
      <template #footer>
        <el-button @click="payDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="payLoading" @click="confirmPay">
          确认付款
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router' // 👈 必须导入
import { ElMessage } from 'element-plus'
import { Picture, Wallet, ArrowLeft } from '@element-plus/icons-vue' // 👈 增加 ArrowLeft
import request from '../utils/request'

const router = useRouter() // 👈 初始化路由
const orderList = ref([])
const loading = ref(false)
const payDialogVisible = ref(false)
const payLoading = ref(false)
const currentOrder = ref({})

// 获取订单列表
const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/order/list')
    // 兜底处理：防止 res 为空导致表格渲染出错
    orderList.value = res || []
  } catch (error) {
    console.error('获取订单失败', error)
    ElMessage.error('无法加载订单列表')
  } finally {
    loading.value = false
  }
}

// 触发支付弹窗
const handlePay = (order) => {
  currentOrder.value = order
  payDialogVisible.value = true
}

// 核心：确认付款并自动跳转
const confirmPay = async () => {
  if (!currentOrder.value.id) return
  
  payLoading.value = true
  try {
    // 1. 调用后端支付接口
    await request.post(`/order/pay?orderId=${currentOrder.value.id}`)
    
    // 2. 显示成功消息
    ElMessage({
      message: '支付成功！正在为您返回集市...',
      type: 'success',
      duration: 1500
    })

    // 3. 延时跳转，增强仪式感
    setTimeout(() => {
      payDialogVisible.value = false
      router.push('/') // 👈 支付完直接回首页
    }, 1500)

  } catch (error) {
    console.error('支付环节异常:', error)
    // 报错时关闭 Loading，保持在当前页，让用户看清楚错误
  } finally {
    payLoading.value = false
  }
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.order-container {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 100px);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
.product-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.p-img {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
}
.p-name {
  font-weight: 500;
}
.price {
  color: #f56c6c;
  font-weight: bold;
}
.box-card {
  border-radius: 8px;
}
</style>