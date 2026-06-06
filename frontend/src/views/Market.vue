<template>
  <div class="page-container">
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

    <div class="card-grid">
      <div 
        v-for="item in items" 
        :key="item.id" 
        class="item-card"
        @click="goDetail(item.id)"
      >
        <button
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

    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="filterForm.page"
        v-model:page-size="filterForm.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </div>

    <el-empty v-if="items.length === 0 && !loading" description="暂无物品" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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

const loading = ref(false)
const items = ref([])
const total = ref(0)

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

const mockItems = () => {
  const list = []
  for (let i = 0; i < 12; i++) {
    list.push({
      id: i + 1,
      title: ['闲置书籍', '家用电器', '儿童玩具', '运动器材', '数码产品', '家居装饰'][i % 6] + (i + 1),
      categoryName: categories.value[i % 6].name,
      description: '这是一件非常好的闲置物品，希望能找到需要它的人',
      condition: ['全新', '九成新', '八成新', '七成新'][i % 4],
      createTime: '2天前',
      images: [`https://picsum.photos/400/300?random=${i + 10}`]
    })
  }
  return list
}

const handleSearch = async () => {
  loading.value = true
  try {
    const params = { ...filterForm.value }
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get('/item/list', { params })
    if (res.data.success) {
      items.value = res.data.data.list
      total.value = res.data.data.total
      favoriteStore.setFavoritedFromItems(res.data.data.list)
    }
  } catch (e) {
    items.value = mockItems()
    total.value = 50
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

onMounted(() => {
  if (route.query.category) {
    filterForm.value.categoryId = Number(route.query.category)
  }
  handleSearch()
})
</script>

<style lang="scss" scoped>
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}
</style>
