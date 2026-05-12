<template>
  <div class="mall-container">
    <el-header class="mall-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')">
          <el-icon size="28" color="#409EFF"><ShoppingBag /></el-icon>
          <span class="logo-text">杰物 Jemall</span>
        </div>

        <div class="search-box">
          <div class="search-row">
            <el-input v-model="searchQuery" placeholder="搜索你想要的商品..." class="search-input" clearable @keyup.enter="handleSearch" @clear="handleSearch" />
            <el-segmented v-model="searchMode" :options="searchModeOptions" size="small" @change="handleSearch" />
            <el-button type="primary" :icon="Search" :loading="productLoading" @click="handleSearch">搜索</el-button>
          </div>
          <div class="hot-keywords">
            <span class="hot-label">热门搜索：</span>
            <el-tag v-for="k in hotKeywords" :key="k" size="small" class="hot-tag" @click="quickSearch(k)">{{ k }}</el-tag>
          </div>
        </div>

        <div class="user-actions">
          <el-button type="warning" plain round @click="openFavoriteDrawer">我的收藏</el-button>
          <el-badge :value="threadCount" :hidden="threadCount === 0" :max="99" class="msg-badge">
            <el-icon size="24" class="action-icon" title="站内消息" @click="router.push('/messages')"><ChatDotRound /></el-icon>
          </el-badge>
          <el-badge :value="cartList.length" class="cart-badge" :hidden="cartList.length === 0">
            <el-icon size="24" class="action-icon" @click="cartVisible = true"><ShoppingCart /></el-icon>
          </el-badge>

          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-badge :value="orderNoticeTotal" :hidden="orderNoticeTotal === 0" :max="99" class="avatar-badge" :offset="[10, -4]">
                <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              </el-badge>
              <span class="nickname">{{ userInfo.nickname || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/messages')">站内消息</el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item v-if="userInfo.role === 'ADMIN' || userInfo.role === 'SUPER_ADMIN'" @click="router.push('/admin')">进入后台</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <div class="mall-main">
      <el-carousel height="300px" class="banner-carousel" indicator-position="outside">
        <el-carousel-item v-for="banner in banners" :key="banner.id || banner.title">
          <div class="banner-content" :style="{ backgroundColor: banner.color || banner.bgColor }">
            <h2>{{ banner.title }}</h2>
            <p>{{ banner.subtitle }}</p>
          </div>
        </el-carousel-item>
      </el-carousel>

      <el-alert v-for="n in notices" :key="n.id" :title="n.title" :description="n.content" type="info" show-icon :closable="false" style="margin-bottom:10px;" />

      <div class="category-tabs">
        <div style="display:flex;justify-content:space-between;align-items:center;gap:12px;flex-wrap:wrap;">
          <el-tabs v-model="activeCategory" @tab-change="handleSearch">
            <el-tab-pane label="🚀 全部商品" name="" />
            <el-tab-pane v-for="c in categories" :key="c.id || c.name" :label="`${c.icon || '📦'} ${c.name}`" :name="c.name" />
          </el-tabs>
          <el-select v-model="sortBy" placeholder="排序方式" style="width: 180px;" @change="handleSearch">
            <el-option label="新品优先" value="new_desc" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="销量优先" value="sales_desc" />
          </el-select>
        </div>
      </div>

      <div class="featured-grid">
        <el-card class="feature-card" shadow="never">
          <template #header>🔥 热门推荐</template>
          <div class="feature-list">
            <div v-for="item in hotProducts" :key="`hot-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name">{{ item.name }}</span><span class="f-price">￥{{ item.price }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="feature-card" shadow="never">
          <template #header>🆕 新品上架</template>
          <div class="feature-list">
            <div v-for="item in newProducts" :key="`new-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name">{{ item.name }}</span><span class="f-price">￥{{ item.price }}</span>
            </div>
          </div>
        </el-card>
        <el-card class="feature-card" shadow="never">
          <template #header>⚡ 促销专区</template>
          <div class="feature-list">
            <div v-for="item in promoProducts" :key="`promo-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name">{{ item.name }}</span><span class="f-price">￥{{ item.price }}</span>
            </div>
            <el-empty v-if="promoProducts.length === 0" description="暂无促销商品" :image-size="56" />
          </div>
        </el-card>
      </div>

      <el-row :gutter="20" class="product-grid">
        <el-col :span="6" v-for="(item, index) in pagedProductList" :key="item.id" style="margin-bottom:20px;">
          <el-card shadow="hover" :body-style="{ padding: '0px' }" class="product-card" :style="{ '--i': index }" @click="router.push(`/product/${item.id}`)">
            <div class="image-placeholder" :style="{ background: item.bgColor }">
              <el-icon v-if="!pickFirstImage(item.image, item.imageUrl)" size="40" color="#fff"><Picture /></el-icon>
              <img v-else :src="getImageUrl(pickFirstImage(item.image, item.imageUrl))" class="product-img" />
            </div>
            <div class="product-info">
              <h3 class="title line-clamp-1">{{ item.name }}</h3>
              <p class="desc line-clamp-2">{{ item.description || '这件宝贝还没有详细描述哦~' }}</p>
              <div class="price-row">
                <span class="price">¥ {{ item.price }}</span>
                <span class="original-price" v-if="item.originalPrice > 0">原价 ¥{{ item.originalPrice }}</span>
              </div>
              <div class="location-tag"><el-icon><Location /></el-icon><span>{{ item.sellerAddress || '平台配送' }}</span></div>
              <div class="seller-info">
                <div class="seller-tag">
                  <el-avatar :size="20" :src="item.sellerAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                  <span class="seller-name">{{ item.sellerName || '平台商家' }}</span>
                </div>
                <div class="seller-actions">
                  <el-button type="warning" size="small" plain @click.stop="toggleFavorite(item)">{{ favoriteSet.has(item.id) ? '取消收藏' : '收藏' }}</el-button>
                  <el-button type="info" size="small" plain @click.stop="openReviewDialog(item)">评价</el-button>
                  <el-button type="success" size="small" plain @click.stop="contactSeller(item)">咨询商家</el-button>
                  <el-button type="primary" size="small" circle :icon="ShoppingCart" @click.stop="addToCart(item)" />
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <div style="display:flex;justify-content:flex-end;margin-top:8px;">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="productList.length" v-model:current-page="pageNo" v-model:page-size="pageSize" :page-sizes="[8,12,16,20]" />
      </div>
    </div>

    <el-drawer v-model="cartVisible" title="我的购物车" size="400px">
      <div v-if="cartList.length === 0" class="empty-cart"><el-icon size="60" color="#c0c4cc"><ShoppingCart /></el-icon><p>购物车空空如也，快去挑点宝贝吧~</p></div>
      <div v-else class="cart-list">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;"><el-checkbox v-model="allChecked" @change="toggleAll">全选 / 反选</el-checkbox></div>
        <div v-for="item in cartList" :key="item.id" class="cart-item">
          <el-checkbox v-model="checkedMap[item.productId || item.id]" />
          <img v-if="pickFirstImage(item.image, item.imageUrl)" :src="getImageUrl(pickFirstImage(item.image, item.imageUrl))" class="cart-item-img" />
          <div v-else class="cart-item-img placeholder"><el-icon><Picture /></el-icon></div>
          <div class="cart-item-info"><h4 class="cart-item-title">{{ item.productName || item.name || '未知商品' }}</h4><span class="cart-item-price">¥ {{ item.price }}</span></div>
          <el-input-number :model-value="item.quantity || 1" :min="1" :max="99" size="small" @change="(v)=>updateCartQuantity(item, v)" />
          <el-button type="danger" size="small" plain @click="removeFromCart(item)">移除</el-button>
        </div>
        <div class="cart-footer">
          <div style="margin-bottom:10px;"><el-select v-model="selectedCouponId" clearable placeholder="选择优惠券（可选）" style="width:100%;"><el-option v-for="c in myCoupons" :key="c.id" :label="`${c.title}（满${c.thresholdAmount}减${c.discountAmount}）`" :value="c.id" /></el-select></div>
          <div class="total-price">合计: <span>¥ {{ cartTotalPrice }}</span></div>
          <el-button type="success" size="large" style="width:100%;" @click="openCheckoutConfirm">马上结算</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="checkoutConfirmVisible" title="确认订单" width="560px">
      <el-form label-width="96px">
        <el-form-item label="收货地址" required><el-select v-model="selectedAddressId" style="width:100%;" placeholder="请选择收货地址"><el-option v-for="a in addressList" :key="a.id" :label="formatAddressOption(a)" :value="a.id" /></el-select></el-form-item>
        <el-form-item label="支付方式" required><el-radio-group v-model="paymentMethod"><el-radio value="ALIPAY">支付宝</el-radio><el-radio value="WECHAT">微信支付</el-radio><el-radio value="CAMPUS_COD">货到付款</el-radio></el-radio-group></el-form-item>
        <el-form-item label="订单金额"><span style="color:#e76f51;font-size:20px;font-weight:700;">¥ {{ cartTotalPrice }}</span></el-form-item>
      </el-form>
      <template #footer><el-button @click="checkoutConfirmVisible=false">取消</el-button><el-button type="primary" :loading="checkoutLoading" @click="handleCheckout">提交订单</el-button></template>
    </el-dialog>

    <el-drawer v-model="favoriteVisible" title="我的收藏" size="460px">
      <el-empty v-if="favoriteProducts.length === 0" description="你还没有收藏商品" />
      <div v-else class="fav-list">
        <div v-for="item in favoriteProducts" :key="item.id" class="fav-item">
          <img v-if="pickFirstImage(item.image, item.imageUrl)" :src="getImageUrl(pickFirstImage(item.image, item.imageUrl))" class="fav-img" />
          <div v-else class="fav-img placeholder"><el-icon><Picture /></el-icon></div>
          <div class="fav-info">
            <div class="fav-name">{{ item.name }}</div>
            <div class="fav-price">￥{{ item.price }}</div>
            <div class="fav-actions">
              <el-button size="small" type="primary" plain @click="addToCart(item)">加入购物车</el-button>
              <el-button size="small" type="success" plain @click="contactSeller(item)">咨询商家</el-button>
              <el-button size="small" type="danger" plain @click="toggleFavorite(item)">取消收藏</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="reviewVisible" :title="`商品评价 - ${currentReviewProduct?.name || ''}`" width="640px">
      <el-form label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" />
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="请输入评价内容" />
        </el-form-item>
        <el-form-item label="评价图片">
          <el-upload
            :action="uploadActionUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/jpeg,image/jpg,image/png,image/webp,image/gif"
            name="file"
            :on-success="handleReviewUploadSuccess"
          >
            <el-button type="primary" plain>本地上传图片</el-button>
          </el-upload>
          <img v-if="reviewForm.imageUrl" :src="getImageUrl(reviewForm.imageUrl)" style="margin-left:10px;width:72px;height:72px;object-fit:cover;border-radius:6px;" />
        </el-form-item>
      </el-form>
      <div style="margin: 8px 0 12px; font-weight: 600;">历史评价</div>
      <el-empty v-if="!reviewList.length" description="暂无评价" />
      <div v-else style="max-height:260px;overflow:auto;">
        <div v-for="r in reviewList" :key="r.id" style="padding:10px 0;border-bottom:1px solid #f0f0f0;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span>{{ r.nickname || r.username || `用户#${r.userId || '-'}` }}</span>
            <div style="display:flex;align-items:center;gap:10px;">
              <el-rate :model-value="Number(r.rating || 0)" disabled />
              <el-button
                v-if="Number(r.userId) === Number(userInfo?.id || userInfo?.userId || 0)"
                type="danger"
                link
                size="small"
                @click="deleteMyReview(r)"
              >
                删除
              </el-button>
            </div>
          </div>
          <div style="margin-top:6px;color:#606266;">{{ r.content }}</div>
          <img v-if="r.imageUrl" :src="getImageUrl(r.imageUrl)" style="margin-top:8px;width:96px;height:96px;object-fit:cover;border-radius:6px;" />
        </div>
      </div>
      <template #footer>
        <el-button @click="reviewVisible=false">关闭</el-button>
        <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch, onBeforeUnmount, onActivated } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingBag, Search, ShoppingCart, Picture, Location, ChatDotRound } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveImageUrl, uploadActionUrl } from '../utils/env'

