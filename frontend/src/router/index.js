import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  { 
    path: '/login', 
    component: () => import('../views/Login.vue') 
  },
  { 
    path: '/', 
    component: () => import('../views/Mall.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'MessageInbox',
    component: () => import('../views/MessageInbox.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat/product/:productId',
    name: 'ProductChat',
    component: () => import('../views/ChatRoom.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/chat/thread/:threadId',
    name: 'ThreadChat',
    component: () => import('../views/ChatRoom.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders/:orderId/chat',
    name: 'OrderChat',
    component: () => import('../views/ChatRoom.vue'),
    meta: { requiresAuth: true }
  },
  { 
    path: '/orders',
    name: 'OrderList',
    component: () => import('../views/OrderList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  { 
    path: '/admin', 
    component: () => import('../views/Dashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 👉 核心操作：升级版路由守卫（消灭 next 警告）
router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('user_role')

  // 1. 如果去登录页，直接放行
  if (to.path === '/login') return true

  // 2. 检查是否需要登录
  if (to.meta.requiresAuth && !token) {
    ElMessage.error('请先登录')
    return '/login' // 直接 return 路径，不再用 next('/login')
  }

  // 3. 检查管理员权限
  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    ElMessage.warning('权限不足')
    return '/' // 直接 return，不再用 next('/')
  }

  // 最后默认放行
  return true 
})

export default router