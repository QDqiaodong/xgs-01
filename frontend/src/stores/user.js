import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/utils/api'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const token = ref(localStorage.getItem('token') || '')

  const isLoggedIn = computed(() => !!token.value)

  const login = async (form) => {
    try {
      const res = await api.post('/user/login', form)
      if (res.data.success) {
        const { user, token: t } = res.data.data
        userInfo.value = user
        token.value = t
        localStorage.setItem('userInfo', JSON.stringify(user))
        localStorage.setItem('token', t)
        return true
      } else {
        return false
      }
    } catch (e) {
      return false
    }
  }

  const logout = () => {
    userInfo.value = {}
    token.value = ''
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token')
  }

  return {
    userInfo,
    token,
    isLoggedIn,
    login,
    logout
  }
})