const router = useRouter()
const route = useRoute()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

const searchQuery = ref('')
const searchMode = ref('fuzzy')
const searchModeOptions = [
  { label: '模糊', value: 'fuzzy' },
  { label: '精准', value: 'exact' }
]
const hotKeywords = ref(['手机', '耳机', '笔记本', '图书', '运动鞋'])
const activeCategory = ref('')
const sortBy = ref('new_desc')
const productList = ref<any[]>([])
const productLoading = ref(false)
const pageNo = ref(1)
const pageSize = ref(8)
const pagedProductList = computed(() => productList.value.slice((pageNo.value - 1) * pageSize.value, (pageNo.value - 1) * pageSize.value + pageSize.value))
const hotProducts = computed(() => [...productList.value].sort((a, b) => Number(b.sales || b.saleCount || 0) - Number(a.sales || a.saleCount || 0)).slice(0, 4))
const newProducts = computed(() => [...productList.value].sort((a, b) => Number(b.id || 0) - Number(a.id || 0)).slice(0, 4))
const promoProducts = computed(() => productList.value.filter((x) => Number(x.isSeckill) === 1 || Number(x.originalPrice || 0) > Number(x.price || 0)).slice(0, 4))

const favoriteSet = ref(new Set<number>())
const favoriteVisible = ref(false)
const favoriteProducts = ref<any[]>([])
const reviewVisible = ref(false)
const currentReviewProduct = ref<any>(null)
const reviewList = ref<any[]>([])
const reviewSubmitting = ref(false)
const reviewForm = ref({ rating: 5, content: '', imageUrl: '' })
const uploadHeaders = (() => {
  const tk = localStorage.getItem('token') || ''
  return tk ? { Authorization: `Bearer ${tk}` } : {}
})()

