<template>
  <div class="inbox-wrap">
    <el-page-header @back="router.push('/')">
      <template #content>
        <span class="page-title">我的私信</span>
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
        description="暂无会话。可在商品页「联系卖家」或订单里「私信」开始聊天。"
      />
      <el-table
        v-else
        v-loading="loading"
        :data="rows"
        stripe
        class="inbox-table"
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

<script setup>
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
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
  router.push(`/chat/thread/${row.threadId}`)
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
  background: #f5f7fa;
}
.page-title {
  font-weight: 600;
  font-size: 16px;
}
.inbox-card {
  margin-top: 16px;
  border-radius: 8px;
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
.unread-badge :deep(.el-badge__content) {
  top: -2px;
  right: -6px;
}
.peer-name {
  display: inline-block;
  padding-right: 6px;
}
</style>
