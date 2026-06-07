import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const HISTORY_KEY_PREFIX = 'browseHistory'
const MAX_HISTORY = 100

const getStorageKey = (userId) => {
  if (userId) {
    return `${HISTORY_KEY_PREFIX}_${userId}`
  }
  return `${HISTORY_KEY_PREFIX}_guest`
}

const loadFromStorage = (userId) => {
  const key = getStorageKey(userId)
  try {
    return JSON.parse(localStorage.getItem(key) || '[]')
  } catch (e) {
    return []
  }
}

export const useHistoryStore = defineStore('history', () => {
  const userStore = useUserStore()
  const historyList = ref(loadFromStorage(userStore.userInfo?.id))

  watch(
    () => userStore.userInfo?.id,
    (newUserId, oldUserId) => {
      if (newUserId !== oldUserId) {
        historyList.value = loadFromStorage(newUserId)
      }
    }
  )

  const recentHistory = computed(() => historyList.value.slice(0, 6))

  const getCurrentStorageKey = () => {
    return getStorageKey(userStore.userInfo?.id)
  }

  const saveToStorage = () => {
    localStorage.setItem(getCurrentStorageKey(), JSON.stringify(historyList.value))
  }

  const addHistory = (item) => {
    if (!item || !item.id) return

    const id = Number(item.id)
    const existingIndex = historyList.value.findIndex(h => Number(h.id) === id)

    const historyItem = {
      id: item.id,
      title: item.title,
      description: item.description,
      condition: item.condition,
      categoryName: item.categoryName,
      categoryId: item.categoryId,
      images: item.images,
      createTime: item.createTime,
      browseTime: Date.now()
    }

    if (existingIndex > -1) {
      historyList.value.splice(existingIndex, 1)
    }

    historyList.value.unshift(historyItem)

    if (historyList.value.length > MAX_HISTORY) {
      historyList.value = historyList.value.slice(0, MAX_HISTORY)
    }

    saveToStorage()
  }

  const removeHistory = (itemId) => {
    const id = Number(itemId)
    const index = historyList.value.findIndex(h => Number(h.id) === id)
    if (index > -1) {
      historyList.value.splice(index, 1)
      saveToStorage()
    }
  }

  const clearHistory = () => {
    historyList.value = []
    saveToStorage()
    ElMessage.success('浏览历史已清空')
  }

  const formatBrowseTime = (timestamp) => {
    const now = Date.now()
    const diff = now - timestamp
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`

    const date = new Date(timestamp)
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }

  return {
    historyList,
    recentHistory,
    addHistory,
    removeHistory,
    clearHistory,
    formatBrowseTime
  }
})
