<template>
  <div class="page">
    <el-card>
      <template #header>
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <span>商品详情</span>
          <el-button link type="primary" @click="router.push('/')">返回商城</el-button>
        </div>
      </template>
      <div v-if="detail" class="detail">
        <img :src="getImageUrl(detail.image || detail.imageUrl)" class="img" />
        <div class="info">
          <h2>{{ detail.name }}</h2>
          <p>{{ detail.description || '暂无描述' }}</p>
          <p>分类：{{ detail.category || '-' }}</p>
          <p>价格：￥{{ detail.price }}</p>
          <p v-if="Number(detail.isSeckill) === 1 && detail.seckillPrice">秒杀价：<span style="color:#f56c6c;font-weight:700;">￥{{ detail.seckillPrice }}</span></p>
          <p>库存：{{ detail.stock }}</p>
          <p>销量：{{ detail.salesCount || 0 }}</p>
          <div v-if="specColors.length || specSizes.length" style="margin:10px 0;">
            <div v-if="specColors.length">颜色：
              <el-tag v-for="c in specColors" :key="c" style="margin-right:6px;">{{ c }}</el-tag>
            </div>
            <div v-if="specSizes.length" style="margin-top:8px;">尺寸：
              <el-tag v-for="s in specSizes" :key="s" type="success" style="margin-right:6px;">{{ s }}</el-tag>
            </div>
          </div>
          <div style="margin-top:12px;">
            <el-button type="primary" @click="receiveCoupon">领取优惠券</el-button>
            <el-button type="success" @click="router.push('/')">返回选购</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { resolveImageUrl } from '../utils/env'

const route = useRoute()
const router = useRouter()
const detail = ref<any>(null)
const specColors = ref<string[]>([])
const specSizes = ref<string[]>([])
const getImageUrl = (url) => resolveImageUrl(url)

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  detail.value = await request.get('/product/detail', { params: { id } })
  try {
    const spec = detail.value?.specJson ? JSON.parse(detail.value.specJson) : {}
    specColors.value = Array.isArray(spec.colors) ? spec.colors : []
    specSizes.value = Array.isArray(spec.sizes) ? spec.sizes : []
  } catch {
    specColors.value = []
    specSizes.value = []
  }
})

const receiveCoupon = async () => {
  try {
    const list = await request.get('/order/coupon/list')
    if (!Array.isArray(list) || list.length === 0) return
    await request.post(`/order/coupon/receive?couponId=${list[0].id}`)
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.page { padding: 20px; }
.detail { display: flex; gap: 20px; }
.img { width: 360px; height: 360px; object-fit: cover; border-radius: 8px; }
.info { flex: 1; }
</style>
