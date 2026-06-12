<template>
  <div class="page-container market-page">
    <h1 class="page-title">物品市集</h1>

    <div class="filter-bar">
      <el-form :inline="true" :model="filterForm">
        <el-form-item label="品类">
          <el-select v-model="filterForm.categoryId" placeholder="全部品类" clearable @change="handleFilterChange">
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
          <el-select 
            v-model="filterForm.condition" 
            placeholder="全部成色" 
            clearable
            multiple
            collapse-tags
            collapse-tags-tooltip
            style="min-width: 180px"
            @change="handleFilterChange"
          >
            <el-option label="全新" value="全新" />
            <el-option label="九成新" value="九成新" />
            <el-option label="八成新" value="八成新" />
            <el-option label="七成新" value="七成新" />
            <el-option label="六成新及以下" value="六成新及以下" />
          </el-select>
        </el-form-item>
        <el-form-item label="发布时间">
          <el-select v-model="filterForm.timeRange" placeholder="全部时间" clearable @change="handleFilterChange">
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
            @change="handleFilterChange"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button link type="primary" @click="showAdvanced = !showAdvanced">
            <el-icon style="vertical-align: middle"><component :is="showAdvanced ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
            <span style="vertical-align: middle">{{ showAdvanced ? '收起高级筛选' : '高级筛选' }}</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="advanced-panel" v-show="showAdvanced">
        <el-divider style="margin: 0 0 16px" />
        <el-form :inline="true" :model="filterForm" label-width="100px">
          <el-form-item label="有实物图">
            <el-switch v-model="filterForm.hasImages" active-text="是" inactive-text="否" @change="handleFilterChange" />
          </el-form-item>
          <el-form-item label="期望交换">
            <el-input 
              v-model="filterForm.exchangeKeyword" 
              placeholder="输入期望交换的物品关键词" 
              style="width: 280px"
              clearable
              @keyup.enter="handleSearch"
              @change="handleFilterChange"
            >
              <template #prefix>
                <el-icon><RefreshRight /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
        <div class="active-filters" v-if="activeFiltersCount > 0">
          <span class="active-label">已选条件 ({{ activeFiltersCount }}):</span>
          <el-tag 
            v-for="tag in activeFilterTags" 
            :key="tag.key" 
            closable 
            type="info" 
            size="small"
            style="margin-right: 8px; margin-bottom: 4px"
            @close="removeFilter(tag.key)"
          >
            {{ tag.label }}
          </el-tag>
          <el-button link type="primary" size="small" @click="handleReset">清空全部</el-button>
        </div>
      </div>
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
        <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
        <div class="item-content">
          <div class="item-title">{{ item.title }}</div>
          <span class="item-category">{{ getCategoryName(item) }}</span>
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
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Loading, Search, ArrowDown, ArrowUp, RefreshRight } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { getCategoryName } from '@/utils/category'
import { useFavoriteStore } from '@/stores/favorite'
import { useUserStore } from '@/stores/user'

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const router = useRouter()
const route = useRoute()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()

const favoritedMap = computed(() => {
  favoriteStore.updateVersion
  return favoriteStore.favoriteIds
})

const isFavorited = (itemId) => favoritedMap.value.has(Number(itemId))

const toggleFavorite = async (item) => {
  const result = await favoriteStore.toggleFavorite(item.id)
  if (result !== undefined) {
    item.favorited = isFavorited(item.id)
  }
}

const cardGridRef = ref(null)
const loading = ref(false)
const items = ref([])
const total = ref(0)
const noMore = ref(false)
const showAdvanced = ref(false)

const categories = ref([
  { id: 1, name: '数码家电' },
  { id: 2, name: '图书文具' },
  { id: 3, name: '家居用品' },
  { id: 4, name: '母婴儿童' },
  { id: 5, name: '运动户外' },
  { id: 6, name: '服饰鞋包' }
])

const createDefaultFilter = () => ({
  categoryId: null,
  condition: [],
  timeRange: null,
  keyword: '',
  hasImages: false,
  exchangeKeyword: '',
  page: 1,
  size: 12
})

