<template>
  <div class="page-container market-page">
    <h1 class="page-title">物品市集</h1>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="品类">
          <el-select v-model="filterForm.categoryId" placeholder="全部品类" clearable>
            <el-option label="全部" :value="null" />
            <el-option 
              v-for="cat in categories" 
              :key="cat.id" 
              :label="cat.name" 
              :value="cat.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="成色">
          <el-select v-model="filterForm.condition" placeholder="全部成色" clearable>
            <el-option label="全新" value="全新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新" value="七成新" />
            <el-option label="六成新及以下" value="六成新及以下" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-select v-model="filterForm.timeRange" placeholder="全部时间" clearable>
            <el-option label="今天" value="today" />
            <el-option label="本周" value="week" />
            <el-option label="本月" value="month" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-input 
            v-model="filterForm.keyword" 
            placeholder="搜索物品名称或描述" 
            style="width: 250px"
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card-grid" ref="cardGridRef">
      <div 
        v-for="item in items" 
        :key="item.id" 
        class="item-card"
        @click="goDetail(item.id)"
      >
        <button
          v-if="!item._isMock"
          class="card-favorite-btn"
          :class="{ favorited: isFavorited(item.id) }"
          @click.stop="toggleFavorite(item)"
        >
          <el-icon :size="20">
            <component :is="isFavorited(item.id) ? 'HeartFilled' : 'Heart'" />
          </el-icon>
        </button>
        <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
        <div class="item-content">
          <div class="item-title">{{ item.title }}</div>
          <span class="item-category">{{ item.categoryName }}</span>
          <div class="item-desc">{{ item.description }}</div>
          <div class="item-footer">
            <span class="item-condition">{{ item.condition }}</span>
            <span class="item-time">{{ item.createTime }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="load-more" v-if="items.length > 0">
      <el-icon v-if="loading" class="is-loading" :size="18"><Loading /></el-icon>
      <span v-if="loading">加载中...</span>
      <span v-else-if="noMore" class="no-more">—— 没有更多了 ——</span>
    </div>

    <el-empty v-if="items.length === 0 && !loading" description="暂无物品" />
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { useFavoriteStore } from '@/stores/favorite'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()

const isFavorited = (itemId) => favoriteStore.isFavorited(itemId)

const toggleFavorite = async (item) => {
  const result = await favoriteStore.toggleFavorite(item.id)
  if (result !== undefined) {
    item.favorited = favoriteStore.isFavorited(item.id)
  }
}

const cardGridRef = ref(null)
const loading = ref(false)
const items = ref([])
const total = ref(0)
const noMore = ref(false)

const categories = ref([
  { id: 1, name: '数码家电' },
  { id: 2, name: '图书文具' },
  { id: 3, name: '家居用品' },
  { id: 4, name: '母婴儿童' },
  { id: 5, name: '运动户外' },
  { id: 6, name: '服饰鞋包' }
])

const filterForm = ref({
  categoryId: null,
  condition: null,
  timeRange: null,
  keyword: '',
  page: 1,
  size: 12
})

const imageHeights = [200, 260, 320, 240, 360, 280, 220, 300, 340, 250, 380, 270]

const mockItems = (page = 1, size = 12) => {
  const list = []
  const start = (page - 1) * size
  for (let i = 0; i < size; i++) {
    const idx = start + i
    const h = imageHeights[idx % imageHeights.length]
    list.push({
      _isMock: true,
      id: idx + 1,
      title: ['闲置书籍', '家用电器', '儿童玩具', '运动器材', '数码产品', '家居装饰'][idx % 6] + (idx + 1),
      categoryName: categories.value[idx % 6].name,
      description: '这是一件非常好的闲置物品，希望能找到需要它的人',
      condition: ['全新', '九成新', '八成新', '七成新'][idx % 4],
      createTime: `${(idx % 7) + 1}天前`,
      images: [`https://picsum.photos/400/${h}?random=${idx + 10}`]
    })
  }
  return list
}

const handleSearch = async () => {
  loading.value = true
  noMore.value = false
  filterForm.value.page = 1
  try {
    const params = { ...filterForm.value }
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get('/item/list', { params })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
      noMore.value = items.value.length >= total.value
      favoriteStore.setFavoritedFromItems(res.data.data.list)
    }
  } catch (e) {
    items.value = mockItems(1, filterForm.value.size)
    total.value = 60
    noMore.value = false
  } finally {
    loading.value = false
  }
  await nextTick()
  checkScroll()
}

const loadMore = async () => {
  if (loading.value || noMore.value) return
  loading.value = true
  filterForm.value.page += 1
  try {
    const params = { ...filterForm.value }
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get('/item/list', { params })
    if (res.data.success) {
      const newList = res.data.data.list
      items.value = [...items.value, ...newList]
      total.value = res.data.data.total
      if (newList.length === 0 || items.value.length >= total.value) {
        noMore.value = true
      }
      favoriteStore.setFavoritedFromItems(newList)
    }
  } catch (e) {
    const newList = mockItems(filterForm.value.page, filterForm.value.size)
    items.value = [...items.value, ...newList]
    if (filterForm.value.page >= 5) {
      noMore.value = true
    }
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = {
    categoryId: null,
    condition: null,
    timeRange: null,
    keyword: '',
    page: 1,
    size: 12
  }
  handleSearch()
}

const goDetail = (id) => {
  router.push(`/detail/${id}`)
}

const checkScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight || document.documentElement.clientHeight
  const scrollHeight = document.documentElement.scrollHeight
  if (scrollTop + windowHeight >= scrollHeight - 200) {
    loadMore()
  }
}

const onScroll = () => {
  checkScroll()
}

onMounted(() => {
  if (route.query.category) {
    filterForm.value.categoryId = Number(route.query.category)
  }
  handleSearch()
  window.addEventListener('scroll', onScroll)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style lang="scss" scoped>
.load-more {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  padding: 30px 0 10px;
  color: #909399;
  font-size: 14px;

  .is-loading {
    animation: rotating 1s linear infinite;
  }

  .no-more {
    color: #c0c4cc;
  }
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
