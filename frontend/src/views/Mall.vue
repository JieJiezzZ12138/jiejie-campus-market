<template>
  <div class="mall-container">
    <el-header class="mall-header">
      <div class="header-content">
        <div class="logo" @click="router.push('/')">
          <el-icon size="28" color="#409EFF"><ShoppingBag /></el-icon>
          <span class="logo-text">JieJie 校园集市</span>
        </div>
        
        <div class="search-box">
          <el-input v-model="searchQuery" placeholder="搜索你想要的闲置物品..." class="search-input" clearable @keyup.enter="handleSearch" @clear="handleSearch">
            <template #append><el-button :icon="Search" @click="handleSearch" /></template>
          </el-input>
        </div>

        <div class="user-actions">
          <el-button type="primary" plain :icon="Plus" round @click="publishVisible = true">发布闲置</el-button>
          
          <el-badge :value="cartList.length" class="cart-badge" :hidden="cartList.length === 0">
            <el-icon size="24" class="action-icon" @click="cartVisible = true"><ShoppingCart /></el-icon>
          </el-badge>
          
          <el-dropdown trigger="click">
            <div class="user-profile">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <span class="nickname">{{ userInfo.nickname || '同学' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item>我的收藏</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <div class="mall-main">
      <el-carousel height="300px" class="banner-carousel" indicator-position="outside">
        <el-carousel-item v-for="(banner, index) in banners" :key="index">
          <div class="banner-content" :style="{ backgroundColor: banner.color }">
            <h2>{{ banner.title }}</h2>
            <p>{{ banner.subtitle }}</p>
          </div>
        </el-carousel-item>
      </el-carousel>

      <div class="category-tabs">
        <el-tabs v-model="activeCategory">
          <el-tab-pane label="🚀 最新发布" name="new"></el-tab-pane>
          <el-tab-pane label="💻 数码电子" name="digital"></el-tab-pane>
        </el-tabs>
      </div>

      <el-row :gutter="20" class="product-grid">
        <el-col :span="6" v-for="item in productList" :key="item.id" style="margin-bottom: 20px;">
          <el-card shadow="hover" :body-style="{ padding: '0px' }" class="product-card">
            <div class="image-placeholder" :style="{ background: item.bgColor }">
              <el-icon v-if="!item.image" size="40" color="#fff"><Picture /></el-icon>
              <img v-else :src="item.image" class="product-img" />
            </div>
            <div class="product-info">
              <h3 class="title line-clamp-2">{{ item.name }}</h3>
              <div class="price-row">
                <span class="price">¥ {{ item.price }}</span>
                <span class="original-price">原价 ¥{{ item.originalPrice }}</span>
              </div>
              
              <div class="seller-info">
                <div class="seller-tag">
                  <el-avatar :size="20" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
                  <span class="seller-name">{{ item.sellerName || '匿名卖家' }}</span>
                </div>
                <el-button type="primary" size="small" circle :icon="ShoppingCart" @click.stop="addToCart(item)" />
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="publishVisible" title="发布我的闲置" width="500px">
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="商品名称" required><el-input v-model="publishForm.name" /></el-form-item>
        <el-form-item label="转手价格" required><el-input-number v-model="publishForm.price" :min="0.1" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="购入原价"><el-input-number v-model="publishForm.originalPrice" :min="0.1" :precision="2" :step="10" /></el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="publishVisible = false">取消</el-button>
          <el-button type="primary" :loading="publishLoading" @click="submitPublish">确认发布</el-button>
        </span>
      </template>
    </el-dialog>

    <el-drawer v-model="cartVisible" title="我的购物车" size="400px">
      <div v-if="cartList.length === 0" class="empty-cart">
        <el-icon size="60" color="#c0c4cc"><ShoppingCart /></el-icon>
        <p>购物车空空如也，快去挑点宝贝吧~</p>
      </div>
      <div v-else class="cart-list">
        <div v-for="item in cartList" :key="item.id" class="cart-item">
          <div class="cart-item-info">
            <h4 class="cart-item-title">{{ item.name }}</h4>
            <span class="cart-item-price">¥ {{ item.price }}</span>
          </div>
          <el-button type="danger" size="small" plain @click="removeFromCart(item.productId)">移除</el-button>
        </div>
        <div class="cart-footer">
          <div class="total-price">合计: <span>¥ {{ cartTotalPrice }}</span></div>
          <el-button type="success" size="large" style="width: 100%;" @click="handleCheckout" :loading="checkoutLoading">马上结算</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ShoppingBag, Search, ShoppingCart, Plus, Picture } from '@element-plus/icons-vue'
import request from '../utils/request' 

const router = useRouter()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

// --- 商品列表逻辑 ---
const searchQuery = ref('')
const activeCategory = ref('new')
const productList = ref([])
const banners = [
  { title: '毕业季大甩卖', subtitle: '全场闲置 1 折起，学长学姐吐血推荐', color: '#a0cfff' },
  { title: '校园数码节', subtitle: '二手电脑/平板/相机 淘好物', color: '#f3d19e' }
]

const handleSearch = () => fetchProducts()

const fetchProducts = async () => {
  try {
    // 👉 升级：无需传 username，后端从 Token 获取身份
    const res = await request.get('/product/list', { params: { keyword: searchQuery.value } })
    const colors = ['#ffb8b8', '#b8e9ff', '#b8ffc9', '#ffdfb8', '#e1b8ff', '#b8fff4']
    productList.value = res.map((item, index) => ({
      ...item,
      bgColor: colors[index % colors.length]
    }))
  } catch (error) {
    console.error("获取商品失败:", error)
  }
}

// --- 发布闲置 ---
const publishVisible = ref(false)
const publishLoading = ref(false)
const publishForm = ref({ name: '', price: 0, originalPrice: 0 })

const submitPublish = async () => {
  if (!publishForm.value.name || publishForm.value.price <= 0) return ElMessage.warning('请填写完整')
  publishLoading.value = true
  try {
    await request.post('/product/publish', publishForm.value)
    ElMessage.success('发布成功，请等待审核！')
    publishVisible.value = false 
    publishForm.value = { name: '', price: 0, originalPrice: 0 }
    fetchProducts() 
  } finally {
    publishLoading.value = false
  }
}

// --- 🛒 购物车逻辑 (核心升级：不再手动拼用户名) ---
const cartVisible = ref(false)
const cartList = ref([])

const cartTotalPrice = computed(() => {
  return cartList.value.reduce((total, item) => total + item.price, 0).toFixed(2)
})

const fetchCart = async () => {
  try {
    // 👉 升级：直接请求，拦截器会自动带上 Token
    const res = await request.get('/cart/list')
    cartList.value = res || []
  } catch (error) {
    console.error("获取购物车失败", error)
  }
}

const addToCart = async (product) => {
  try {
    // 👉 升级：只传 productId，后端从请求头认出你是谁
    await request.post(`/cart/add?productId=${product.id}`)
    ElMessage.success(`《${product.name}》已加入购物车！`)
    fetchCart() 
  } catch (error) {
    console.error("加入购物车失败", error)
  }
}

const removeFromCart = async (productId) => {
  try {
    await request.post(`/cart/remove?productId=${productId}`)
    ElMessage.success('已从购物车移除')
    fetchCart() 
  } catch (error) {
    console.error("移除失败", error)
  }
}

// --- 💳 结算逻辑 ---
const checkoutLoading = ref(false)

const handleCheckout = async () => {
  if (cartList.value.length === 0) return ElMessage.warning('购物车是空的哦')
  
  checkoutLoading.value = true
  try {
    // 👉 升级：结算接口安全化
    await request.post(`/order/checkout?totalAmount=${cartTotalPrice.value}`)
    
    await ElMessageBox.alert('订单已生成，请前往订单中心完成支付', '🎉 下单成功', { 
      confirmButtonText: '去支付', 
      type: 'success', 
      center: true 
    })
    
    cartVisible.value = false
    fetchCart()      
    fetchProducts() // 商品被下架了，刷新首页
    router.push('/orders') // 自动跳转到订单页面
  } catch (error) {
    console.error("结算失败", error)
  } finally {
    checkoutLoading.value = false
  }
}

// --- 初始化与退出 ---
onMounted(() => {
  fetchProducts()
  fetchCart() 
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出账号吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.clear() // 清空所有 token 和用户信息
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
/* 样式保留你原来的，增加了一个用户信息的微调 */
.mall-container { min-height: 100vh; background-color: #f4f4f4; }
.mall-header { background-color: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.06); position: sticky; top: 0; z-index: 100; padding: 0; }
.header-content { max-width: 1200px; margin: 0 auto; height: 60px; display: flex; justify-content: space-between; align-items: center; padding: 0 20px; }
.logo { display: flex; align-items: center; gap: 10px; cursor: pointer; }
.logo-text { font-size: 22px; font-weight: bold; color: #409EFF; }
.search-box { width: 400px; }
.user-actions { display: flex; align-items: center; gap: 25px; }
.user-profile { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.nickname { font-size: 14px; color: #606266; }
.action-icon { color: #606266; cursor: pointer; }
.action-icon:hover { color: #409EFF; }
.mall-main { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.banner-carousel { border-radius: 12px; overflow: hidden; margin-bottom: 20px; }
.banner-content { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.banner-content h2 { font-size: 36px; margin: 0 0 10px 0; }
.category-tabs { background: #fff; padding: 10px 20px 0; border-radius: 8px; margin-bottom: 20px; }
.product-card { border-radius: 8px; border: none; transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; height: 100%; }
.product-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
.image-placeholder { height: 200px; display: flex; justify-content: center; align-items: center; position: relative; overflow: hidden; }
.product-img { width: 100%; height: 100%; object-fit: cover; }
.product-info { padding: 14px; }
.title { font-size: 14px; color: #333; margin: 0 0 10px 0; height: 40px; line-height: 20px; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; }
.price { font-size: 20px; color: #f56c6c; font-weight: bold; }
.original-price { font-size: 12px; color: #999; text-decoration: line-through; }
.seller-info { border-top: 1px solid #f5f5f5; padding-top: 10px; display: flex; justify-content: space-between; align-items: center; }
.seller-tag { display: flex; align-items: center; gap: 6px; }
.seller-name { font-size: 12px; color: #666; }

.empty-cart { text-align: center; color: #999; margin-top: 80px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.cart-list { display: flex; flex-direction: column; height: 100%; }
.cart-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 0; border-bottom: 1px solid #eee; }
.cart-item-title { margin: 0 0 8px 0; font-size: 14px; color: #333; }
.cart-item-price { color: #f56c6c; font-weight: bold; }
.cart-footer { margin-top: auto; padding-top: 20px; border-top: 2px solid #eee; }
.total-price { font-size: 16px; font-weight: bold; margin-bottom: 15px; text-align: right; }
.total-price span { color: #f56c6c; font-size: 24px; }
</style>