const defaultBanners = [
  { title: '杰物 618 狂欢', subtitle: '爆款好物限时直降，低价不等人', color: '#a0cfff' },
  { title: '数码焕新季', subtitle: '手机/平板/耳机爆款上新，品质优选', color: '#f3d19e' }
]
const banners = ref<any[]>([...defaultBanners])
const notices = ref<any[]>([])
const categories = ref<any[]>([])

const threadCount = ref(0)
const orderNotice = ref({ buyer: 0, seller: 0, total: 0 })
const orderNoticeTotal = computed(() => Number(orderNotice.value.total || 0))

const cartVisible = ref(false)
const cartList = ref<any[]>([])
const myCoupons = ref<any[]>([])
const selectedCouponId = ref<number | null>(null)
const addressList = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const paymentMethod = ref('ALIPAY')
const checkoutConfirmVisible = ref(false)
const checkedMap = ref<Record<string, boolean>>({})
const allChecked = ref(false)
const checkoutLoading = ref(false)

const cartTotalPrice = computed(() => cartList.value.reduce((t, item) => {
  const id = item.productId || item.id
  return checkedMap.value[id] ? t + Number(item.price || 0) * Number(item.quantity || 1) : t
}, 0).toFixed(2))

const pickFirstImage = (image: any, imageUrl: any) => {
  const parse = (v: any): string[] => {
    if (!v) return []
    if (Array.isArray(v)) return v.filter(Boolean)
    const s = String(v)
    if (s.startsWith('[')) {
      try { return (JSON.parse(s) || []).filter(Boolean) } catch { return [] }
    }
    return [s]
  }
  const list = [...parse(image), ...parse(imageUrl)]
  return list[0] || ''
}
const getImageUrl = (url: string) => resolveImageUrl(url)
const handleSearch = async () => {
  await fetchProducts()
}
const quickSearch = (k: string) => {
  searchQuery.value = k
  handleSearch()
}

