import { defineStore } from 'pinia'
import { ref } from 'vue'

type UserInfo = Record<string, unknown>

type LoginPayload = {
  token: string
  userInfo: UserInfo
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo>(JSON.parse(localStorage.getItem('userInfo') || '{}'))

  const saveLogin = (data: LoginPayload) => {
    token.value = data.token
    userInfo.value = data.userInfo
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
  }

  const logout = () => {
    token.value = ''
    userInfo.value = {}
    localStorage.clear()
  }

  return { token, userInfo, saveLogin, logout }
})
