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
          
          <el-badge :value="threadCount" :hidden="threadCount === 0" :max="99" class="msg-badge">
            <el-icon size="24" class="action-icon" title="我的私信" @click="router.push('/messages')"><ChatDotRound /></el-icon>
          </el-badge>

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
                <el-dropdown-item @click="router.push('/messages')">我的私信</el-dropdown-item>
                <el-dropdown-item @click="router.push('/profile')">个人资料</el-dropdown-item>
                <el-dropdown-item v-if="userInfo.role === 'ADMIN'" @click="router.push('/admin')">进入后台</el-dropdown-item>
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
        <el-tabs v-model="activeCategory" @tab-change="handleSearch">
          <el-tab-pane label="🚀 全部商品" name=""></el-tab-pane>
          <el-tab-pane label="💻 数码电子" name="数码电子"></el-tab-pane>
          <el-tab-pane label="📚 书籍资料" name="书籍资料"></el-tab-pane>
          <el-tab-pane label="👗 衣物鞋帽" name="衣物鞋帽"></el-tab-pane>
          <el-tab-pane label="🧴 生活用品" name="生活用品"></el-tab-pane>
          <el-tab-pane label="📦 其他闲置" name="其他闲置"></el-tab-pane>
        </el-tabs>
      </div>

      <el-row :gutter="20" class="product-grid">
        <el-col :span="6" v-for="item in productList" :key="item.id" style="margin-bottom: 20px;">
          <el-card shadow="hover" :body-style="{ padding: '0px' }" class="product-card">
            <div class="image-placeholder" :style="{ background: item.bgColor }">
              <el-icon v-if="!item.image && !item.imageUrl" size="40" color="#fff"><Picture /></el-icon>
              <img v-else :src="getImageUrl(item.image || item.imageUrl)" class="product-img" />
            </div>
            <div class="product-info">
              <h3 class="title line-clamp-1">{{ item.name }}</h3>
              <p class="desc line-clamp-2">{{ item.description || '这件宝贝还没有详细描述哦~' }}</p>
              
              <div class="price-row">
                <span class="price">¥ {{ item.price }}</span>
                <span class="original-price" v-if="item.originalPrice > 0">原价 ¥{{ item.originalPrice }}</span>
              </div>
              
              <div class="location-tag">
                <el-icon><Location /></el-icon>
                <span>{{ item.sellerAddress || '校内面交' }}</span>
              </div>
              
              <div class="seller-info">
                <div class="seller-tag">
                  <el-avatar :size="20" :src="item.sellerAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                  <span class="seller-name">{{ item.sellerName || '匿名卖家' }}</span>
                </div>
                <div class="seller-actions">
                  <el-button type="success" size="small" plain @click.stop="contactSeller(item)">联系卖家</el-button>
                  <el-button type="primary" size="small" circle :icon="ShoppingCart" @click.stop="addToCart(item)" />
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="publishVisible" title="发布我的闲置" width="550px">
      <el-form :model="publishForm" label-width="100px">
        <el-form-item label="商品图片" required>
          <el-upload
            class="avatar-uploader"
            action="http://localhost:8080/product/upload" 
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
            name="file"
          >
            <img v-if="publishForm.image" :src="getImageUrl(publishForm.image)" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="商品名称" required><el-input v-model="publishForm.name" placeholder="请输入核心关键字" /></el-form-item>
        
        <el-form-item label="商品分类" required>
          <el-select v-model="publishForm.category" placeholder="请选择正确的商品分类" style="width: 100%;">
            <el-option label="数码电子" value="数码电子" />
            <el-option label="书籍资料" value="书籍资料" />
            <el-option label="衣物鞋帽" value="衣物鞋帽" />
            <el-option label="生活用品" value="生活用品" />
            <el-option label="其他闲置" value="其他闲置" />
          </el-select>
        </el-form-item>

        <el-form-item label="详细描述">
          <el-input type="textarea" v-model="publishForm.description" :rows="3" placeholder="介绍一下你的闲置（如：几成新、购买时间、是否包邮等）" />
        </el-form-item>

        <el-form-item label="转手价格" required><el-input-number v-model="publishForm.price" :min="0.1" :precision="2" :step="10" /></el-form-item>
        <el-form-item label="购入原价"><el-input-number v-model="publishForm.originalPrice" :min="0" :precision="2" :step="10" /></el-form-item>
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
          <img v-if="item.image || item.imageUrl" :src="getImageUrl(item.image || item.imageUrl)" class="cart-item-img" />
          <div v-else class="cart-item-img placeholder"><el-icon><Picture /></el-icon></div>
          <div class="cart-item-info">
            <h4 class="cart-item-title">{{ item.productName || item.name || '未知商品' }}</h4>
            <span class="cart-item-price">¥ {{ item.price }}</span>
          </div>
          <el-button type="danger" size="small" plain @click="removeFromCart(item)">移除</el-button>
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
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
// 👉 引入 Location 图标
import { ShoppingBag, Search, ShoppingCart, Plus, Picture, Location, ChatDotRound } from '@element-plus/icons-vue'
import request from '../utils/request' 

