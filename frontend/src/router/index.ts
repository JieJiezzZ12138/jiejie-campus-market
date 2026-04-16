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

  if (to.path === '/login') return true

  if (to.meta.requiresAuth && !token) {
    ElMessage.error('请先登录')
    return '/login'
  }

  if (to.meta.requiresAdmin && role !== 'ADMIN') {
    ElMessage.warning('权限不足')
    return '/'
  }

  return true
})

export default router
