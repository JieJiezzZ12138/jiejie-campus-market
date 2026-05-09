import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes: RouteRecordRaw[] = [
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
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue'),
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

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('user_role')
  const isAdmin = role === 'ADMIN' || role === 'SUPER_ADMIN'

  if (to.path === '/login') return true

  if (to.meta.requiresAuth && !token) {
    ElMessage.error('请先登录')
    return '/login'
  }

  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    if (role === 'SUPER_ADMIN') return true
    ElMessage.warning('权限不足')
    return '/'
  }

  if (isAdmin) {
    const frontBuyerPaths = ['/orders', '/messages']
    const path = to.path || ''
    const allowFromAdmin = to.query?.from === 'admin'
    if (!allowFromAdmin && (frontBuyerPaths.includes(path) || path.startsWith('/chat/') || path.startsWith('/orders/'))) {
      ElMessage.info('商家/管理员请在后台处理订单与消息')
      return '/admin'
    }
  }

  return true
})

export default router