const router = useRouter()

const contactSeller = (item) => {
  const me = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (me.id != null && item.sellerId != null && Number(me.id) === Number(item.sellerId)) {
    ElMessage.info('这是您自己发布的商品')
    return
  }
  router.push(`/chat/product/${item.id}`)
}
const route = useRoute()
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))

watch(
  () => route.path,
  (p) => {
    if (p === '/') {
      userInfo.value = JSON.parse(localStorage.getItem('userInfo') || '{}')
      fetchThreadCount()
    }
  }
)

const searchQuery = ref('')
const activeCategory = ref('')
const productList = ref([])
const banners = [
  { title: '毕业季大甩卖', subtitle: '全场闲置 1 折起，学长学姐吐血推荐', color: '#a0cfff' },
  { title: '校园数码节', subtitle: '二手电脑/平板/相机 淘好物', color: '#f3d19e' }
]

const getImageUrl = (url) => {
  if (!url) return '';
  if (url.includes('localhost:8080') || url.includes('localhost:8081')) {
    return url.replace(/8080|8081/g, '8082');
  }
  if (url.startsWith('http')) return url;
  return 'http://localhost:8082' + url;
}

const handleSearch = () => fetchProducts()

const fetchProducts = async () => {
  try {
    const res = await request.get('/product/list', { 
      params: { 
        keyword: searchQuery.value,
        category: activeCategory.value 
      } 
    })
    const colors = ['#ffb8b8', '#b8e9ff', '#b8ffc9', '#ffdfb8', '#e1b8ff', '#b8fff4']
    productList.value = res.map((item, index) => ({
      ...item,
      bgColor: colors[index % colors.length]
    }))
  } catch (error) {
    console.error("获取商品失败:", error)
  }
}

const publishVisible = ref(false)
const publishLoading = ref(false)
const publishForm = ref({ name: '', description: '', category: '其他闲置', price: 0, originalPrice: 0, image: '' })

const localToken = localStorage.getItem('token') || localStorage.getItem('jiejie_assignment_token') || '';
const uploadHeaders = {
  token: localToken,
  Authorization: localToken 
}

const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    publishForm.value.image = res.data
    ElMessage.success('图片上传成功！')
  } else {
    ElMessage.error(res.msg || '图片上传失败')
  }
}

const submitPublish = async () => {
  if (!publishForm.value.name || publishForm.value.price <= 0 || !publishForm.value.image) {
    return ElMessage.warning('请填写完整商品信息并上传图片')
  }
  
  publishLoading.value = true
  try {
    await request.post('/product/publish', publishForm.value)
    ElMessage.success('发布成功，商品已上架！')
    publishVisible.value = false 
    publishForm.value = { name: '', description: '', category: '其他闲置', price: 0, originalPrice: 0, image: '' }
    fetchProducts() 
  } catch (error) {
    console.error("发布失败", error)
  } finally {
    publishLoading.value = false
  }
}

const threadCount = ref(0)

const fetchThreadCount = async () => {
  try {
    const n = await request.get('/order/chat/unread-count')
    threadCount.value = typeof n === 'number' ? n : 0
  } catch {
    threadCount.value = 0
  }
}

const cartVisible = ref(false)
const cartList = ref([])

const cartTotalPrice = computed(() => {
  return cartList.value.reduce((total, item) => total + item.price, 0).toFixed(2)
})

const fetchCart = async () => {
  try {
    const res = await request.get('/cart/list')
    cartList.value = res || []
  } catch (error) {
    console.error("获取购物车失败", error)
  }
}

const addToCart = async (product) => {
  const me = userInfo.value || {}
  if (me.id != null && product?.sellerId != null && Number(me.id) === Number(product.sellerId)) {
    return ElMessage.warning('不能把自己的商品加入购物车')
  }
  try {
    await request.post(`/cart/add?productId=${product.id}`)
    ElMessage.success(`《${product.name}》已加入购物车！`)
    fetchCart() 
  } catch (error) {
    console.error("加入购物车失败", error)
  }
}

const removeFromCart = async (item) => {
  try {
    const targetId = item.productId || item.product_id || item.id;
    if (!targetId) return ElMessage.error('无法获取商品ID，移除失败');
    await request.post(`/cart/remove?productId=${targetId}`)
    ElMessage.success('已从购物车移除')
    fetchCart() 
  } catch (error) {
    console.error("移除失败", error)
  }
}

