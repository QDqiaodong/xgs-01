<template>
  <div class="page-container">
    <div class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">闲置好物置换市集</h1>
        <p class="hero-subtitle">让闲置物品流动起来，邻里互助，绿色环保</p>
        <el-button type="primary" size="large" @click="$router.push('/market')">
          逛市集
        </el-button>
        <el-button size="large" @click="$router.push('/publish')">
          发布闲置
        </el-button>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Star /></el-icon>
          精选置顶
        </h2>
        <el-link type="primary" @click="$router.push('/market')">查看更多</el-link>
      </div>
      <div class="card-grid">
        <div 
          v-for="item in topItems" 
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
    </div>

    <div class="section">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><TrendCharts /></el-icon>
          热门品类
        </h2>
      </div>
      <div class="category-grid">
        <div 
          v-for="cat in categories" 
          :key="cat.id" 
          class="category-card"
          @click="goMarket(cat.id)"
        >
          <div class="category-icon">
            <el-icon size="40"><component :is="cat.icon" /></el-icon>
          </div>
          <div class="category-name">{{ cat.name }}</div>
          <div class="category-count">{{ cat.count }}件物品</div>
        </div>
      </div>
    </div>

    <div class="section" v-if="recentHistory.length > 0">
      <div class="section-header">
        <h2 class="section-title">
          <el-icon><Clock /></el-icon>
          最近浏览
        </h2>
        <el-link type="primary" @click="$router.push('/my')">查看全部</el-link>
      </div>
      <div class="history-scroll">
        <div 
          v-for="item in recentHistory" 
          :key="item.id" 
          class="history-card"
          @click="goDetail(item.id)"
        >
          <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="history-image" @error="handleImageError" />
          <div class="history-content">
            <div class="history-title">{{ item.title }}</div>
            <span class="history-category">{{ getCategoryName(item) }}</span>
            <div class="history-time">{{ formatBrowseTime(item.browseTime) }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { getCategoryName } from '@/utils/category'
import { useFavoriteStore } from '@/stores/favorite'
import { useUserStore } from '@/stores/user'
import { useHistoryStore } from '@/stores/history'

const router = useRouter()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()
const historyStore = useHistoryStore()

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const recentHistory = computed(() => historyStore.recentHistory)
const formatBrowseTime = (timestamp) => historyStore.formatBrowseTime(timestamp)

const isFavorited = (itemId) => favoriteStore.isFavorited(itemId)

const toggleFavorite = async (item) => {
  const result = await favoriteStore.toggleFavorite(item.id)
  if (result !== undefined) {
    item.favorited = favoriteStore.isFavorited(item.id)
  }
}

const topItems = ref([
  {
    _isMock: true,
    id: 1,
    title: '小米空气净化器Pro H',
    categoryName: '数码家电',
    description: '九成新，使用一年，功能完好，除醛效果好',
    condition: '九成新',
    createTime: '2天前',
    images: ['https://picsum.photos/400/300?random=1']
  },
  {
    _isMock: true,
    id: 2,
    title: '儿童绘本套装30册',
    categoryName: '图书文具',
    description: '适合3-6岁儿童，培养阅读兴趣',
    condition: '全新',
    createTime: '1天前',
    images: ['https://picsum.photos/400/300?random=2']
  },
  {
    _isMock: true,
    id: 3,
    title: '宜家懒人沙发',
    categoryName: '家居用品',
    description: '舒适休闲，可折叠，小户型必备',
    condition: '八成新',
    createTime: '3天前',
    images: ['https://picsum.photos/400/300?random=3']
  },
  {
    _isMock: true,
    id: 4,
    title: '儿童平衡车',
    categoryName: '运动户外',
    description: '适合2-5岁宝宝，锻炼平衡能力',
    condition: '七成新',
    createTime: '5天前',
    images: ['https://picsum.photos/400/300?random=4']
  }
])

const categories = ref([
  { id: 1, name: '数码家电', icon: 'Monitor', count: 128 },
  { id: 2, name: '图书文具', icon: 'Reading', count: 256 },
  { id: 3, name: '家居用品', icon: 'House', count: 189 },
  { id: 4, name: '母婴儿童', icon: 'Cpu', count: 167 },
  { id: 5, name: '运动户外', icon: 'Bicycle', count: 98 },
  { id: 6, name: '服饰鞋包', icon: 'Trophy', count: 234 }
])

const goDetail = (id) => {
  router.push(`/detail/${id}`)
}

const goMarket = (categoryId) => {
  router.push({ path: '/market', query: { category: categoryId } })
}

onMounted(async () => {
  try {
    const params = {}
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get('/item/top', { params })
    if (res.data.success) {
      topItems.value = res.data.data
      favoriteStore.setFavoritedFromItems(res.data.data)
    }
  } catch (e) {
    console.log('使用模拟数据')
  }
})
</script>

<style lang="scss" scoped>
.hero-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 60px 40px;
  margin-bottom: 40px;
  color: white;
  text-align: center;

  .hero-title {
    font-size: 42px;
    font-weight: bold;
    margin-bottom: 16px;
  }

  .hero-subtitle {
    font-size: 18px;
    margin-bottom: 32px;
    opacity: 0.9;
  }

  .el-button {
    margin: 0 10px;
  }
}

.section {
  margin-bottom: 40px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .section-title {
      font-size: 22px;
      font-weight: 600;
      color: #303133;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.category-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  .category-icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
  }

  .category-name {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 8px;
  }

  .category-count {
    font-size: 14px;
    color: #909399;
  }
}

.history-scroll {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 12px;
  scroll-behavior: smooth;

  &::-webkit-scrollbar {
    height: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-track {
    background: #f5f7fa;
  }
}

.history-card {
  flex-shrink: 0;
  width: 220px;
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  }

  .history-image {
    width: 100%;
    height: 140px;
    object-fit: cover;
    background: #f5f7fa;
  }

  .history-content {
    padding: 12px;

    .history-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 6px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .history-category {
      display: inline-block;
      padding: 1px 6px;
      background: #ecf5ff;
      color: #409eff;
      border-radius: 3px;
      font-size: 11px;
      margin-bottom: 6px;
    }

    .history-time {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
