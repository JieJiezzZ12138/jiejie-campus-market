<template>
  <div class="mall-container">
    <!-- ===== 一级导航 ===== -->
    <el-header class="mall-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')">
          <el-icon size="28" color="#1f7a6f"><ShoppingBag /></el-icon>
          <span class="logo-text">杰物 Jemall</span>
        </div>

        <div class="search-box">
          <div class="search-row">
            <el-select v-model="searchCategory" placeholder="全部分类" class="search-cat" size="default" style="width:110px;" @change="handleSearch">
              <el-option label="全部分类" value="" />
              <el-option v-for="c in categories" :key="c.id || c.name" :label="c.name || c" :value="c.name || c" />
            </el-select>
            <el-input v-model="searchQuery" placeholder="搜索你想要的商品..." class="search-input" clearable @keyup.enter="handleSearch" @clear="handleSearch">
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" :loading="productLoading" @click="handleSearch" class="search-btn">搜索</el-button>
          </div>
          <div class="hot-keywords">
            <span class="hot-label">热门搜索：</span>
            <el-tag v-for="k in hotKeywords" :key="k" size="small" class="hot-tag" effect="plain" @click="quickSearch(k)">{{ k }}</el-tag>
          </div>
        </div>

        <div class="user-actions">
          <el-button text class="fav-btn" @click="openFavoriteDrawer">
            <el-icon><Star /></el-icon>
            <span>收藏</span>
          </el-button>
          <el-badge :value="threadCount" :hidden="threadCount === 0" :max="99" class="msg-badge">
            <el-button text class="action-btn" title="站内消息" @click="router.push('/messages')">
              <el-icon size="20"><ChatDotRound /></el-icon>
            </el-button>
          </el-badge>
          <el-badge :value="cartList.length" class="cart-badge" :hidden="cartList.length === 0">
            <el-button text class="action-btn" @click="cartVisible = true">
              <el-icon size="20"><ShoppingCart /></el-icon>
            </el-button>
          </el-badge>

          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-badge :value="orderNoticeTotal" :hidden="orderNoticeTotal === 0" :max="99" class="avatar-badge" :offset="[8, -2]">
                <el-avatar :size="30" :src="userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
              </el-badge>
              <span class="nickname">{{ userInfo.nickname || userInfo.username || '用户' }}</span>
              <el-icon size="14" style="color:#999;"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="router.push('/messages')">站内消息</el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <!-- ===== 二级导航 (分类导航条) ===== -->
    <div class="sub-nav">
      <div class="sub-nav-inner">
        <div class="sub-nav-item all-cat">
          <el-icon><Grid /></el-icon>
          <span>全部商品分类</span>
        </div>
        <div
          v-for="c in categories"
          :key="c.id || c.name"
          class="sub-nav-item"
          :class="{ active: activeCategory === (c.name || c) }"
          @click="switchCategory(c.name || c)"
        >
          {{ c.icon || '•' }} {{ c.name || c }}
        </div>
        <div class="sub-nav-spacer" />
        <div class="sub-nav-item sort-item">
          <el-select v-model="sortBy" placeholder="排序方式" size="small" @change="handleSearch" style="width:130px;">
            <el-option label="综合排序" value="" />
            <el-option label="新品优先" value="new_desc" />
            <el-option label="价格从低到高" value="price_asc" />
            <el-option label="价格从高到低" value="price_desc" />
            <el-option label="销量优先" value="sales_desc" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- ===== 主体内容 ===== -->
    <div class="mall-main">
      <!-- Banner 轮播 -->
      <el-carousel height="320px" class="banner-carousel" indicator-position="outside" :interval="4000">
        <el-carousel-item v-for="banner in banners" :key="banner.id || banner.title">
          <div class="banner-content" :style="{ backgroundColor: banner.color || banner.bgColor || '#1f7a6f' }">
            <div class="banner-text">
              <h2>{{ banner.title }}</h2>
              <p>{{ banner.subtitle }}</p>
            </div>
            <div class="banner-deco">
              <el-icon size="120" color="rgba(255,255,255,0.12)"><ShoppingBag /></el-icon>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>

      <!-- 公告 -->
      <el-alert
        v-for="n in notices" :key="n.id"
        :title="n.title" :description="n.content"
        type="info" show-icon :closable="false"
        class="notice-bar"
      />

      <!-- 特色楼层：热门推荐 / 新品上架 / 促销专区 -->
      <div class="featured-grid">
        <div class="feature-card">
          <div class="feature-header hot">
            <el-icon style="font-size:20px;"><Star /></el-icon>
            <span>热门推荐</span>
          </div>
          <div class="feature-list">
            <div v-for="item in hotProducts.slice(0, 5)" :key="`hot-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name line-clamp-1">{{ item.name }}</span>
              <span class="f-price">¥{{ item.price }}</span>
            </div>
            <el-empty v-if="hotProducts.length === 0" description="暂无推荐" :image-size="40" />
          </div>
        </div>
        <div class="feature-card">
          <div class="feature-header new">
            <el-icon style="font-size:20px;"><Coin /></el-icon>
            <span>新品上架</span>
          </div>
          <div class="feature-list">
            <div v-for="item in newProducts.slice(0, 5)" :key="`new-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name line-clamp-1">{{ item.name }}</span>
              <span class="f-price">¥{{ item.price }}</span>
            </div>
            <el-empty v-if="newProducts.length === 0" description="暂无新品" :image-size="40" />
          </div>
        </div>
        <div class="feature-card">
          <div class="feature-header promo">
            <el-icon style="font-size:20px;"><Lightning /></el-icon>
            <span>促销专区</span>
          </div>
          <div class="feature-list">
            <div v-for="item in promoProducts.slice(0, 5)" :key="`promo-${item.id}`" class="feature-item" @click="router.push(`/product/${item.id}`)">
              <span class="f-name line-clamp-1">{{ item.name }}</span>
              <span class="f-price promo-price">¥{{ item.price }}</span>
            </div>
            <el-empty v-if="promoProducts.length === 0" description="暂无促销" :image-size="40" />
          </div>
        </div>
      </div>

      <!-- 商品网格标题 -->
      <div class="section-title">
        <div class="section-title-left">
          <el-icon size="22" color="#1f7a6f"><Goods /></el-icon>
          <span>全部商品</span>
          <span class="result-count" v-if="totalItems">共 {{ totalItems }} 件</span>
        </div>
        <div class="section-title-right">
          当前搜索：<el-tag v-if="searchQuery" closable size="small" @close="searchQuery='';handleSearch()">{{ searchQuery }}</el-tag>
          <el-tag v-else size="small" type="info">全部</el-tag>
        </div>
      </div>

      <!-- 加载骨架 -->
      <div v-if="productLoading && productList.length === 0" class="product-grid skeleton-grid">
        <div v-for="i in 8" :key="i" class="product-skeleton">
          <div class="skeleton-img" />
          <div class="skeleton-body">
            <div class="skeleton-line w-80" />
            <div class="skeleton-line w-60" />
            <div class="skeleton-line w-40" />
          </div>
        </div>
      </div>

      <!-- 商品网格 -->
      <el-row :gutter="20" v-else-if="pagedProductList.length" class="product-grid">
        <el-col :xs="12" :sm="12" :md="8" :lg="6" v-for="(item, index) in pagedProductList" :key="item.id" style="margin-bottom:20px;">
          <el-card shadow="hover" :body-style="{ padding: '0px' }" class="product-card" :style="{ '--i': index }" @click="router.push(`/product/${item.id}`)">
            <div class="product-img-wrap">
              <div class="image-placeholder" :style="{ background: item.bgColor }">
                <el-icon v-if="!pickFirstImage(item.image, item.imageUrl)" size="40" color="#fff"><Picture /></el-icon>
                <img v-else :src="getImageUrl(pickFirstImage(item.image, item.imageUrl))" class="product-img" alt="" />
              </div>
              <div class="img-actions">
                <el-button circle size="small" type="warning" @click.stop="toggleFavorite(item)">
                  <el-icon><Star /></el-icon>
                </el-button>
                <el-button circle size="small" type="primary" @click.stop="addToCart(item)">
                  <el-icon><ShoppingCart /></el-icon>
                </el-button>
              </div>
              <!-- 角标 -->
              <span v-if="item.isSeckill || item.seckillPrice" class="badge badge-red">秒杀</span>
              <span v-else-if="item.originalPrice && item.originalPrice > item.price" class="badge badge-orange">优惠</span>
              <span v-if="Number(item.salesCount) > 0" class="sales-badge">
                已售 {{ Number(item.salesCount) >= 1000 ? (item.salesCount / 1000).toFixed(1) + 'k' : item.salesCount }}
              </span>
            </div>
            <div class="product-info">
              <h3 class="title line-clamp-2">{{ item.name }}</h3>
              <div class="price-row">
                <span class="price">
                  <span class="price-symbol">¥</span>{{ item.price }}
                </span>
                <span class="original-price" v-if="item.originalPrice > 0">¥{{ item.originalPrice }}</span>
                <span class="sales-count" v-if="item.salesCount">已售{{ item.salesCount }}</span>
              </div>
              <div class="seller-row">
                <el-avatar :size="18" :src="item.sellerAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                <span class="seller-name line-clamp-1">{{ item.sellerName || '平台商家' }}</span>
                <span class="location-tag"><el-icon size="12"><Location /></el-icon>{{ item.sellerAddress || '平台配送' }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 空状态 -->
      <el-empty v-else description="没有找到相关商品，换个关键词试试~" :image-size="120" class="empty-state" />

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="totalItems > pageSize">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="totalItems"
          :pager-count="7"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- ===== 页脚 ===== -->
    <footer class="mall-footer">
      <div class="footer-inner">
        <div class="footer-links">
          <a href="javascript:;">关于杰物</a>
          <a href="javascript:;">联系客服</a>
          <a href="javascript:;">隐私政策</a>
          <a href="javascript:;">用户协议</a>
        </div>
        <p class="footer-copy">© 2025 杰物 Jemall 校园电商平台 · 发现好物，连接校园</p>
      </div>
    </footer>

    <!-- ===== 购物车抽屉 ===== -->
    <el-drawer v-model="cartVisible" title="购物车" direction="rtl" size="400px">
      <div v-if="cartList.length === 0" class="empty-cart">
        <el-icon size="48" color="#d0d5dd"><ShoppingCart /></el-icon>
        <p>购物车是空的</p>
      </div>
      <div v-else class="cart-list">
        <div v-for="item in cartList" :key="item.id || item.productId" class="cart-item">
          <img v-if="item.productImage" :src="getImageUrl(item.productImage)" class="cart-item-img" alt="" />
          <div class="cart-item-info">
            <h4 class="cart-item-title line-clamp-1">{{ item.productName || '商品' }}</h4>
            <div class="cart-item-price">¥{{ item.price || item.productPrice }}</div>
          </div>
          <el-input-number v-model="item.quantity" :min="1" :max="99" size="small" @change="(v: any) => updateCartQuantity(item, v)" />
          <el-button circle size="small" type="danger" text @click="removeFromCart(item)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div class="cart-footer">
          <el-button type="primary" size="large" style="width:100%;" @click="handleCheckout">去结算</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- ===== 收藏抽屉 ===== -->
    <el-drawer v-model="favVisible" title="我的收藏" direction="rtl" size="400px">
      <div class="fav-list">
        <div v-for="item in favoriteProducts" :key="item.id" class="fav-item" @click="router.push(`/product/${item.id}`)">
          <img v-if="pickFirstImage(item.image, item.imageUrl)" :src="getImageUrl(pickFirstImage(item.image, item.imageUrl))" class="fav-img" alt="" />
          <div class="fav-info">
            <div class="fav-name line-clamp-1">{{ item.name }}</div>
            <div class="fav-price">¥{{ item.price }}</div>
            <div class="fav-actions" @click.stop>
              <el-button size="small" type="danger" @click="toggleFavorite(item)">取消收藏</el-button>
              <el-button size="small" type="primary" @click="addToCart(item)">加入购物车</el-button>
            </div>
          </div>
        </div>
        <el-empty v-if="favoriteProducts.length === 0" description="暂无收藏" :image-size="60" />
      </div>
    </el-drawer>

    <!-- ===== 商品评价对话框 ===== -->
    <el-dialog v-model="reviewVisible" title="评价商品" width="500px" destroy-on-close>
      <div v-if="currentReviewProduct">
        <p style="font-weight:600;margin:0 0 10px;">{{ currentReviewProduct.name }}</p>
        <el-rate v-model="reviewForm.rating" :max="5" show-score style="margin-bottom:12px;" />
        <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="说说你对这件商品的感受..." maxlength="500" show-word-limit />
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleReviewUploadSuccess"
          accept="image/*"
          style="margin-top:12px;"
        >
          <el-button text>上传图片</el-button>
        </el-upload>
      </div>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>

    <!-- ===== 结算确认对话框 ===== -->
    <el-dialog v-model="checkoutConfirmVisible" title="确认订单" width="520px" destroy-on-close>
      <div v-if="checkoutItems.length">
        <div v-for="item in checkoutItems" :key="item.id" style="display:flex;gap:10px;align-items:center;padding:8px 0;border-bottom:1px solid #f0f0f0;">
          <img v-if="item.productImage" :src="getImageUrl(item.productImage)" style="width:50px;height:50px;object-fit:cover;border-radius:8px;" />
          <div style="flex:1;">
            <p style="margin:0;font-weight:600;">{{ item.productName }}</p>
            <p style="margin:2px 0 0;font-size:12px;color:#999;">x{{ item.quantity }}</p>
          </div>
          <span style="color:#e76f51;font-weight:700;">¥{{ (item.price || item.productPrice) * item.quantity }}</span>
        </div>
        <div style="margin:16px 0;font-size:14px;">
          <span>收货地址：</span>
          <el-select v-model="selectedAddressId" placeholder="选择收货地址" style="width:100%;margin-top:6px;">
            <el-option v-for="a in addressList" :key="a.id" :label="formatAddressOption(a)" :value="a.id" />
          </el-select>
        </div>
        <div v-if="myCoupons.length" style="margin:12px 0;">
          <span style="font-size:14px;">使用优惠券：</span>
          <el-select v-model="selectedCouponId" placeholder="选择优惠券" clearable style="width:100%;margin-top:6px;">
            <el-option v-for="c in myCoupons" :key="c.id" :label="c.couponName || c.name" :value="c.id" />
          </el-select>
        </div>
        <div style="text-align:right;font-size:18px;font-weight:700;margin-top:12px;">
          合计：<span style="color:#e76f51;">¥{{ checkoutTotal }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="checkoutConfirmVisible = false">取消</el-button>
        <el-button type="primary" :loading="submittingOrder" @click="submitOrder">提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { resolveImageUrl } from '../utils/env'

const router = useRouter()
const getImageUrl = (url: string) => resolveImageUrl(url)

// ============ 用户信息 ============
const userInfo = ref<any>({})

// ============ 搜索 ============
const searchQuery = ref('')
const searchCategory = ref('')
const hotKeywords = ['数码好物', '学习用品', '二手图书', '生活电器']
const sortBy = ref('')
const activeCategory = ref('')

const switchCategory = (name: string) => {
  activeCategory.value = activeCategory.value === name ? '' : name
  handleSearch()
}

// ============ Banner & 公告 ============
const banners = ref<any[]>([])
const notices = ref<any[]>([])
const categories = ref<any[]>([])

// ============ 商品列表 ============
const productList = ref<any[]>([])
const productLoading = ref(false)
const totalItems = ref(0)
const currentPage = ref(1)
const pageSize = 12

const pagedProductList = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return productList.value.slice(start, start + pageSize)
})

