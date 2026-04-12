<template>
  <div class="chat-page">
    <el-header class="chat-header">
      <el-button :icon="ArrowLeft" circle @click="goBack" />
      <div class="header-title">
        <span class="title">{{ context?.chatTitle || '私聊' }}</span>
        <span class="sub" v-if="context">
          <template v-if="context.orderNo">订单 {{ context.orderNo }} · </template>
          {{ context.productName || '商品' }}
        </span>
      </div>
      <el-button type="primary" link @click="loadMessages">刷新</el-button>
    </el-header>

    <div class="msg-list" ref="msgListRef">
      <div v-if="!loading && messages.length === 0" class="empty-hint">暂无消息，打个招呼吧</div>
      <div
        v-for="m in messages"
        :key="m.id"
        class="msg-row"
        :class="{ mine: isMine(m), other: !isMine(m) }"
      >
        <div class="bubble">{{ m.content }}</div>
        <div class="time">{{ formatTime(m.createTime) }}</div>
      </div>
    </div>

    <div class="input-bar">
      <el-input
        v-model="draft"
        type="textarea"
        :rows="2"
        maxlength="2000"
        show-word-limit
        placeholder="输入消息，Enter 发送"
        @keydown.enter.exact.prevent="send"
      />
      <el-button type="primary" :loading="sendLoading" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()

const isProductEntry = computed(() => route.name === 'ProductChat')
const isThreadEntry = computed(() => route.name === 'ThreadChat')

/** 当前登录用户 id（与 senderId 数值比较），在 boot 时从 localStorage 写入以保证响应式 */
const selfId = ref(null)

const context = ref(null)
const threadId = ref(null)
/** 用于合并仅带 order_id 的旧消息；从会话上下文或订单路由得到 */
const legacyOrderId = ref(null)

const messages = ref([])
const draft = ref('')
const loading = ref(false)
const sendLoading = ref(false)
const msgListRef = ref(null)
let pollTimer = null

const isMine = (m) => {
  if (selfId.value == null) return false
  return Number(m.senderId) === selfId.value
}

const formatTime = (t) => {
  if (!t) return ''
  return new Date(t).toLocaleString()
}

const scrollBottom = async () => {
  await nextTick()
  const el = msgListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

const loadContext = async () => {
  try {
    if (isProductEntry.value) {
      const pid = Number(route.params.productId)
      if (!pid || Number.isNaN(pid)) {
        ElMessage.error('无效的商品')
        router.push('/')
        return false
      }
      context.value = await request.get('/order/chat/context', { params: { productId: pid } })
      legacyOrderId.value = null
    } else if (isThreadEntry.value) {
      const tid = Number(route.params.threadId)
      if (!tid || Number.isNaN(tid)) {
        ElMessage.error('无效的会话')
        router.push('/messages')
        return false
      }
      context.value = await request.get('/order/chat/context', { params: { threadId: tid } })
      legacyOrderId.value = context.value?.orderId != null ? Number(context.value.orderId) : null
    } else {
      const oid = Number(route.params.orderId)
      if (!oid || Number.isNaN(oid)) {
        ElMessage.error('无效的订单')
        router.push('/orders')
        return false
      }
      context.value = await request.get('/order/chat/context', { params: { orderId: oid } })
      legacyOrderId.value = Number(route.params.orderId)
    }
    threadId.value = context.value?.threadId
    return true
  } catch (e) {
    console.error(e)
    return false
  }
}

const loadMessages = async () => {
  if (!threadId.value) return
  loading.value = true
  try {
    const params = { threadId: threadId.value }
    if (legacyOrderId.value != null && !Number.isNaN(legacyOrderId.value)) {
      params.orderId = legacyOrderId.value
    }
    const list = await request.get('/order/chat/messages', { params })
    messages.value = list || []
    await scrollBottom()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const send = async () => {
  const text = draft.value.trim()
  if (!text || !threadId.value) return
  sendLoading.value = true
  try {
    const body = { threadId: threadId.value, content: text }
    if (legacyOrderId.value != null && !Number.isNaN(legacyOrderId.value)) {
      body.orderId = legacyOrderId.value
    }
    await request.post('/order/chat/send', body)
    draft.value = ''
    await loadMessages()
  } catch (e) {
    console.error(e)
  } finally {
    sendLoading.value = false
  }
}

const goBack = () => {
  if (isThreadEntry.value) {
    router.push('/messages')
  } else if (isProductEntry.value) {
    router.push('/')
  } else {
    router.push('/orders')
  }
}

const boot = async () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  try {
    const u = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const v = u.id != null ? u.id : u.userId
    const n = v !== '' && v != null ? Number(v) : NaN
    selfId.value = Number.isFinite(n) ? n : null
  } catch {
    selfId.value = null
  }
  const ok = await loadContext()
  if (!ok) return
  await loadMessages()
  pollTimer = setInterval(loadMessages, 4000)
}

watch(
  () => [route.name, route.params.productId, route.params.orderId, route.params.threadId],
  () => {
    boot()
  }
)

onMounted(() => {
  boot()
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}
.chat-header {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  height: 56px !important;
}
.header-title {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.header-title .title {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}
.header-title .sub {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.empty-hint {
  text-align: center;
  color: #909399;
  margin-top: 40px;
  font-size: 14px;
}
.msg-row {
  display: flex;
  flex-direction: column;
  max-width: 85%;
}
.msg-row.other {
  align-self: flex-start;
  align-items: flex-start;
}
.msg-row.mine {
  align-self: flex-end;
  align-items: flex-end;
}
.bubble {
  background: #fff;
  padding: 10px 14px;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  font-size: 14px;
  color: #303133;
  word-break: break-word;
}
.msg-row.mine .bubble {
  background: #409eff;
  color: #fff;
}
.time {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 4px;
}
.input-bar {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  align-items: flex-end;
}
.input-bar :deep(.el-textarea) {
  flex: 1;
}
</style>
