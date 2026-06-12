import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

export const useLikeStore = defineStore('like', () => {
  const likeIdList = ref([])
  const loading = ref(false)
  const likeLoadingMap = ref(new Map())
  const updateVersion = ref(0)

  const userStore = useUserStore()

  const likeIds = computed(() => new Set(likeIdList.value))

  const isLiked = (itemId) => {
    return likeIdList.value.includes(Number(itemId))
  }

  const isLikeLoading = (itemId) => {
    return likeLoadingMap.value.get(Number(itemId)) || false
  }

  const touchUpdate = () => {
    updateVersion.value++
  }

  const addId = (id) => {
    const numId = Number(id)
    if (!likeIdList.value.includes(numId)) {
      likeIdList.value.push(numId)
      touchUpdate()
    }
  }

  const removeId = (id) => {
    const numId = Number(id)
    const idx = likeIdList.value.indexOf(numId)
    if (idx > -1) {
      likeIdList.value.splice(idx, 1)
      touchUpdate()
    }
  }

  const setLikeLoading = (id, loading) => {
    likeLoadingMap.value.set(Number(id), loading)
  }

  const toggleLike = async (itemId) => {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      return undefined
    }
    const id = Number(itemId)
    const currentlyLiked = likeIdList.value.includes(id)

    setLikeLoading(id, true)
    try {
      if (currentlyLiked) {
        await api.delete(`/item/like/${id}`, { params: { userId: userStore.userInfo.id } })
        removeId(id)
        ElMessage.success('已取消点赞')
      } else {
        await api.post(`/item/like/${id}`, null, { params: { userId: userStore.userInfo.id } })
        addId(id)
        ElMessage.success('点赞成功')
      }
      return !currentlyLiked
    } catch (e) {
      const errMsg = e.response?.data?.message || '操作失败，请稍后重试'
      if (errMsg.includes('已经点过赞')) {
        addId(id)
        ElMessage.info(errMsg)
        return true
      }
      ElMessage.error(errMsg)
      return undefined
    } finally {
      setLikeLoading(id, false)
    }
  }

  const setLikedFromItems = (items) => {
    items.forEach(item => {
      if (item.liked) {
        addId(item.id)
      }
    })
  }

  const setItemLiked = (itemId, liked) => {
    if (liked) {
      addId(itemId)
    } else {
      removeId(itemId)
    }
  }

  const clearLikes = () => {
    likeIdList.value = []
    likeLoadingMap.value.clear()
    touchUpdate()
  }

  return {
    likeIds,
    likeIdList,
    loading,
    updateVersion,
    isLiked,
    isLikeLoading,
    toggleLike,
    setLikedFromItems,
    setItemLiked,
    clearLikes
  }
})
