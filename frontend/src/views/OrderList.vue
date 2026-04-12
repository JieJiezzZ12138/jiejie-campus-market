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
            <el-button type="primary" link @click="router.push('/profile')">个人资料</el-button>
            <el-button type="primary" link @click="fetchOrders">刷新列表</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="orderScope" class="order-tabs" @tab-change="onTabChange">
        <el-tab-pane label="我买到的" name="buyer" />
        <el-tab-pane label="我卖出的（待发货等）" name="seller" />
      </el-tabs>

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

        <el-table-column label="对方信息" width="200">
          <template #default="scope">
            <div class="user-info">
              <div class="user-name">
                {{ orderScope === 'buyer'
                  ? (scope.row.sellerNickname || '卖家')
                  : (scope.row.buyerNickname || '买家') }}
              </div>
              <div class="user-contact">
                电话/账号：{{ orderScope === 'buyer'
                  ? (scope.row.sellerPhone || scope.row.sellerUsername || '-')
                  : (scope.row.buyerPhone || scope.row.buyerUsername || '-') }}
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="买家地址" width="180">
          <template #default="scope">
            <el-tag
              v-if="orderScope === 'seller' && scope.row.buyerAddress"
              type="warning"
              effect="plain"
            >
              {{ scope.row.buyerAddress }}
            </el-tag>
            <span v-else>{{ scope.row.buyerAddress || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="卖家地址" width="180">
          <template #default="scope">
            {{ scope.row.sellerAddress || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="130">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.orderStatus)">
              {{ statusText(scope.row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="300" fixed="right">
          <template #default="scope">
            <el-space wrap>
              <el-button type="primary" link size="small" @click="openChat(scope.row)">
                私信
              </el-button>
              <el-button type="danger" link size="small" @click="openFeedback(scope.row)">
                纠纷反馈
              </el-button>
              <el-button
                v-if="orderScope === 'buyer' && scope.row.orderStatus === 0"
                type="warning"
                size="small"
                @click="handlePay(scope.row)"
              >
                立即支付
              </el-button>
              <el-button
                v-if="orderScope === 'seller' && scope.row.orderStatus === 1"
                type="success"
                size="small"
                :loading="shipLoadingId === scope.row.id"
                @click="handleShip(scope.row)"
              >
                确认发货
              </el-button>
              <el-button
                v-if="orderScope === 'buyer' && scope.row.orderStatus === 2"
                type="success"
                size="small"
                :loading="receiveLoadingId === scope.row.id"
                @click="handleReceive(scope.row)"
              >
                确认收货
              </el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="feedbackVisible" title="请求管理员协助" width="520px" destroy-on-close>
      <p class="feedback-tip">请客观描述问题（如：未收到货、描述不符、对方失联等）。买卖双方均可提交，管理员将在后台收到通知。</p>
      <el-input
        v-model="feedbackText"
        type="textarea"
        :rows="5"
        maxlength="2000"
        show-word-limit
        placeholder="请详细说明情况，便于管理员处理"
      />
      <template #footer>
        <el-button @click="feedbackVisible = false">取消</el-button>
        <el-button type="primary" :loading="feedbackLoading" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="payDialogVisible" title="安全支付确认" width="30%" center>
      <div style="text-align: center" v-if="currentOrder">
        <el-icon size="50" color="#E6A23C"><Wallet /></el-icon>
        <h3>确认支付 ￥{{ currentOrder.totalAmount }} 吗？</h3>
        <p style="color: #909399">支付完成后，卖家将尽快为您发货</p>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Wallet, ArrowLeft } from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const orderList = ref([])
const loading = ref(false)
const payDialogVisible = ref(false)
const payLoading = ref(false)
const currentOrder = ref({})
const orderScope = ref('buyer')
const shipLoadingId = ref(null)
const receiveLoadingId = ref(null)

const feedbackVisible = ref(false)
const feedbackOrder = ref(null)
const feedbackText = ref('')
const feedbackLoading = ref(false)

const statusText = (s) => {
  if (s === 0) return '待支付'
  if (s === 1) return '已支付 / 待发货'
  if (s === 2) return '已发货'
  if (s === 3) return '已完成'
  return '未知'
}

const statusTagType = (s) => {
  if (s === 0) return 'danger'
  if (s === 1) return 'warning'
  if (s === 2) return 'success'
  if (s === 3) return 'success'
  return 'info'
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/order/list', { params: { scope: orderScope.value } })
    orderList.value = res || []
  } catch (error) {
    console.error('获取订单失败', error)
    ElMessage.error('无法加载订单列表')
  } finally {
    loading.value = false
  }
}

const onTabChange = () => {
  fetchOrders()
}

const openChat = (row) => {
  router.push(`/orders/${row.id}/chat`)
}

const openFeedback = (row) => {
  feedbackOrder.value = row
  feedbackText.value = ''
  feedbackVisible.value = true
}

const submitFeedback = async () => {
  const t = feedbackText.value.trim()
  if (!t) {
    ElMessage.warning('请填写反馈内容')
    return
  }
  if (!feedbackOrder.value?.id) return
  feedbackLoading.value = true
  try {
    await request.post('/order/feedback', { orderId: feedbackOrder.value.id, content: t })
    ElMessage.success('已提交，管理员将收到通知')
    feedbackVisible.value = false
  } catch (e) {
    console.error(e)
  } finally {
    feedbackLoading.value = false
  }
}

const handlePay = (order) => {
  currentOrder.value = order
  payDialogVisible.value = true
}

const handleShip = async (row) => {
  shipLoadingId.value = row.id
  try {
    await request.post(`/order/ship?orderId=${row.id}`)
    ElMessage.success('已标记发货')
    await fetchOrders()
  } catch (e) {
    console.error(e)
  } finally {
    shipLoadingId.value = null
  }
}

const handleReceive = async (row) => {
  receiveLoadingId.value = row.id
  try {
    await request.post(`/order/receive?orderId=${row.id}`)
    ElMessage.success('已确认收货')
    await fetchOrders()
  } catch (e) {
    console.error(e)
  } finally {
    receiveLoadingId.value = null
  }
}

const confirmPay = async () => {
  if (!currentOrder.value.id) return

  payLoading.value = true
  try {
    await request.post(`/order/pay?orderId=${currentOrder.value.id}`)
    ElMessage({
      message: '支付成功！',
      type: 'success',
      duration: 1500
    })
    payDialogVisible.value = false
    await fetchOrders()
  } catch (error) {
    console.error('支付环节异常:', error)
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
.order-tabs {
  margin-bottom: 16px;
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
.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.user-name {
  font-weight: 600;
  color: #303133;
}
.user-contact {
  font-size: 12px;
  color: #909399;
}
.price {
  color: #f56c6c;
  font-weight: bold;
}
.box-card {
  border-radius: 8px;
}
.feedback-tip {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px;
}
</style>
