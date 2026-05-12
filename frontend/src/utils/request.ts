import axios from 'axios'
import type { AxiosError, AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

type ApiResponse<T = unknown> = {
  code: number
  msg?: string
  data: T
}

type ApiClient = Omit<AxiosInstance, 'request' | 'get' | 'delete' | 'head' | 'options' | 'post' | 'put' | 'patch'> & {
  request<T = any>(config: Parameters<AxiosInstance['request']>[0]): Promise<T>
  get<T = any>(url: string, config?: Parameters<AxiosInstance['get']>[1]): Promise<T>
  delete<T = any>(url: string, config?: Parameters<AxiosInstance['delete']>[1]): Promise<T>
  head<T = any>(url: string, config?: Parameters<AxiosInstance['head']>[1]): Promise<T>
  options<T = any>(url: string, config?: Parameters<AxiosInstance['options']>[1]): Promise<T>
  post<T = any>(url: string, data?: unknown, config?: Parameters<AxiosInstance['post']>[2]): Promise<T>
  put<T = any>(url: string, data?: unknown, config?: Parameters<AxiosInstance['put']>[2]): Promise<T>
  patch<T = any>(url: string, data?: unknown, config?: Parameters<AxiosInstance['patch']>[2]): Promise<T>
}

const rawRequest = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API ?? '',
  timeout: 5000
})

const request = rawRequest as ApiClient

let authRedirecting = false

rawRequest.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')

    if (token) {
      config.headers.set('Authorization', `Bearer ${token}`)
    }

    return config
  },
  (error: AxiosError) => Promise.reject(error)
)

type ApiResponseInterceptor = (
  onFulfilled: (response: AxiosResponse<ApiResponse>) => unknown,
  onRejected?: (error: AxiosError) => unknown
) => number

const useApiResponseInterceptor = rawRequest.interceptors.response.use as unknown as ApiResponseInterceptor

useApiResponseInterceptor(
  (response) => {
    const res = response.data as ApiResponse

    if (res.code === 401) {
      handleUnauthorized(res.msg)
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
    if (error.response?.status === 401) {
      handleUnauthorized('未登录或登录已过期，请重新登录')
      return Promise.reject(error)
    }

    if (!error.response) {
      ElMessage.error('网络请求失败，请检查后端服务是否启动')
    } else if (error.response.status >= 500) {
      ElMessage.error('服务器开小差了，请稍后再试')
    }
    return Promise.reject(error)
  }
)

export default request

function handleUnauthorized(message?: string) {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')

  if (!authRedirecting) {
    authRedirecting = true
    ElMessage.error(message || '未登录或登录已过期，请重新登录')
    router.push('/login').finally(() => {
      authRedirecting = false
    })
  }
}
