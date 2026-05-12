<template>
  <div class="inbox-wrap" :class="{ 'embedded-mode': props.adminEmbedded }">
    <el-page-header v-if="!props.adminEmbedded" @back="goBackToAdmin">
      <template #content>
        <span class="page-title">站内消息</span>
      </template>
    </el-page-header>

    <el-card class="inbox-card" shadow="never">
      <template #header>
        <div class="card-h">
          <span>全部会话</span>
          <el-button type="primary" link @click="loadInbox">刷新</el-button>
        </div>
      </template>

      <el-empty
        v-if="!loading && rows.length === 0"
        description="暂无会话。可在商品页「咨询商家」或订单里「消息」开始沟通。"
      />
      <el-table
        v-else
        v-loading="loading"
        :data="rows"
        stripe
        class="inbox-table"
        :row-class-name="rowClassName"
        @row-click="openThread"
      >
        <el-table-column label="对方" min-width="140">
          <template #default="{ row }">
            <el-badge
              :value="row.unreadCount"
              :hidden="!row.unreadCount || Number(row.unreadCount) <= 0"
              :max="99"
              class="unread-badge"
            >
              <span class="peer-name">{{ row.peerNickname || '用户' }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column prop="productName" label="关联商品" min-width="160" show-overflow-tooltip />
        <el-table-column label="最近一条" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.lastPreview || '（点开开始聊天）' }}
          </template>
        </el-table-column>
        <el-table-column label="最后时间" width="180">
          <template #default="{ row }">
            {{ row.lastTime ? new Date(row.lastTime).toLocaleString() : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="" width="90" align="center">
          <template #default>
            <el-button type="primary" link>进入</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const props = withDefaults(defineProps<{ adminEmbedded?: boolean }>(), {
  adminEmbedded: false
})

const goBackToAdmin = () => {
  if (props.adminEmbedded) return
  const role = localStorage.getItem('user_role')
  if (role === 'ADMIN' || role === 'SUPER_ADMIN') {
    router.push('/admin')
    return
  }
  router.push('/')
}
const rows = ref([])
const loading = ref(false)

const loadInbox = async () => {
  loading.value = true
  try {
    const list = await request.get('/order/chat/inbox')
    rows.value = list || []
  } catch (e) {
    console.error(e)
    rows.value = []
  } finally {
    loading.value = false
  }
}

const openThread = (row) => {
  if (!row?.threadId) return
  const role = localStorage.getItem('user_role')
  const fromAdmin = props.adminEmbedded || role === 'ADMIN' || role === 'SUPER_ADMIN'
  router.push(fromAdmin ? `/chat/thread/${row.threadId}?from=admin` : `/chat/thread/${row.threadId}`)
}

const rowClassName = ({ row }) => {
  return row?.unreadCount && Number(row.unreadCount) > 0 ? 'row-unread' : ''
}

onMounted(() => {
  loadInbox()
})

onActivated(() => {
  loadInbox()
})
</script>

<style scoped>
.inbox-wrap {
  max-width: 960px;
  margin: 0 auto;
  padding: 20px;
  min-height: 100vh;
  background:
    radial-gradient(800px 360px at 10% 0%, rgba(31, 122, 111, 0.14), transparent 60%),
    linear-gradient(180deg, #f8f4ee 0%, #eef4f8 100%);
}
.page-title {
  font-weight: 700;
  font-size: 18px;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
}
.inbox-card {
  margin-top: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(31, 122, 111, 0.08);
  box-shadow: var(--shadow-1);
  background: rgba(255, 255, 255, 0.95);
}
.inbox-wrap.embedded-mode .inbox-card {
  margin-top: 0;
}
.card-h {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}
.inbox-table :deep(tbody tr) {
  cursor: pointer;
}
.inbox-table :deep(th) {
  background: rgba(31, 122, 111, 0.06);
}
.inbox-table :deep(.row-unread td) {
  background: #fff7e6 !important;
}
.unread-badge {
  position: relative;
  z-index: 5;
}
.unread-badge :deep(.el-badge__content) {
  z-index: 6;
}
.unread-badge :deep(.el-badge__content) {
  top: -2px;
  right: -6px;
}
.peer-name {
  display: inline-block;
  padding-right: 6px;
}

@media (max-width: 768px) {
  .inbox-wrap {
    padding: 10px;
  }
  .card-h {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .page-title {
    font-size: 16px;
  }
  :deep(.el-card__header),
  :deep(.el-card__body) {
    padding: 12px;
  }
  .inbox-table :deep(.el-table__inner-wrapper) {
    min-width: 720px;
  }
  .inbox-table :deep(.el-table__body-wrapper),
  .inbox-table :deep(.el-table__header-wrapper) {
    overflow-x: auto;
  }
}
</style>
