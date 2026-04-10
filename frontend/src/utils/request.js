import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建 axios 实例
const request = axios.create({
  baseURL: 'http://localhost:8080', // 你的网关地址或后端地址，保持你原来的不变
  timeout: 5000
})

// 👉 1. 请求拦截器：出门前检查口袋
request.interceptors.request.use(
  config => {
    // 从 localStorage 中拿到刚才登录时存进去的 token
    const token = localStorage.getItem('token')
    
    if (token) {
      // 如果有，就塞进请求头里。注意这里属性名必须是 'token'，跟后端保持一致
      config.headers['token'] = token
    }
    
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 👉 2. 响应拦截器：回家后检查状态
request.interceptors.response.use(
  response => {
    // 假设你的后端统一返回格式是 { code: ..., msg: ..., data: ... }
    let res = response.data

    // 如果后端返回 401，说明 Token 失效或没登录
    if (res.code === 401) {
      ElMessage.error(res.msg || '未登录或登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login') // 自动踢回登录页
      return Promise.reject(new Error(res.msg || 'Error'))
    }

    // 如果业务状态码是 200，说明成功，直接返回 data 给调用方 (你在 Login.vue 里 await 拿到的就是这个 res)
    if (res.code === 200) {
      return res.data
    } else {
      // 业务错误（比如密码错误），抛出异常但不跳转
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg || 'Error'))
    }
  },
  error => {
    console.error('响应异常:', error)
    ElMessage.error('网络请求失败，请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request