const contactSeller = (item: any) => {
  const me = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (me.id != null && item.sellerId != null && Number(me.id) === Number(item.sellerId)) return ElMessage.info('这是您负责的商品，无需重复咨询')
  router.push(`/chat/product/${item.id}`)
}

const refreshFavoriteProducts = () => {
  favoriteProducts.value = productList.value.filter((x) => favoriteSet.value.has(Number(x.id)))
}

const fetchProducts = async () => {
  productLoading.value = true
  try {
    const res = await request.get('/product/list', { params: { keyword: searchQuery.value.trim(), searchMode: searchMode.value, category: activeCategory.value, sortBy: sortBy.value } })
    const colors = ['#ffb8b8', '#b8e9ff', '#b8ffc9', '#ffdfb8', '#e1b8ff', '#b8fff4']
    productList.value = (res || []).map((x: any, i: number) => ({ ...x, bgColor: colors[i % colors.length] }))
    pageNo.value = 1
    refreshFavoriteProducts()
  } finally {
    productLoading.value = false
  }
}
const fetchFavorites = async () => {
  try {
    const ids = await request.get('/product/favorite/list')
    favoriteSet.value = new Set((ids || []).map((id: any) => Number(id)))
  } catch { favoriteSet.value = new Set() }
  refreshFavoriteProducts()
}
const openFavoriteDrawer = async () => { if (!productList.value.length) await fetchProducts(); refreshFavoriteProducts(); favoriteVisible.value = true }
const toggleFavorite = async (item: any) => {
  const res = await request.post(`/product/favorite/toggle?productId=${item.id}`)
  if (res?.favorited) favoriteSet.value.add(Number(item.id)); else favoriteSet.value.delete(Number(item.id))
  ElMessage.success(res?.favorited ? '收藏成功' : '已取消收藏')
  refreshFavoriteProducts()
}