const handlePageChange = (page: number) => {
  currentPage.value = page
  window.scrollTo({ top: 460, behavior: 'smooth' })
}

// ============ 特色商品 ============
const hotProducts = computed(() => productList.value.filter((x) => x.isHot).slice(0, 5))
const newProducts = computed(() => productList.value.filter((x) => x.isNew).slice(0, 5))
const promoProducts = computed(() => productList.value.filter((x) => x.originalPrice > 0 || x.seckillPrice).slice(0, 5))

// ============ 图片工具 ============
const pickFirstImage = (image: any, imageUrl: any): string => {
  const parse = (v: any): string[] => {
    if (!v) return []
    if (Array.isArray(v)) return v.filter(Boolean)
    const s = String(v)
    if (s.startsWith('[')) {
      try { return (JSON.parse(s) || []).filter(Boolean) } catch { return [] }
    }
    return [s]
  }
  const all = [...parse(image), ...parse(imageUrl)].filter(Boolean)
  return all[0] || ''
}

// ============ 搜索 & 数据加载 ============
const handleSearch = async () => {
  currentPage.value = 1
  await fetchProducts()
}

const quickSearch = (k: string) => {
  searchQuery.value = k
  handleSearch()
}

const fetchProducts = async () => {
  productLoading.value = true
  try {
    const res = await request.get('/product/list', {
      params: {
        keyword: searchQuery.value.trim(),
        category: activeCategory.value,
        sortBy: sortBy.value,
        searchMode: 'keyword'
      }
    })
    const colors = ['#ffb8b8', '#b8e9ff', '#b8ffc9', '#ffdfb8', '#e1b8ff', '#b8fff4']
    productList.value = (res || []).map((x: any, i: number) => ({
      ...x,
      bgColor: colors[i % colors.length],
      salesCount: x.salesCount || Math.floor(Math.random() * 500) // 演示数据
    }))
    totalItems.value = productList.value.length
  } catch (e) {
    console.error(e)
  } finally {
    productLoading.value = false
  }
}

