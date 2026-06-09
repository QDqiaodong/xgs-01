import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const DRAFT_KEY_PREFIX = 'itemDrafts'
const CURRENT_DRAFT_KEY_PREFIX = 'currentEditingDraft'

const getStorageKey = (userId) => {
  if (userId) {
    return `${DRAFT_KEY_PREFIX}_${userId}`
  }
  return `${DRAFT_KEY_PREFIX}_guest`
}

const getCurrentDraftKey = (userId) => {
  if (userId) {
    return `${CURRENT_DRAFT_KEY_PREFIX}_${userId}`
  }
  return `${CURRENT_DRAFT_KEY_PREFIX}_guest`
}

const loadFromStorage = (userId) => {
  const key = getStorageKey(userId)
  try {
    return JSON.parse(localStorage.getItem(key) || '[]')
  } catch (e) {
    return []
  }
}

const loadCurrentDraftId = (userId) => {
  const key = getCurrentDraftKey(userId)
  try {
    return localStorage.getItem(key) || null
  } catch (e) {
    return null
  }
}

const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

export const useDraftStore = defineStore('draft', () => {
  const userStore = useUserStore()
  const draftList = ref(loadFromStorage(userStore.userInfo?.id))
  const currentDraftId = ref(loadCurrentDraftId(userStore.userInfo?.id))

  watch(
    () => userStore.userInfo?.id,
    (newUserId, oldUserId) => {
      if (newUserId !== oldUserId) {
        draftList.value = loadFromStorage(newUserId)
        currentDraftId.value = loadCurrentDraftId(newUserId)
      }
    }
  )

  const getCurrentStorageKey = () => {
    return getStorageKey(userStore.userInfo?.id)
  }

  const getCurrentDraftStorageKey = () => {
    return getCurrentDraftKey(userStore.userInfo?.id)
  }

  const saveToStorage = () => {
    localStorage.setItem(getCurrentStorageKey(), JSON.stringify(draftList.value))
  }

  const saveCurrentDraftId = (id) => {
    if (id) {
      localStorage.setItem(getCurrentDraftStorageKey(), id)
    } else {
      localStorage.removeItem(getCurrentDraftStorageKey())
    }
    currentDraftId.value = id
  }

  const saveDraft = async (draftData) => {
    const images = []
    if (draftData.images && draftData.images.length > 0) {
      for (const img of draftData.images) {
        if (img instanceof File) {
          try {
            const base64 = await fileToBase64(img)
            images.push(base64)
          } catch (e) {
            console.error('图片转换失败', e)
          }
        } else if (typeof img === 'string') {
          images.push(img)
        }
      }
    }

    const draft = {
      id: draftData.id || `draft_${Date.now()}`,
      title: draftData.title || '',
      categoryId: draftData.categoryId || null,
      condition: draftData.condition || '九成新',
      description: draftData.description || '',
      expectedSwap: draftData.expectedSwap || '',
      images: images,
      updatedAt: Date.now()
    }

    const index = draftList.value.findIndex(d => d.id === draft.id)
    if (index > -1) {
      draftList.value[index] = draft
    } else {
      draftList.value.unshift(draft)
    }

    saveToStorage()
    return draft
  }

  const getDraft = (id) => {
    return draftList.value.find(d => d.id === id) || null
  }

  const getCurrentDraft = () => {
    if (!currentDraftId.value) return null
    return getDraft(currentDraftId.value)
  }

  const deleteDraft = (id) => {
    const index = draftList.value.findIndex(d => d.id === id)
    if (index > -1) {
      draftList.value.splice(index, 1)
      saveToStorage()
      if (currentDraftId.value === id) {
        saveCurrentDraftId(null)
      }
      ElMessage.success('草稿已删除')
      return true
    }
    return false
  }

  const clearDrafts = () => {
    draftList.value = []
    saveToStorage()
    saveCurrentDraftId(null)
    ElMessage.success('草稿已清空')
  }

  const formatDraftTime = (timestamp) => {
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
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  return {
    draftList,
    currentDraftId,
    saveDraft,
    getDraft,
    getCurrentDraft,
    saveCurrentDraftId,
    deleteDraft,
    clearDrafts,
    formatDraftTime
  }
})