const openReviewDialog = async (item: any) => { currentReviewProduct.value = item; reviewVisible.value = true; reviewForm.value = { rating: 5, content: '', imageUrl: '' }; await loadReviewList() }
const handleReviewUploadSuccess = (resp: any) => {
  const path = String(resp?.data || '').trim()
  if (!path) return
  reviewForm.value.imageUrl = path
}
const loadReviewList = async () => {
  if (!currentReviewProduct.value?.id) return
  reviewList.value = await request.get('/product/review/list', { params: { productId: currentReviewProduct.value.id } }) || []
}
const submitReview = async () => {
  const content = reviewForm.value.content.trim()
  const imageUrl = reviewForm.value.imageUrl?.trim() || ''
  if (!currentReviewProduct.value?.id) return
  if (!content && !imageUrl) {
    ElMessage.warning('请填写评价内容或上传评价图片')
    return
  }
  reviewSubmitting.value = true
  try {
    await request.post('/product/review/add', { productId: currentReviewProduct.value.id, rating: reviewForm.value.rating, content, imageUrl })
    reviewForm.value.content = ''
    reviewForm.value.imageUrl = ''
    ElMessage.success('评价提交成功')
    await loadReviewList()
  } finally { reviewSubmitting.value = false }
}

const deleteMyReview = async (row: any) => {
  if (!row?.id) return
  try {
    await request.post(`/product/review/delete?id=${row.id}`)
    ElMessage.success('评价已删除')
    await loadReviewList()
  } catch (e) {
    console.error(e)
  }
}

const fetchThreadCount = async () => { try { threadCount.value = Number(await request.get('/order/chat/unread-count') || 0) } catch { threadCount.value = 0 } }
const fetchOrderNoticeCount = async () => { try { orderNotice.value = await request.get('/order/notice/unread-count') || { buyer: 0, seller: 0, total: 0 } } catch { orderNotice.value = { buyer: 0, seller: 0, total: 0 } } }
const handleOrderNoticeEvent = (e: any) => { if (e?.detail) orderNotice.value = e.detail; else fetchOrderNoticeCount() }

const fetchBannersAndNotices = async () => {
  try { const x = await request.get('/user/banner/list'); banners.value = Array.isArray(x) && x.length ? x : [...defaultBanners] } catch { banners.value = [...defaultBanners] }
  try { notices.value = await request.get('/user/notice/list?limit=3') || [] } catch { notices.value = [] }
}
const fetchCategories = async () => {
  try {
    const list = await request.get('/product/category/list')
    if (Array.isArray(list) && list.length) categories.value = list
    else categories.value = [
      { id: 1, name: '数码电子', icon: '💻' },
      { id: 2, name: '书籍资料', icon: '📚' },
      { id: 3, name: '衣物鞋帽', icon: '👗' },
      { id: 4, name: '生活用品', icon: '🧴' },
      { id: 5, name: '其他商品', icon: '📦' }
    ]
  } catch { categories.value = [] }
}

const fetchCart = async () => {
  cartList.value = await request.get('/cart/list') || []
  const next: Record<string, boolean> = {}
  cartList.value.forEach((it: any) => { const id = it.productId || it.id; next[id] = checkedMap.value[id] ?? true })
  checkedMap.value = next
  allChecked.value = cartList.value.length > 0 && cartList.value.every((x: any) => checkedMap.value[x.productId || x.id])
}
const fetchMyCoupons = async () => { try { myCoupons.value = await request.get('/order/coupon/my') || [] } catch { myCoupons.value = [] } }
const fetchAddressList = async () => {
  try {
    addressList.value = await request.get('/user/address/list') || []
    const def = addressList.value.find((x: any) => Number(x.isDefault) === 1) || addressList.value[0]
    if (def?.id) selectedAddressId.value = def.id
  } catch { addressList.value = [] }
}
const formatAddressOption = (a: any) => `${a.receiver || a.contactName || '收货人'} ${a.phone || ''} / ${a.province || ''}${a.city || ''}${a.district || ''} ${a.detailAddress || ''}`