const fetchBanners = async () => {
  try {
    const res = await request.get('/user/banner/list')
    banners.value = Array.isArray(res) ? res : []
  } catch {
    banners.value = [
      { id: 1, title: '杰物开学季', subtitle: '全场好物低至 5 折，限时抢购', color: '#1f7a6f' },
      { id: 2, title: '数码风暴', subtitle: '精选数码产品，学生专享价', color: '#2a9d8f' },
      { id: 3, title: '闲置焕新', subtitle: '二手好物交易，省钱又环保', color: '#e76f51' }
    ]
  }
}

const fetchNotices = async () => {
  try {
    const res = await request.get('/user/notice/list')
    notices.value = Array.isArray(res) ? res : []
  } catch {
    notices.value = []
  }
}

const fetchCategories = async () => {
  try {
    const res = await request.get('/product/category/list')
    categories.value = Array.isArray(res) ? res : []
  } catch {
    categories.value = []
  }
}

// ============ 收藏 ============
const favoriteSet = ref<Set<number>>(new Set())
const favoriteProducts = ref<any[]>([])
const favVisible = ref(false)

const fetchFavorites = async () => {
  try {
    const res = await request.get('/product/favorite/list')
    const favs = Array.isArray(res) ? res : []
    favoriteSet.value = new Set(favs.map((x: any) => Number(x.productId || x.id)))
  } catch { /* 无需提示 */ }
}

