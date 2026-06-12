import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

export const useFavoriteStore = defineStore('favorite', () => {
  const favoriteIdList = ref([])
  const favoriteItems = ref([])
  const loading = ref(false)
  const updateVersion = ref(0)

  const userStore = useUserStore()

  const favoriteIds = computed(() => new Set(favoriteIdList.value))

  const isFavorited = (itemId) => {
    return favoriteIdList.value.includes(Number(itemId))
  }

  const touchUpdate = () => {
    updateVersion.value++
  }

  const addId = (id) => {
    const numId = Number(id)
    if (!favoriteIdList.value.includes(numId)) {
      favoriteIdList.value.push(numId)
      touchUpdate()
    }
  }

  const removeId = (id) => {
    const numId = Number(id)
    const idx = favoriteIdList.value.indexOf(numId)
    if (idx > -1) {
      favoriteIdList.value.splice(idx, 1)
      touchUpdate()
    }
  }

  const toggleFavorite = async (itemId) => {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      return
    }
    const id = Number(itemId)
    const currentlyFavorited = favoriteIdList.value.includes(id)
    try {
      if (currentlyFavorited) {
        await api.delete(`/item/favorite/${id}`, { params: { userId: userStore.userInfo.id } })
        removeId(id)
        favoriteItems.value = favoriteItems.value.filter(item => Number(item.id) !== id)
        ElMessage.success('已取消收藏')
      } else {
        await api.post(`/item/favorite/${id}`, null, { params: { userId: userStore.userInfo.id } })
        addId(id)
        ElMessage.success('收藏成功')
      }
      return !currentlyFavorited
    } catch (e) {
      const errMsg = e.response?.data?.message || '操作失败，请稍后重试'
      ElMessage.error(errMsg)
      return undefined
    }
  }

  const loadFavorites = async () => {
    if (!userStore.isLoggedIn) {
      favoriteIdList.value = []
      favoriteItems.value = []
      return
    }
    loading.value = true
    try {
      const res = await api.get('/item/favorites', { params: { userId: userStore.userInfo.id } })
      if (res.data.success) {
        favoriteItems.value = res.data.data
        favoriteIdList.value = res.data.data.map(item => Number(item.id))
        touchUpdate()
      }
    } catch (e) {
      console.log('加载收藏列表失败')
    } finally {
      loading.value = false
    }
  }

  const setFavoritedFromItems = (items) => {
    items.forEach(item => {
      if (item.favorited) {
        addId(item.id)
      }
    })
  }

  const setItemFavorited = (itemId, favorited) => {
    if (favorited) {
      addId(itemId)
    } else {
      removeId(itemId)
    }
  }

  return {
    favoriteIds,
    favoriteIdList,
    favoriteItems,
    loading,
    updateVersion,
    isFavorited,
    toggleFavorite,
    loadFavorites,
    setFavoritedFromItems,
    setItemFavorited
  }
})