const filterForm = ref(createDefaultFilter())

const activeFilterTags = computed(() => {
  const tags = []
  if (filterForm.value.categoryId) {
    const cat = categories.value.find(c => c.id === filterForm.value.categoryId)
    if (cat) tags.push({ key: 'categoryId', label: `品类: ${cat.name}` })
  }
  if (filterForm.value.condition && filterForm.value.condition.length > 0) {
    tags.push({ key: 'condition', label: `成色: ${filterForm.value.condition.join('/')}` })
  }
  if (filterForm.value.timeRange) {
    const timeMap = { today: '今天', week: '本周', month: '本月' }
    tags.push({ key: 'timeRange', label: `发布时间: ${timeMap[filterForm.value.timeRange]}` })
  }
  if (filterForm.value.keyword) {
    tags.push({ key: 'keyword', label: `关键词: ${filterForm.value.keyword}` })
  }
  if (filterForm.value.hasImages) {
    tags.push({ key: 'hasImages', label: '有实物图' })
  }
  if (filterForm.value.exchangeKeyword) {
    tags.push({ key: 'exchangeKeyword', label: `期望交换: ${filterForm.value.exchangeKeyword}` })
  }
  return tags
})

const activeFiltersCount = computed(() => activeFilterTags.value.length)

const removeFilter = (key) => {
  switch (key) {
    case 'categoryId':
      filterForm.value.categoryId = null
      break
    case 'condition':
      filterForm.value.condition = []
      break
    case 'timeRange':
      filterForm.value.timeRange = null
      break
    case 'keyword':
      filterForm.value.keyword = ''
      break
    case 'hasImages':
      filterForm.value.hasImages = false
      break
    case 'exchangeKeyword':
      filterForm.value.exchangeKeyword = ''
      break
  }
  handleSearch()
}

const syncToUrl = () => {
  const query = {}
  if (filterForm.value.categoryId) query.category = String(filterForm.value.categoryId)
  if (filterForm.value.condition && filterForm.value.condition.length > 0) {
    query.condition = filterForm.value.condition.join(',')
  }
  if (filterForm.value.timeRange) query.timeRange = filterForm.value.timeRange
  if (filterForm.value.keyword) query.keyword = filterForm.value.keyword
  if (filterForm.value.hasImages) query.hasImages = 'true'
  if (filterForm.value.exchangeKeyword) query.exchangeKeyword = filterForm.value.exchangeKeyword
  query.page = String(filterForm.value.page)
  query.size = String(filterForm.value.size)

  router.replace({
    path: route.path,
    query
  })
}

const initFromUrl = () => {
  const defaultFilter = createDefaultFilter()
  const { query } = route

  if (query.category) defaultFilter.categoryId = Number(query.category)
  if (query.condition) defaultFilter.condition = query.condition.split(',').filter(Boolean)
  if (query.timeRange) defaultFilter.timeRange = query.timeRange
  if (query.keyword) defaultFilter.keyword = query.keyword
  if (query.hasImages === 'true') defaultFilter.hasImages = true
  if (query.exchangeKeyword) defaultFilter.exchangeKeyword = query.exchangeKeyword
  if (query.page) defaultFilter.page = Number(query.page)
  if (query.size) defaultFilter.size = Number(query.size)

  filterForm.value = defaultFilter
}

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
  syncToUrl()
  try {
    const params = { ...filterForm.value }
    if (params.condition && params.condition.length > 0) {
      params.conditions = params.condition
    }
    delete params.condition
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
  syncToUrl()
  try {
    const params = { ...filterForm.value }
    if (params.condition && params.condition.length > 0) {
      params.conditions = params.condition
    }
    delete params.condition
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
  filterForm.value = createDefaultFilter()
  handleSearch()
}

const handleFilterChange = () => {
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
  initFromUrl()
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

.advanced-panel {
  animation: slideDown 0.3s ease;

  .active-filters {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px dashed #ebeef5;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 4px;

    .active-label {
      font-size: 13px;
      color: #606266;
      margin-right: 8px;
    }
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
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