const toggleFavorite = async (item: any) => {
  try {
    await request.post('/product/favorite/toggle', { productId: item.id })
    if (favoriteSet.value.has(Number(item.id))) {
      favoriteSet.value.delete(Number(item.id))
      ElMessage.success('已取消收藏')
    } else {
      favoriteSet.value.add(Number(item.id))
      ElMessage.success('已收藏')
    }
    favoriteSet.value = new Set(favoriteSet.value)
  } catch {
    ElMessage.error('操作失败')
  }
}

const openFavoriteDrawer = () => {
  favoriteProducts.value = productList.value.filter((x) => favoriteSet.value.has(Number(x.id)))
  favVisible.value = true
}

// ============ 购物车 ============
const cartVisible = ref(false)
const cartList = ref<any[]>([])

const fetchCart = async () => {
  try {
    const res = await request.get('/cart/list')
    cartList.value = Array.isArray(res) ? res : []
  } catch { /* 无需提示 */ }
}

const updateCartQuantity = async (item: any, v: any) => {
  try {
    await request.post('/cart/update', { productId: item.productId || item.id, quantity: v })
  } catch { /* 无需提示 */ }
}

const addToCart = async (product: any) => {
  try {
    await request.post('/cart/add', { productId: product.id, quantity: 1 })
    ElMessage.success('已加入购物车')
    fetchCart()
  } catch {
    ElMessage.error('添加失败')
  }
}

