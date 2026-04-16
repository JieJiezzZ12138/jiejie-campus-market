import axios from 'axios'
import type { AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

type ApiResponse<T = unknown> = {
  code: number
  msg?: string
  data: T
}

const request = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API || 'http://localhost:8080',
  timeout: 5000
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')

    if (token) {
      ;(config.headers as any).Authorization = `Bearer ${token}`
    }

    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

;(request.interceptors.response as any).use(
  (response: any) => {
    const res = response.data as ApiResponse

    if (res.code === 401) {
      ElMessage.error(res.msg || '未登录或登录已过期，请重新登录')
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      router.push('/login')
      return Promise.reject(new Error(res.msg || 'Error'))
    }

    if (res.code === 200) {
      return res.data
    }

    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || 'Error'))
  },
  (error: AxiosError) => {
    console.error('响应异常:', error)
    ElMessage.error('网络请求失败，请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request as any
