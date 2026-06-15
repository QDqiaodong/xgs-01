<template>
  <div class="page-container ranking-page">
    <div class="ranking-header">
      <h1 class="page-title">
        <el-icon :size="32"><Trophy /></el-icon>
        热度排行榜
      </h1>
      <p class="ranking-subtitle">综合点赞、分享、浏览量的闲置物品 Top 榜</p>
    </div>

    <div class="ranking-content">
      <div class="ranking-list">
        <div
          v-for="(item, index) in rankingItems"
          :key="item.id"
          class="ranking-item"
          :class="{ 'top-three': index < 3 }"
          @click="goDetail(item.id)"
        >
          <div class="rank-badge" :class="`rank-${index + 1}`">
            <span v-if="index < 3">{{ ['🥇', '🥈', '🥉'][index] }}</span>
            <span v-else>{{ index + 1 }}</span>
          </div>

          <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />

          <div class="item-info">
            <div class="item-title">{{ item.title }}</div>
            <div class="item-meta">
              <el-tag size="small" type="primary" effect="plain">{{ item.categoryName }}</el-tag>
              <span class="item-condition">{{ item.condition }}</span>
            </div>
            <div class="item-desc">{{ item.description }}</div>
          </div>

          <div class="item-stats">
            <div class="like-stat">
              <el-icon :size="20" :class="{ liked: isLiked(item.id) }">
                <component :is="isLiked(item.id) ? 'StarFilled' : 'Star'" />
              </el-icon>
              <span class="like-count">{{ item.likeCount || 0 }}</span>
            </div>
            <button
              class="like-btn"
              :class="{ liked: isLiked(item.id), disabled: isLikeLoading(item.id) }"
              :disabled="isLikeLoading(item.id)"
              @click.stop="handleLike(item)"
            >
              <span>{{ isLiked(item.id) ? '已赞' : '点赞' }}</span>
            </button>
          </div>
        </div>
      </div>

      <el-empty v-if="rankingItems.length === 0 && !loading" description="暂无排行数据" />

      <div class="loading-more" v-if="loading">
        <el-icon class="is-loading" :size="18"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Trophy, Star, StarFilled, Loading } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { useLikeStore } from '@/stores/like'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const likeStore = useLikeStore()
const userStore = useUserStore()

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const rankingItems = ref([])
const loading = ref(false)

const likedMap = computed(() => {
  likeStore.updateVersion
  return likeStore.likeIds
})

const isLiked = (itemId) => likedMap.value.has(Number(itemId))

const isLikeLoading = (itemId) => likeStore.isLikeLoading(itemId)

const handleLike = async (item) => {
  const result = await likeStore.toggleLike(item.id)
  if (result !== undefined) {
    item.liked = isLiked(item.id)
    if (item.likeCount === undefined || item.likeCount === null) {
      item.likeCount = 0
    }
    item.likeCount += result ? 1 : -1
    if (item.likeCount < 0) item.likeCount = 0
  }
}

const loadRanking = async () => {
  loading.value = true
  try {
    const params = { limit: 20 }
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get('/item/like-ranking', { params })
    if (res.data.success) {
      rankingItems.value = res.data.data
      likeStore.setLikedFromItems(res.data.data)
    }
  } catch (e) {
    ElMessage.error('加载排行榜失败')
  } finally {
    loading.value = false
  }
}

const goDetail = (id) => {
  router.push(`/detail/${id}`)
}

onMounted(() => {
  loadRanking()
})
</script>

<style lang="scss" scoped>
.ranking-page {
  .ranking-header {
    text-align: center;
    margin-bottom: 40px;
    padding: 40px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 16px;
    color: white;

    .page-title {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      font-size: 32px;
      font-weight: 700;
      margin: 0 0 12px;
    }

    .ranking-subtitle {
      font-size: 16px;
      margin: 0;
      opacity: 0.9;
    }
  }

  .ranking-content {
    max-width: 900px;
    margin: 0 auto;
  }

  .ranking-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .ranking-item {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 20px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    cursor: pointer;
    transition: all 0.3s;
    border: 2px solid transparent;

    &:hover {
      transform: translateX(4px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }

    &.top-three {
      border-color: #f0f0f0;

      &.rank-1 {
        border-color: #ffd700;
        background: linear-gradient(135deg, #fffbe6 0%, #ffffff 100%);
      }

      &.rank-2 {
        border-color: #c0c4cc;
        background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
      }

      &.rank-3 {
        border-color: #e6a23c;
        background: linear-gradient(135deg, #fdf6ec 0%, #ffffff 100%);
      }
    }

    .rank-badge {
      width: 50px;
      height: 50px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      font-weight: 700;
      background: #f5f7fa;
      color: #909399;
      flex-shrink: 0;

      &.rank-1 {
        background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
        color: #b8860b;
      }

      &.rank-2 {
        background: linear-gradient(135deg, #c0c4cc 0%, #e4e7ed 100%);
        color: #606266;
      }

      &.rank-3 {
        background: linear-gradient(135deg, #e6a23c 0%, #f5dab1 100%);
        color: #a0522d;
      }

      span {
        font-size: 18px;
        font-weight: 700;
      }
    }

    .item-image {
      width: 100px;
      height: 100px;
      object-fit: cover;
      border-radius: 8px;
      background: #f5f7fa;
      flex-shrink: 0;
    }

    .item-info {
      flex: 1;
      min-width: 0;

      .item-title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 8px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .item-meta {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 8px;

        .item-condition {
          font-size: 13px;
          color: #909399;
        }
      }

      .item-desc {
        font-size: 14px;
        color: #606266;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .item-stats {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      flex-shrink: 0;

      .like-stat {
        display: flex;
        align-items: center;
        gap: 6px;
        font-size: 24px;
        font-weight: 700;
        color: #e6a23c;

        .liked {
          color: #e6a23c;
        }
      }

      .like-btn {
        padding: 6px 20px;
        border-radius: 20px;
        border: none;
        cursor: pointer;
        font-size: 14px;
        font-weight: 500;
        transition: all 0.2s;
        background: #ecf5ff;
        color: #409eff;

        &:hover:not(.disabled) {
          background: #409eff;
          color: white;
        }

        &.liked {
          background: #fdf6ec;
          color: #e6a23c;

          &:hover {
            background: #fef0f0;
            color: #f56c6c;
          }
        }

        &.disabled {
          cursor: not-allowed;
          opacity: 0.6;
        }
      }
    }
  }

  .loading-more {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    padding: 30px 0;
    color: #909399;
    font-size: 14px;

    .is-loading {
      animation: rotating 1s linear infinite;
    }
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
