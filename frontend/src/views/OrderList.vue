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
            <el-badge
              :value="currentScopeNoticeCount"
              :hidden="currentScopeNoticeCount === 0"
              :max="99"
              class="order-notice-badge"
            >
              <el-button type="primary" link @click="openNoticeDrawer">订单通知</el-button>
            </el-badge>
          </div>
        </div>
      </template>

      <el-tabs v-model="orderScope" class="order-tabs" @tab-change="onTabChange">
        <el-tab-pane name="buyer">
          <template #label>
            <el-badge
              :value="noticeCounts.buyer"
              :hidden="noticeCounts.buyer === 0"
              :max="99"
              class="tab-badge"
            >
              <span>我买到的</span>
            </el-badge>
          </template>
        </el-tab-pane>
        <el-tab-pane name="seller">
          <template #label>
            <el-badge
              :value="noticeCounts.seller"
              :hidden="noticeCounts.seller === 0"
              :max="99"
              class="tab-badge"
            >
              <span>我卖出的（待发货等）</span>
            </el-badge>
          </template>
        </el-tab-pane>
      </el-tabs>

      <el-table :data="orderList" v-loading="loading" style="width: 100%" border stripe :row-class-name="orderRowClass">
        <el-table-column label="订单信息" min-width="680">
          <template #default="scope">
            <div class="order-info-two-rows">
              <div class="order-info-line order-info-line-top">
                <div class="thumb-cell">
                  <el-icon v-if="!scope.row.productImage"><Picture /></el-icon>
                  <el-image
                    v-else
                    :src="getImageUrl(scope.row.productImage)"
                    class="p-img"
                    fit="cover"
                  >
                    <template #error>
                      <div class="p-img p-img-fallback">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                </div>
                <span class="p-name p-name-full">{{ scope.row.productName || '二手商品' }}</span>
                <span class="price">￥{{ scope.row.totalAmount }}</span>
                <el-tag :type="statusTagType(scope.row.orderStatus)">
                  {{ statusText(scope.row.orderStatus) }}
                </el-tag>
              </div>

              <div class="order-info-line order-info-line-bottom">
                <span>订单号：{{ scope.row.orderNo || '-' }}</span>
                <span>下单时间：{{ scope.row.createTime ? new Date(scope.row.createTime).toLocaleString() : '-' }}</span>
                <span>
                  对方：{{ orderScope === 'buyer'
                    ? (scope.row.sellerNickname || '卖家')
                    : (scope.row.buyerNickname || '买家') }}
                  （{{ orderScope === 'buyer'
                    ? (scope.row.sellerPhone || scope.row.sellerUsername || '-')
                    : (scope.row.buyerPhone || scope.row.buyerUsername || '-') }}）
                </span>
                <span>买家地址：{{ scope.row.buyerAddress || '-' }}</span>
                <span>卖家地址：{{ scope.row.sellerAddress || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
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

    <el-drawer v-model="noticeDrawerVisible" title="订单通知" size="480px" destroy-on-close>
      <div class="notice-actions">
        <el-button size="small" :icon="Refresh" @click="fetchNoticeList">刷新</el-button>
        <el-select v-model="noticeTypeFilter" size="small" class="notice-filter" placeholder="筛选类型">
          <el-option v-for="opt in noticeTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <span class="notice-count">未读 {{ currentScopeNoticeCount }}</span>
      </div>
      <el-empty v-if="!noticeLoading && noticeList.length === 0" description="暂无通知" />
      <el-table
        v-else
        v-loading="noticeLoading"
        :data="filteredNoticeList"
        size="small"
        style="width: 100%"
        :row-class-name="noticeRowClass"
      >
        <el-table-column label="时间" width="160">
          <template #default="{ row }">
            {{ row.createTime ? new Date(row.createTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="事件" min-width="140">
          <template #default="{ row }">
            {{ noticeText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="订单" min-width="120">
          <template #default="{ row }">
            <div class="notice-order">
              <div class="order-no">{{ row.orderNo || ('#' + row.orderId) }}</div>
              <div class="order-product">{{ row.productName || '商品' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{ row }">
            <el-space size="small">
              <el-button type="primary" link size="small" @click="openOrderDetail(row)">订单详情</el-button>
              <el-button type="primary" link size="small" @click="openOrderChat(row)">私信</el-button>
              <el-button
                v-if="Number(row.isRead) === 0 && canManualRead(row)"
                type="primary"
                link
                size="small"
                @click="markNoticeRead(row)"
              >
                已读
              </el-button>
              <span v-else-if="Number(row.isRead) === 1" class="read-flag">已读</span>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="orderDetailVisible" title="订单详情" width="620px" destroy-on-close>
      <el-skeleton v-if="orderDetailLoading" :rows="6" animated />
      <el-descriptions v-else-if="orderDetail" :column="2" border>
        <el-descriptions-item label="订单号">
          {{ orderDetail.orderNo || ('#' + orderDetail.id) }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          {{ statusText(orderDetail.orderStatus) }}
        </el-descriptions-item>
        <el-descriptions-item label="商品">
          {{ orderDetail.productName || '商品' }}
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          ￥{{ orderDetail.totalAmount }}
        </el-descriptions-item>
        <el-descriptions-item label="买家">
          {{ orderDetail.buyerNickname || '买家' }} / {{ orderDetail.buyerPhone || orderDetail.buyerUsername || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="卖家">
          {{ orderDetail.sellerNickname || '卖家' }} / {{ orderDetail.sellerPhone || orderDetail.sellerUsername || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="买家地址">
          {{ orderDetail.buyerAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="卖家地址">
          {{ orderDetail.sellerAddress || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="下单时间">
          {{ orderDetail.createTime ? new Date(orderDetail.createTime).toLocaleString() : '-' }}
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="orderDetailChat">私信沟通</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Wallet, ArrowLeft, Refresh } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveImageUrl } from '../utils/env'

const router = useRouter()
const orderList = ref([])
const loading = ref(false)
const payDialogVisible = ref(false)
const payLoading = ref(false)
const currentOrder = ref<any>({})
const orderScope = ref('buyer')
const shipLoadingId = ref(null)
const receiveLoadingId = ref(null)
const noticeCounts = ref({ buyer: 0, seller: 0, total: 0 })
const noticeDrawerVisible = ref(false)
const noticeList = ref([])
const noticeLoading = ref(false)
const orderDetailVisible = ref(false)
const orderDetailLoading = ref(false)
const orderDetail = ref(null)
const noticeTypeFilter = ref('ALL')
const noticeTypeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待付款', value: 'PAY_PENDING' },
  { label: '新订单', value: 'NEW_ORDER' },
  { label: '已付款', value: 'PAID' },
  { label: '已发货', value: 'SHIPPED' },
  { label: '已收货', value: 'RECEIVED' }
]
const highlightOrderId = ref(null)
let highlightTimer = null

const currentScopeNoticeCount = computed(() => {
  return orderScope.value === 'seller'
    ? Number(noticeCounts.value?.seller || 0)
    : Number(noticeCounts.value?.buyer || 0)
})

const filteredNoticeList = computed(() => {
  if (noticeTypeFilter.value === 'ALL') return noticeList.value
  return noticeList.value.filter((n) => n.noticeType === noticeTypeFilter.value)
})

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

const getImageUrl = (url) => {
  return resolveImageUrl(url)
}

const fetchOrders = async () => {
  loading.value = true
  try {
    const res = await request.get('/order/list', { params: { scope: orderScope.value } })
    orderList.value = res || []
    if (highlightOrderId.value) {
      await nextTick()
      scrollToOrder(highlightOrderId.value)
    }
  } catch (error) {
    console.error('获取订单失败', error)
    ElMessage.error('无法加载订单列表')
  } finally {
    loading.value = false
    await fetchOrderNoticeCount()
  }
}

const fetchOrderNoticeCount = async () => {
  try {
    const res = await request.get('/order/notice/unread-count')
    noticeCounts.value = res || { buyer: 0, seller: 0, total: 0 }
    window.dispatchEvent(new CustomEvent('order-notice-count', { detail: noticeCounts.value }))
  } catch (e) {
    console.error(e)
    noticeCounts.value = { buyer: 0, seller: 0, total: 0 }
    window.dispatchEvent(new CustomEvent('order-notice-count', { detail: noticeCounts.value }))
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

const openNoticeDrawer = async () => {
  noticeDrawerVisible.value = true
  noticeTypeFilter.value = 'ALL'
  await fetchNoticeList()
}

const fetchNoticeList = async () => {
  noticeLoading.value = true
  try {
    const res = await request.get('/order/notice/list', {
      params: { scope: orderScope.value, limit: 80 }
    })
    noticeList.value = res || []
  } catch (e) {
    console.error(e)
    noticeList.value = []
  } finally {
    noticeLoading.value = false
    await fetchOrderNoticeCount()
  }
}

const markNoticeRead = async (row) => {
  if (!row?.id) return
  try {
    await request.post(`/order/notice/read?id=${row.id}`)
    row.isRead = 1
  } catch (e) {
    console.error(e)
  } finally {
    await fetchOrderNoticeCount()
  }
}

const noticeText = (row) => {
  const t = row?.noticeType
  if (t === 'NEW_ORDER') return '新订单待处理'
  if (t === 'PAY_PENDING') return '待付款：订单已创建'
  if (t === 'PAID') return '买家已付款'
  if (t === 'SHIPPED') return '卖家已发货'
  if (t === 'RECEIVED') return '买家已确认收货'
  return '订单状态更新'
}

const noticeRowClass = ({ row }) => {
  return Number(row?.isRead) === 0 ? 'notice-unread' : ''
}

const orderRowClass = ({ row }) => {
  const base = `order-row-${row.id}`
  if (highlightOrderId.value && row.id === highlightOrderId.value) {
    return `${base} order-row-highlight`
  }
  return base
}

const canManualRead = (row) => {
  // 待处理类通知不允许手动清除，避免红点失效
  const t = row?.noticeType
  return t === 'RECEIVED'
}

const openOrderDetail = async (row) => {
  if (!row?.orderId) return
  noticeDrawerVisible.value = false
  await focusOrderInList(row.orderId)
  await loadOrderDetail(row.orderId)
}

const loadOrderDetail = async (orderId) => {
  orderDetailVisible.value = true
  orderDetailLoading.value = true
  orderDetail.value = null
  try {
    const res = await request.get('/order/detail', { params: { orderId } })
    orderDetail.value = res
  } catch (e) {
    console.error(e)
  } finally {
    orderDetailLoading.value = false
  }
}

const orderDetailChat = () => {
  if (!orderDetail.value?.id) return
  orderDetailVisible.value = false
  router.push(`/orders/${orderDetail.value.id}/chat`)
}

const openOrderChat = (row) => {
  if (!row?.orderId) return
  noticeDrawerVisible.value = false
  router.push(`/orders/${row.orderId}/chat`)
}

const focusOrderInList = async (orderId) => {
  if (!orderId) return
  const exists = orderList.value.some((o) => Number(o.id) === Number(orderId))
  if (!exists) {
    await fetchOrders()
  }
  highlightOrderId.value = orderId
  await nextTick()
  scrollToOrder(orderId)
  if (highlightTimer) clearTimeout(highlightTimer)
  highlightTimer = setTimeout(() => {
    highlightOrderId.value = null
  }, 5000)
}

const scrollToOrder = (orderId) => {
  const el = document.querySelector(`.order-row-${orderId}`)
  if (el && el.scrollIntoView) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}

onMounted(() => {
  fetchOrders()
})

onBeforeUnmount(() => {
  if (highlightTimer) clearTimeout(highlightTimer)
})
</script>

<style scoped>
.order-container {
  padding: 20px;
  background:
    radial-gradient(900px 420px at 10% 0%, rgba(31, 122, 111, 0.14), transparent 60%),
    linear-gradient(180deg, #f8f4ee 0%, #eef4f8 100%);
  min-height: calc(100vh - 100px);
}
.order-tabs {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(90deg, rgba(31, 122, 111, 0.12), rgba(244, 162, 97, 0.12));
  padding: 6px 10px;
  border-radius: 10px;
}
.header-left {
  display: flex;
  align-items: center;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}
.header-right :deep(.el-button.is-link) {
  height: 32px;
  line-height: 32px;
  padding-top: 0;
  padding-bottom: 0;
}
.header-right :deep(.el-badge) {
  display: inline-flex;
  align-items: center;
}
.title {
  font-size: 18px;
  font-weight: 700;
  color: #1f2a37;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}
.p-img {
  width: 52px;
  height: 52px;
  border-radius: 4px;
  overflow: hidden;
}
.p-img-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f3f4f6;
  color: #9ca3af;
}
.thumb-cell {
  display: flex;
  align-items: center;
  justify-content: center;
}
.p-name {
  font-weight: 500;
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.p-name-full {
  max-width: 100%;
}
.order-info-two-rows {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}
.order-info-line {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}
.order-info-line-top {
  font-size: 14px;
}
.order-info-line-bottom {
  flex-wrap: wrap;
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
}
.tab-badge :deep(.el-badge__content) {
  transform: translate(6px, -6px);
}
.price {
  color: #e76f51;
  font-weight: 700;
}
.box-card {
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 122, 111, 0.08);
  box-shadow: var(--shadow-1);
  background: rgba(255, 255, 255, 0.95);
}
.feedback-tip {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px;
}
.order-tabs :deep(.el-tabs__item) {
  font-weight: 600;
}
:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}
.order-notice-badge :deep(.el-badge__content) {
  transform: translate(6px, -6px);
}
.notice-actions {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.notice-filter {
  min-width: 140px;
}
.notice-count {
  font-size: 12px;
  color: #909399;
}
:deep(.order-row-highlight td) {
  background: #e8f4ff !important;
  transition: background-color 0.8s ease;
}
.notice-order .order-no {
  font-weight: 600;
}
.notice-order .order-product {
  font-size: 12px;
  color: #909399;
}
.read-flag {
  font-size: 12px;
  color: #c0c4cc;
}
:deep(.notice-unread td) {
  background: #fff7e6 !important;
}
</style>