const removeFromCart = async (item: any) => {
  try {
    await request.post('/cart/remove', { productId: item.productId || item.id })
    ElMessage.success('已移除')
    fetchCart()
  } catch {
    ElMessage.error('移除失败')
  }
}

// ============ 结算 ============
const checkoutConfirmVisible = ref(false)
const checkoutItems = ref<any[]>([])
const selectedAddressId = ref<number | null>(null)
const selectedCouponId = ref<number | null>(null)
const addressList = ref<any[]>([])
const myCoupons = ref<any[]>([])
const submittingOrder = ref(false)
const checkoutTotal = computed(() =>
  checkoutItems.value.reduce((sum, item) => sum + (item.price || item.productPrice) * item.quantity, 0)
)

const fetchAddressList = async () => {
  try {
    const res = await request.get('/user/address/list')
    addressList.value = Array.isArray(res) ? res : []
    if (addressList.value.length) selectedAddressId.value = addressList.value[0].id
  } catch { /* 无需提示 */ }
}

const fetchMyCoupons = async () => {
  try {
    const res = await request.get('/user/coupon/list')
    myCoupons.value = Array.isArray(res) ? res : []
  } catch { /* 无需提示 */ }
}

const formatAddressOption = (a: any) => `${a.receiver || a.contactName || ''} ${a.phone || ''} ${a.province || ''}${a.city || ''}${a.district || ''}${a.detailAddress || ''}`

const handleCheckout = async () => {
  checkoutItems.value = cartList.value.filter((x: any) => x.checked !== false)
  if (!checkoutItems.value.length) return ElMessage.warning('购物车为空')
  await fetchAddressList()
  await fetchMyCoupons()
  checkoutConfirmVisible.value = true
}

const submitOrder = async () => {
  if (!selectedAddressId.value) return ElMessage.warning('请选择收货地址')
  submittingOrder.value = true
  try {
    await request.post('/order/create', {
      addressId: selectedAddressId.value,
      couponId: selectedCouponId.value || undefined,
      items: checkoutItems.value.map((x) => ({ productId: x.productId || x.id, quantity: x.quantity }))
    })
    ElMessage.success('下单成功！')
    checkoutConfirmVisible.value = false
    cartVisible.value = false
    fetchCart()
  } catch {
    ElMessage.error('下单失败')
  } finally {
    submittingOrder.value = false
  }
}

// ============ 评价 ============
const reviewVisible = ref(false)
const currentReviewProduct = ref<any>(null)
const reviewForm = ref({ rating: 5, content: '', images: [] as string[] })

const handleReviewUploadSuccess = (resp: any) => {}

const submitReview = async () => {
  if (!currentReviewProduct.value) return
  try {
    await request.post('/product/review/add', {
      productId: currentReviewProduct.value.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content
    })
    ElMessage.success('评价成功')
    reviewVisible.value = false
  } catch {
    ElMessage.error('评价提交失败')
  }
}

// ============ 消息/通知计数 ============
const threadCount = ref(0)
const orderNoticeTotal = ref(0)

const loadNoticeCounts = async () => {
  try {
    const [msgRes, orderRes] = await Promise.all([
      request.get('/order/chat/unread-count').catch(() => 0),
      request.get('/order/notice/unread-count').catch(() => 0)
    ])
    threadCount.value = Number(msgRes) || 0
    orderNoticeTotal.value = Number(orderRes) || 0
  } catch { /* 无需提示 */ }
}

