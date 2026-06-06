import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

export const useFavoriteStore = defineStore('favorite', () => {
  const favoriteIdList = ref([])
  const favoriteItems = ref([])
  const loading = ref(false)

  const userStore = useUserStore()

  const favoriteIds = computed(() => new Set(favoriteIdList.value))

  const isFavorited = (itemId) => {
    return favoriteIdList.value.includes(Number(itemId))
  }

  const addId = (id) => {
    const numId = Number(id)
    if (!favoriteIdList.value.includes(numId)) {
      favoriteIdList.value.push(numId)
    }
  }

  const removeId = (id) => {
    const numId = Number(id)
    const idx = favoriteIdList.value.indexOf(numId)
    if (idx > -1) {
      favoriteIdList.value.splice(idx, 1)
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
      if (currentlyFavorited) {
        removeId(id)
        favoriteItems.value = favoriteItems.value.filter(item => Number(item.id) !== id)
        ElMessage.success('已取消收藏')
        return false
      } else {
        addId(id)
        ElMessage.success('收藏成功')
        return true
      }
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
    isFavorited,
    toggleFavorite,
    loadFavorites,
    setFavoritedFromItems,
    setItemFavorited
  }
})