const openCheckoutConfirm = async () => {
  const selected = cartList.value.filter((x: any) => checkedMap.value[x.productId || x.id])
  if (!selected.length) return ElMessage.warning('请先勾选要结算的商品')
  await fetchAddressList()
  if (!addressList.value.length) return ElMessage.warning('请先到个人资料页添加收货地址')
  checkoutConfirmVisible.value = true
}
const toggleAll = (v: boolean) => { cartList.value.forEach((x: any) => { checkedMap.value[x.productId || x.id] = !!v }) }
const updateCartQuantity = async (item: any, v: any) => { await request.post(`/cart/quantity?productId=${item.productId || item.id}&quantity=${Number(v || 1)}`); item.quantity = Number(v || 1) }
const addToCart = async (product: any) => {
  const me = userInfo.value || {}
  if (me.id != null && product?.sellerId != null && Number(me.id) === Number(product.sellerId)) return ElMessage.warning('不能把自己的商品加入购物车')
  let selectedSpec = ''
  try { const spec = product.specJson ? JSON.parse(product.specJson) : {}; selectedSpec = [spec.colors?.[0], spec.sizes?.[0]].filter(Boolean).join('/') } catch {}
  await request.post(`/cart/add-with-spec?productId=${product.id}&selectedSpec=${encodeURIComponent(selectedSpec)}`)
  ElMessage.success(`《${product.name}》已加入购物车！`)
  fetchCart()
}
const removeFromCart = async (item: any) => { await request.post(`/cart/remove?productId=${item.productId || item.product_id || item.id}`); ElMessage.success('已从购物车移除'); fetchCart() }

const handleCheckout = async () => {
  const selected = cartList.value.filter((x: any) => checkedMap.value[x.productId || x.id])
  if (!selected.length) return ElMessage.warning('请先勾选要结算的商品')
  if (!selectedAddressId.value) return ElMessage.warning('请选择收货地址')
  if (!paymentMethod.value) return ElMessage.warning('请选择支付方式')
  checkoutLoading.value = true
  try {
    const couponParam = selectedCouponId.value ? `&couponId=${selectedCouponId.value}` : ''
    const productIds = selected.map((x: any) => x.productId || x.id).filter(Boolean).join(',')
    await request.post(`/order/checkout?totalAmount=${cartTotalPrice.value}&addressId=${selectedAddressId.value}&productIds=${encodeURIComponent(productIds)}${couponParam}`)
    await ElMessageBox.alert('订单已生成，请前往订单中心完成支付', '🎉 下单成功', { confirmButtonText: '去支付', type: 'success', center: true })
    cartVisible.value = false
    checkoutConfirmVisible.value = false
    await fetchCart(); await fetchProducts(); await fetchOrderNoticeCount()
    router.push('/orders')
  } finally { checkoutLoading.value = false }
}

watch(() => route.path, (p) => {
  if (p === '/') {
    userInfo.value = JSON.parse(localStorage.getItem('userInfo') || '{}')
    fetchThreadCount(); fetchOrderNoticeCount()
  }
})

onMounted(() => {
  fetchProducts(); fetchCart(); fetchFavorites(); fetchThreadCount(); fetchOrderNoticeCount(); fetchMyCoupons(); fetchBannersAndNotices(); fetchCategories()
  window.addEventListener('order-notice-count', handleOrderNoticeEvent)
})
onActivated(() => { fetchOrderNoticeCount() })
onBeforeUnmount(() => { window.removeEventListener('order-notice-count', handleOrderNoticeEvent) })

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出账号吗？', '提示', { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' })
    .then(() => { localStorage.clear(); router.push('/login') })
    .catch(() => {})
}
</script>