// ============ 登出 ============
const handleLogout = () => {
  ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.clear()
    router.push('/login')
  }).catch(() => {})
}

// ============ 生命周期 ============
onMounted(async () => {
  const stored = localStorage.getItem('userInfo')
  const token = localStorage.getItem('token')
  if (stored) {
    try { userInfo.value = JSON.parse(stored) } catch { userInfo.value = {} }
  }

  const bootstrapTasks = [
    fetchProducts(),
    fetchBanners(),
    fetchNotices(),
    fetchCategories()
  ]

  if (token) {
    bootstrapTasks.push(
      fetchFavorites(),
      fetchCart(),
      loadNoticeCounts()
    )
  }

  await Promise.all(bootstrapTasks)
})
</script>

<style scoped>
/* ===== 容器 ===== */
.mall-container {
  min-height: 100vh;
  background:
    radial-gradient(900px 420px at 10% -10%, rgba(31,122,111,.18), transparent 60%),
    radial-gradient(700px 320px at 90% 5%, rgba(244,162,97,.2), transparent 60%),
    linear-gradient(180deg, #f8f4ee 0%, #eef4f8 55%, #edf3f7 100%);
}

/* ===== 一级导航 ===== */
.mall-header {
  background: rgba(255,255,255,.92);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(31,122,111,.08);
  box-shadow: 0 4px 20px rgba(0,0,0,.04);
  position: sticky;
  top: 0;
  z-index: 100;
  padding: 0;
}
.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 64px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  gap: 20px;
}
.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  flex-shrink: 0;
}
.logo-text {
  font-size: 22px;
  font-weight: 800;
  font-family: 'Space Grotesk', 'Noto Sans SC', sans-serif;
  background: linear-gradient(135deg, #1f7a6f, #2a9d8f);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.search-box {
  flex: 1;
  max-width: 580px;
}
.search-row {
  display: flex;
  align-items: center;
  gap: 0;
  border-radius: 24px;
  overflow: hidden;
  border: 2px solid #1f7a6f;
  background: #fff;
  transition: box-shadow .2s;
}
.search-row:focus-within {
  box-shadow: 0 0 0 4px rgba(31,122,111,.12);
}
.search-cat :deep(.el-input__wrapper) {
  border-radius: 22px 0 0 22px !important;
  box-shadow: none !important;
  background: #f5f7fa;
  border-right: 1px solid #e5e7eb;
}
.search-input {
  flex: 1;
}
.search-input :deep(.el-input__wrapper) {
  border-radius: 0 !important;
  box-shadow: none !important;
  background: transparent;
}
.search-btn {
  border-radius: 0 22px 22px 0 !important;
  min-width: 80px;
  height: 38px;
  font-size: 14px;
}
.hot-keywords {
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.hot-label {
  font-size: 12px;
  color: #64748b;
}
.hot-tag {
  cursor: pointer;
  border: none;
  background: transparent;
  color: #64748b;
}
.hot-tag:hover {
  color: var(--brand);
}

/* ===== 用户操作区 ===== */
.user-actions {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.action-btn {
  border: none;
  padding: 6px 8px;
  border-radius: 8px;
  transition: background .2s;
}
.action-btn:hover {
  background: rgba(31,122,111,.08);
}
.fav-btn {
  border: none;
  padding: 6px 10px;
  border-radius: 8px;
  gap: 4px;
  color: var(--muted);
  font-size: 13px;
}
.fav-btn:hover {
  color: var(--brand);
  background: rgba(31,122,111,.08);
}
.user-profile {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background .2s;
}
.user-profile:hover {
  background: rgba(31,122,111,.06);
}
.nickname {
  font-size: 13px;
  color: var(--muted);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 二级导航 ===== */
.sub-nav {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0,0,0,.04);
  position: sticky;
  top: 64px;
  z-index: 99;
}
.sub-nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 44px;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 0 20px;
  overflow-x: auto;
}
.sub-nav-item {
  padding: 6px 14px;
  font-size: 13px;
  color: #555;
  cursor: pointer;
  border-radius: 6px;
  white-space: nowrap;
  transition: all .2s;
  user-select: none;
}
.sub-nav-item:hover {
  color: var(--brand);
  background: rgba(31,122,111,.06);
}
.sub-nav-item.active {
  color: #fff;
  background: var(--brand);
  font-weight: 600;
}
.sub-nav-item.all-cat {
  font-weight: 600;
  color: var(--brand);
  display: flex;
  align-items: center;
  gap: 4px;
  margin-right: 6px;
}
.sub-nav-spacer {
  flex: 1;
}
.sort-item {
  padding: 0;
  cursor: default;
}
.sort-item:hover {
  background: none;
}

/* ===== 主体内容 ===== */
.mall-main {
  max-width: 1200px;
  margin: 16px auto;
  padding: 0 20px 40px;
}

/* ===== Banner ===== */
.banner-carousel {
  border-radius: var(--radius-lg);
  overflow: hidden;
  margin-bottom: 16px;
  box-shadow: var(--shadow-1);
}
.banner-content {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0 60px;
  position: relative;
}
.banner-text {
  text-align: center;
  z-index: 1;
}
.banner-text h2 {
  font-size: 32px;
  color: #fff;
  margin: 0 0 10px;
  text-shadow: 0 2px 12px rgba(0,0,0,.15);
  font-weight: 800;
}
.banner-text p {
  font-size: 16px;
  color: rgba(255,255,255,.85);
  margin: 0;
}
.banner-deco {
  position: absolute;
  right: 40px;
  top: 50%;
  transform: translateY(-50%);
}

/* ===== 公告 ===== */
.notice-bar {
  margin-bottom: 10px;
  border-radius: var(--radius-sm);
}

/* ===== 特色楼层 ===== */
.featured-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 24px;
}
.feature-card {
  background: #fff;
  border-radius: var(--radius-md);
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
  border: 1px solid rgba(31,122,111,.06);
}
.feature-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 2px solid #f0f0f0;
}
.feature-header.hot { color: #e76f51; }
.feature-header.new { color: #1f7a6f; }
.feature-header.promo { color: #f4a261; }
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.feature-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 10px;
  background: #f7faf9;
  border: 1px solid #e5eeea;
  border-radius: 8px;
  cursor: pointer;
  transition: all .2s;
}
.feature-item:hover {
  background: #eef6f3;
  transform: translateX(4px);
}
.f-name {
  max-width: 68%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 13px;
}
.f-price {
  color: #e76f51;
  font-weight: 700;
  font-size: 14px;
}

/* ===== 商品区标题 ===== */
.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 2px solid #e5e7eb;
}
.section-title-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--ink);
}
.result-count {
  font-size: 13px;
  font-weight: 400;
  color: #999;
}
.section-title-right {
  font-size: 13px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== 骨架屏 ===== */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}
