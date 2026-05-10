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
        <div>
          <img :src="getImageUrl(activeImage || imageList[0] || '')" class="img" />
          <div v-if="imageList.length > 1" class="thumbs">
            <img
              v-for="(u, i) in imageList"
              :key="`img-${i}`"
              :src="getImageUrl(u)"
              class="thumb"
              :class="{ active: (activeImage || imageList[0]) === u }"
              @click="activeImage = u"
            />
          </div>
        </div>
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
              <el-radio-group v-model="selectedColor">
                <el-radio-button v-for="c in specColors" :key="c" :label="c">{{ c }}</el-radio-button>
              </el-radio-group>
            </div>
            <div v-if="specSizes.length" style="margin-top:8px;">尺寸：
              <el-radio-group v-model="selectedSize">
                <el-radio-button v-for="s in specSizes" :key="s" :label="s">{{ s }}</el-radio-button>
              </el-radio-group>
            </div>
          </div>
          <div style="margin-top:12px;">
            <el-input-number v-model="buyQty" :min="1" :max="99" style="margin-right:8px;" />
            <el-button type="warning" :loading="addingCart" @click="addToCartFromDetail">加入购物车</el-button>
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
const selectedColor = ref('')
const selectedSize = ref('')
const buyQty = ref(1)
const addingCart = ref(false)
const imageList = ref<string[]>([])
const activeImage = ref('')
const parseImages = (image: any, imageUrl: any): string[] => {
  const parse = (v: any): string[] => {
    if (!v) return []
    if (Array.isArray(v)) return v.filter(Boolean)
    const s = String(v)
    if (s.startsWith('[')) {
      try { return (JSON.parse(s) || []).filter(Boolean) } catch { return [] }
    }
    return [s]
  }
  return Array.from(new Set([...parse(image), ...parse(imageUrl)].filter(Boolean)))
}
const getImageUrl = (url) => resolveImageUrl(url)

onMounted(async () => {
  const id = route.params.id
  if (!id) return
  detail.value = await request.get('/product/detail', { params: { id } })
  imageList.value = parseImages(detail.value?.image, detail.value?.imageUrl)
  activeImage.value = imageList.value[0] || ''
  try {
    const spec = detail.value?.specJson ? JSON.parse(detail.value.specJson) : {}
    specColors.value = Array.isArray(spec.colors) ? spec.colors : []
    specSizes.value = Array.isArray(spec.sizes) ? spec.sizes : []
    selectedColor.value = specColors.value[0] || ''
    selectedSize.value = specSizes.value[0] || ''
  } catch {
    specColors.value = []
    specSizes.value = []
    selectedColor.value = ''
    selectedSize.value = ''
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

const addToCartFromDetail = async () => {
  if (!detail.value?.id) return
  addingCart.value = true
  try {
    const parts = []
    if (selectedColor.value) parts.push(`颜色:${selectedColor.value}`)
    if (selectedSize.value) parts.push(`尺寸:${selectedSize.value}`)
    const selectedSpec = parts.join('，')
    const qty = Number(buyQty.value || 1)
    for (let i = 0; i < qty; i++) {
      await request.post(`/cart/add-with-spec?productId=${detail.value.id}&selectedSpec=${encodeURIComponent(selectedSpec)}`)
    }
  } catch (e) {
    console.error(e)
  } finally {
    addingCart.value = false
  }
}
</script>

<style scoped>
.page { padding: 20px; }
.detail { display: flex; gap: 20px; }
.img { width: 360px; height: 360px; object-fit: cover; border-radius: 8px; }
.thumbs { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; max-width: 360px; }
.thumb { width: 64px; height: 64px; object-fit: cover; border-radius: 6px; border: 2px solid transparent; cursor: pointer; }
.thumb.active { border-color: #409EFF; }
.info { flex: 1; }
</style>