<style scoped>
.mall-container { min-height: 100vh; background: radial-gradient(900px 420px at 10% -10%, rgba(31,122,111,.18), transparent 60%), radial-gradient(700px 320px at 90% 5%, rgba(244,162,97,.2), transparent 60%), linear-gradient(180deg, #f8f4ee 0%, #eef4f8 55%, #edf3f7 100%); }
.mall-header { background: rgba(255,255,255,.86); backdrop-filter: blur(8px); border-bottom: 1px solid rgba(31,122,111,.08); box-shadow: 0 8px 24px rgba(0,0,0,.06); position: sticky; top: 0; z-index: 100; padding: 0; }
.header-content { max-width: 1200px; margin: 0 auto; height: 64px; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-text { font-size: 22px; font-weight: 700; font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif; color: var(--brand); }
.search-box { width: 560px; }
.search-row { display: flex; align-items: center; gap: 8px; }
.search-input { flex: 1; min-width: 220px; }
.hot-keywords { margin-top: 6px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.hot-label { font-size: 12px; color: #64748b; }
.hot-tag { cursor: pointer; }
.user-actions { display: flex; align-items: center; gap: 22px; }
.user-profile { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.nickname, .action-icon { color: var(--muted); }
.mall-main { max-width: 1200px; margin: 24px auto; padding: 0 20px 40px; }
.banner-carousel { border-radius: var(--radius-lg); overflow: hidden; margin-bottom: 20px; box-shadow: var(--shadow-1); }
.banner-content { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #fff; text-shadow: 0 2px 10px rgba(0,0,0,.2); }
.category-tabs { background: rgba(255,255,255,.92); padding: 10px 20px 0; border-radius: var(--radius-md); margin-bottom: 12px; box-shadow: var(--shadow-1); }
.featured-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 16px; }
.feature-card { border-radius: var(--radius-md); border: 1px solid rgba(31,122,111,.08); }
.feature-list { display: flex; flex-direction: column; gap: 8px; }
.feature-item { display: flex; justify-content: space-between; align-items: center; background: #f7faf9; border: 1px solid #e5eeea; border-radius: 8px; padding: 8px 10px; cursor: pointer; }
.f-name { max-width: 72%; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
.f-price { color: #e76f51; font-weight: 700; }
.product-card { border-radius: var(--radius-md); border: 1px solid rgba(31,122,111,.08); transition: transform .25s ease, box-shadow .25s ease; cursor: pointer; height: 100%; display: flex; flex-direction: column; background: rgba(255,255,255,.94); box-shadow: var(--shadow-1); }
.product-card:hover { transform: translateY(-6px); box-shadow: var(--shadow-2); }
.image-placeholder { height: 200px; display: flex; justify-content: center; align-items: center; overflow: hidden; background: linear-gradient(135deg, rgba(31,122,111,.25), rgba(244,162,97,.35)); }
.product-img { width: 100%; height: 100%; object-fit: cover; }
.product-info { padding: 16px; flex: 1; display: flex; flex-direction: column; }
.title { font-size: 16px; font-weight: 700; margin: 0 0 6px 0; }
.desc { font-size: 12px; color: #7b8794; margin: 0 0 10px 0; line-height: 1.6; min-height: 36px; }
.line-clamp-1,.line-clamp-2 { display: -webkit-box; -webkit-box-orient: vertical; overflow: hidden; }
.line-clamp-1 { -webkit-line-clamp: 1; }
.line-clamp-2 { -webkit-line-clamp: 2; }
.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; margin-top: auto; }
.price { font-size: 20px; color: #e76f51; font-weight: 700; }
.original-price { font-size: 12px; color: #a1a7b3; text-decoration: line-through; }
.location-tag { font-size: 12px; color: #6b7280; margin-bottom: 8px; display: flex; align-items: center; gap: 4px; }
.seller-info { border-top: 1px solid rgba(31,122,111,.08); padding-top: 10px; display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.seller-tag { display: flex; align-items: center; gap: 6px; min-width: 0; flex: 1; }
.seller-name { font-size: 12px; color: #556070; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.seller-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.empty-cart { text-align: center; color: #909399; margin-top: 80px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.cart-list { display: flex; flex-direction: column; height: 100%; }
.cart-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 0; border-bottom: 1px solid #eee; gap: 15px; }
.cart-item-img { width: 60px; height: 60px; object-fit: cover; border-radius: 10px; flex-shrink: 0; }
.cart-item-info { flex: 1; overflow: hidden; }
.cart-item-title { margin: 0 0 8px 0; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cart-item-price { color: #e76f51; font-weight: 700; }
.cart-footer { margin-top: auto; padding-top: 20px; border-top: 2px solid #eee; }
.fav-list { display: flex; flex-direction: column; gap: 10px; }
.fav-item { display: flex; gap: 10px; padding: 10px; border: 1px solid #ebeef5; border-radius: 8px; }
.fav-img { width: 72px; height: 72px; border-radius: 8px; object-fit: cover; background: #f6f7f8; }
.fav-info { flex: 1; min-width: 0; }
.fav-name { font-size: 14px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fav-price { margin: 6px 0; color: #f56c6c; font-weight: 700; }
.fav-actions { display: flex; gap: 6px; flex-wrap: wrap; }

@media (max-width: 900px) {
  .header-content { height: auto; min-height: 64px; padding: 10px 12px; gap: 10px; flex-wrap: wrap; }
  .search-box { width: 100%; order: 3; }
  .user-actions { margin-left: auto; gap: 10px; }
  .mall-main { padding: 0 12px 20px; margin: 12px auto; }
  .featured-grid { grid-template-columns: 1fr; }
  :deep(.el-col) { max-width: 100% !important; flex: 0 0 100% !important; }
}

@media (max-width: 640px) {
  .mall-header {
    position: sticky;
  }
  .header-content {
    align-items: flex-start;
    padding: 10px 12px 12px;
  }
  .logo {
    gap: 8px;
  }
  .logo-text {
    font-size: 18px;
  }
  .user-actions {
    gap: 12px;
  }
  .user-actions :deep(.el-button) {
    padding: 7px 10px;
  }
  .nickname {
    display: none;
  }
  .search-row {
    display: grid;
    grid-template-columns: 1fr auto;
    gap: 8px;
  }
  .search-input {
    min-width: 0;
    grid-column: 1 / -1;
  }
  .search-row :deep(.el-segmented) {
    max-width: 100%;
    overflow-x: auto;
  }
  .hot-keywords {
    gap: 5px;
    max-height: 58px;
    overflow: hidden;
  }
  .mall-main {
    margin-top: 10px;
    padding: 0 10px 22px;
  }
  .banner-carousel {
    margin-bottom: 14px;
    border-radius: 18px;
  }
  .banner-carousel :deep(.el-carousel__container) {
    height: 176px !important;
  }
  .banner-content {
    padding: 0 20px;
    text-align: center;
  }
  .banner-content h2 {
    font-size: 22px;
    margin: 0 0 8px;
  }
  .banner-content p {
    font-size: 13px;
    margin: 0;
  }
  .category-tabs {
    padding: 8px 10px 0;
    overflow: hidden;
  }
  .category-tabs :deep(.el-tabs__nav-wrap) {
    overflow-x: auto;
  }
  .category-tabs :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
  .category-tabs :deep(.el-select) {
    width: 100% !important;
  }
  .featured-grid {
    gap: 10px;
  }
  .image-placeholder {
    height: 178px;
  }
  .product-info {
    padding: 13px;
  }
  .title {
    font-size: 15px;
  }
  .price {
    font-size: 18px;
  }
  .seller-info {
    align-items: flex-start;
    flex-direction: column;
  }
  .seller-actions {
    width: 100%;
    flex-wrap: wrap;
    gap: 6px;
  }
  .seller-actions :deep(.el-button) {
    margin-left: 0;
  }
  .cart-item {
    align-items: flex-start;
    gap: 10px;
    flex-wrap: wrap;
  }
  .cart-item-info {
    min-width: 0;
    flex: 1 1 calc(100% - 116px);
  }
  .fav-item {
    padding: 8px;
  }
  .fav-actions :deep(.el-button) {
    margin-left: 0;
  }
  :deep(.el-pagination) {
    justify-content: center;
    flex-wrap: wrap;
    gap: 6px;
  }
  :deep(.el-pagination .el-pagination__total),
  :deep(.el-pagination .el-pagination__sizes) {
    display: none;
  }
  :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    margin-top: 5vh !important;
    border-radius: 18px;
  }
  :deep(.el-dialog__body) {
    max-height: 72vh;
    overflow-y: auto;
    padding: 14px 16px;
  }
  :deep(.el-drawer.rtl) {
    width: min(100vw, 420px) !important;
  }
  :deep(.el-form-item) {
    display: block;
  }
  :deep(.el-form-item__label) {
    justify-content: flex-start;
    width: auto !important;
    margin-bottom: 6px;
  }
  :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
}
</style>