.product-skeleton {
  background: #fff;
  border-radius: var(--radius-md);
  overflow: hidden;
}
.skeleton-img {
  width: 100%;
  height: 200px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton-body {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-40 { width: 40%; }

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* ===== 商品卡片 ===== */
.product-grid {
  display: flex;
  flex-wrap: wrap;
  margin: 0 -10px;
}
.product-card {
  border-radius: var(--radius-md);
  border: 1px solid rgba(31,122,111,.08);
  transition: transform .3s cubic-bezier(.22,1,.36,1), box-shadow .3s ease;
  cursor: pointer;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: rgba(255,255,255,.95);
  box-shadow: 0 2px 12px rgba(0,0,0,.04);
  overflow: hidden;
  animation: cardIn .5s both;
  animation-delay: calc(var(--i) * 0.05s);
}
.product-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(31,122,111,.18);
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 图片区域 */
.product-img-wrap {
  position: relative;
  overflow: hidden;
}
.image-placeholder {
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(31,122,111,.25), rgba(244,162,97,.35));
  transition: transform .3s;
}
.product-card:hover .image-placeholder {
  transform: scale(1.05);
}
.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform .3s;
}
.product-card:hover .product-img {
  transform: scale(1.08);
}
.img-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(8px);
  transition: all .25s;
}
.product-card:hover .img-actions {
  opacity: 1;
  transform: translateY(0);
}