const checkoutLoading = ref(false)
const handleCheckout = async () => {
  if (cartList.value.length === 0) return ElMessage.warning('购物车是空的哦')
  checkoutLoading.value = true
  try {
    await request.post(`/order/checkout?totalAmount=${cartTotalPrice.value}`)
    await ElMessageBox.alert('订单已生成，请前往订单中心完成支付', '🎉 下单成功', { 
      confirmButtonText: '去支付', type: 'success', center: true 
    })
    cartVisible.value = false
    fetchCart()      
    fetchProducts()
    router.push('/orders')
  } catch (error) {
    console.error("结算失败", error)
  } finally {
    checkoutLoading.value = false
  }
}

onMounted(() => {
  fetchProducts()
  fetchCart()
  fetchThreadCount()
})

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出账号吗？', '提示', { type: 'warning' }).then(() => {
    localStorage.clear() 
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
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
.msg-badge :deep(.el-badge__content) { top: 2px; right: 2px; }
.msg-badge {
  position: relative;
  z-index: 10;
}
.msg-badge :deep(.el-badge__content) {
  z-index: 11;
}
.mall-main { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.banner-carousel { border-radius: 12px; overflow: hidden; margin-bottom: 20px; }
.banner-content { height: 100%; display: flex; flex-direction: column; justify-content: center; align-items: center; color: #fff; text-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.banner-content h2 { font-size: 36px; margin: 0 0 10px 0; }
.category-tabs { background: #fff; padding: 10px 20px 0; border-radius: 8px; margin-bottom: 20px; }
.product-card { border-radius: 8px; border: none; transition: transform 0.2s, box-shadow 0.2s; cursor: pointer; height: 100%; display: flex; flex-direction: column;}
.product-card:hover { transform: translateY(-5px); box-shadow: 0 10px 20px rgba(0,0,0,0.1); }
.image-placeholder { height: 200px; display: flex; justify-content: center; align-items: center; position: relative; overflow: hidden; }
.product-img { width: 100%; height: 100%; object-fit: cover; }
.product-info { padding: 14px; flex: 1; display: flex; flex-direction: column;}
.title { font-size: 15px; font-weight: bold; color: #333; margin: 0 0 5px 0; }
.desc { font-size: 12px; color: #999; margin: 0 0 10px 0; line-height: 1.5; min-height: 36px;}
.line-clamp-1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.line-clamp-2 { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.price-row { display: flex; align-items: baseline; gap: 10px; margin-bottom: 12px; margin-top: auto;}
.price { font-size: 20px; color: #f56c6c; font-weight: bold; }
.original-price { font-size: 12px; color: #999; text-decoration: line-through; }

/* 👉 新增的面交地址样式 */
.location-tag { font-size: 12px; color: #909399; margin-bottom: 8px; display: flex; align-items: center; gap: 4px; }

.seller-info { border-top: 1px solid #f5f5f5; padding-top: 10px; display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.seller-tag { display: flex; align-items: center; gap: 6px; min-width: 0; flex: 1; }
.seller-name { font-size: 12px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.seller-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.empty-cart { text-align: center; color: #999; margin-top: 80px; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.cart-list { display: flex; flex-direction: column; height: 100%; }
.cart-item { display: flex; justify-content: space-between; align-items: center; padding: 15px 0; border-bottom: 1px solid #eee; gap: 15px; }
.cart-item-img { width: 60px; height: 60px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.cart-item-img.placeholder { background: #f0f2f5; display: flex; justify-content: center; align-items: center; color: #909399; font-size: 20px; }
.cart-item-info { flex: 1; display: flex; flex-direction: column; justify-content: center; overflow: hidden; }
.cart-item-title { margin: 0 0 8px 0; font-size: 14px; color: #333; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cart-item-price { color: #f56c6c; font-weight: bold; }
.cart-footer { margin-top: auto; padding-top: 20px; border-top: 2px solid #eee; }
.total-price { font-size: 16px; font-weight: bold; margin-bottom: 15px; text-align: right; }
.total-price span { color: #f56c6c; font-size: 24px; }
:deep(.avatar-uploader .el-upload) { border: 1px dashed var(--el-border-color); border-radius: 6px; cursor: pointer; position: relative; overflow: hidden; transition: var(--el-transition-duration-fast); }
:deep(.avatar-uploader .el-upload:hover) { border-color: var(--el-color-primary); }
:deep(.avatar-uploader-icon) { font-size: 28px; color: #8c939d; width: 178px; height: 178px; text-align: center; }
.avatar { width: 178px; height: 178px; display: block; object-fit: cover; }
</style>