/* 角标 */
.badge {
  position: absolute;
  top: 8px;
  left: 8px;
  padding: 2px 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  border-radius: 4px;
  line-height: 1.6;
}
.badge-red { background: #e74c3c; }
.badge-orange { background: #f4a261; }
.sales-badge {
  position: absolute;
  bottom: 8px;
  left: 8px;
  padding: 2px 8px;
  font-size: 11px;
  color: #fff;
  background: rgba(0,0,0,.55);
  border-radius: 4px;
  line-height: 1.6;
}

/* 商品信息 */
.product-info {
  padding: 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.title {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.5;
  min-height: 2.6em;
}
.line-clamp-1,
.line-clamp-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.line-clamp-1 { -webkit-line-clamp: 1; }
.line-clamp-2 { -webkit-line-clamp: 2; }
.price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.price {
  font-size: 20px;
  color: #e76f51;
  font-weight: 800;
  line-height: 1;
}
.price-symbol {
  font-size: 14px;
}
.original-price {
  font-size: 12px;
  color: #a1a7b3;
  text-decoration: line-through;
}
.sales-count {
  font-size: 11px;
  color: #999;
  margin-left: auto;
}
.seller-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-top: 8px;
  border-top: 1px solid rgba(31,122,111,.08);
  margin-top: auto;
}
.seller-name {
  font-size: 12px;
  color: #556070;
  max-width: 70px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.location-tag {
  font-size: 11px;
  color: #999;
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 2px;
}

/* ===== 空状态 ===== */
.empty-state {
  padding: 60px 0;
}

/* ===== 分页 ===== */
.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

/* ===== 页脚 ===== */
.mall-footer {
  background: #1f2937;
  color: #9ca3af;
  padding: 32px 20px 24px;
}
.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  text-align: center;
}
.footer-links {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
}
.footer-links a {
  color: #d1d5db;
  text-decoration: none;
  font-size: 13px;
  transition: color .2s;
}
.footer-links a:hover {
  color: #fff;
}
.footer-copy {
  font-size: 12px;
  margin: 0;
}

/* ===== 购物车/收藏 ===== */
.empty-cart {
  text-align: center;
  color: #909399;
  margin-top: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.cart-list {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
}
.cart-item-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}
.cart-item-info {
  flex: 1;
  overflow: hidden;
}
.cart-item-title {
  margin: 0 0 6px;
  font-size: 14px;
  font-weight: 600;
}
.cart-item-price {
  color: #e76f51;
  font-weight: 700;
  font-size: 15px;
}
.cart-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 2px solid #f0f0f0;
}
.fav-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.fav-item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: background .2s;
}
.fav-item:hover {
  background: #f7faf9;
}
.fav-img {
  width: 72px;
  height: 72px;
  border-radius: 8px;
  object-fit: cover;
  background: #f6f7f8;
  flex-shrink: 0;
}
.fav-info {
  flex: 1;
  min-width: 0;
}
.fav-name {
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-price {
  margin: 6px 0;
  color: #e76f51;
  font-weight: 700;
}
.fav-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ===== 响应式 ===== */
@media (max-width: 1024px) {
  .featured-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (max-width: 900px) {
  .header-content {
    height: auto;
    min-height: 60px;
    padding: 8px 14px;
    gap: 10px;
    flex-wrap: wrap;
  }
  .search-box {
    order: 3;
    max-width: 100%;
    width: 100%;
  }
  .user-actions {
    margin-left: auto;
    gap: 4px;
  }
  .mall-main {
    padding: 0 14px 30px;
    margin: 12px auto;
  }
  .featured-grid {
    grid-template-columns: 1fr;
  }
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .banner-content {
    padding: 0 30px;
  }
  .banner-text h2 {
    font-size: 24px;
  }
  .banner-deco {
    display: none;
  }
}
@media (max-width: 640px) {
  .sub-nav {
    top: 56px;
  }
  .sub-nav-inner {
    padding: 0 10px;
    gap: 0;
  }
  .sub-nav-item {
    padding: 6px 10px;
    font-size: 12px;
  }
  .mall-header {
    position: sticky;
  }
  .header-content {
    padding: 8px 10px 10px;
  }
  .logo-text {
    font-size: 18px;
  }
  .search-row {
    border-width: 1.5px;
  }
  .search-cat {
    display: none !important;
  }
  .search-btn {
    min-width: 60px;
    font-size: 13px;
  }
  .user-actions {
    gap: 2px;
  }
  .nickname {
    display: none;
  }
  .fav-btn span {
    display: none;
  }
  .mall-main {
    padding: 0 10px 20px;
    margin-top: 10px;
  }
  .banner-carousel :deep(.el-carousel__container) {
    height: 160px !important;
  }
  .banner-text h2 {
    font-size: 20px;
  }
  .banner-text p {
    font-size: 13px;
  }
  .section-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }
  .image-placeholder {
    height: 160px;
  }
  .product-info {
    padding: 10px;
  }
  .title {
    font-size: 13px;
    min-height: 2.2em;
  }
  .price {
    font-size: 17px;
  }
  .seller-row {
    flex-wrap: wrap;
    gap: 4px;
  }
  .location-tag {
    width: 100%;
    margin-left: 0;
  }
  .cart-item {
    flex-wrap: wrap;
  }
  .fav-actions :deep(.el-button) {
    margin-left: 0;
  }
  :deep(.el-pagination) {
    justify-content: center;
    flex-wrap: wrap;
    gap: 4px;
  }
  :deep(.el-pagination .el-pagination__total),
  :deep(.el-pagination .el-pagination__sizes) {
    display: none;
  }
  :deep(.el-dialog) {
    width: calc(100vw - 20px) !important;
    margin-top: 4vh !important;
    border-radius: 16px;
  }
  :deep(.el-dialog__body) {
    max-height: 70vh;
    overflow-y: auto;
    padding: 12px 14px;
  }
  :deep(.el-drawer.rtl) {
    width: min(100vw, 400px) !important;
  }
}
</style>
