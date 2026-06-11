<template>
  <div class="page-container">
    <el-page-header @back="goBack" content="物品详情" class="page-header" />

    <template v-if="item">
      <div class="detail-container">
        <el-row :gutter="30">
          <el-col :span="14">
            <el-image 
              class="main-image"
              :src="currentImage" 
              fit="cover"
              @click="handlePreviewOpen"
            >
              <template #error>
                <div class="image-slot">
                  <el-icon :size="48"><Picture /></el-icon>
                  <span>图片加载失败</span>
                </div>
              </template>
            </el-image>
            <div class="image-thumbnails">
              <div 
                v-for="(img, index) in item.images" 
                :key="index"
                class="thumbnail"
                :class="{ active: currentImage === img }"
                @click="handleThumbnailClick(img, index)"
              >
                <img 
                  :src="img" 
                  @error="handleThumbnailError($event, index)"
                />
              </div>
            </div>
          </el-col>

          <el-col :span="10">
            <div class="item-info">
              <div class="title-row">
                <h1 class="item-title" :title="item.title">{{ item.title }}</h1>
                <button
                  class="favorite-btn"
                  :class="{ favorited: isFavorited(item.id) }"
                  @click.stop="handleToggleFavorite"
                >
                  <el-icon :size="28">
                    <component :is="isFavorited(item.id) ? 'HeartFilled' : 'Heart'" />
                  </el-icon>
                </button>
              </div>
              
              <div class="item-meta">
                <el-tag type="primary" effect="plain">{{ getCategoryName(item) }}</el-tag>
                <el-tag effect="plain">{{ item.condition }}</el-tag>
                <span class="publish-time">发布于 {{ item.createTime }}</span>
              </div>

              <div class="item-section">
                <h3>物品描述</h3>
                <p class="item-desc" :title="item.description">{{ item.description }}</p>
              </div>

              <div class="item-section" v-if="item.expectedSwap">
                <h3>期望互换</h3>
                <p class="expected-swap" :title="item.expectedSwap">{{ item.expectedSwap }}</p>
              </div>

              <div class="item-section">
                <h3>发布者</h3>
                <div class="publisher">
                  <el-avatar :size="48">{{ item.publisher?.nickname?.[0] || 'U' }}</el-avatar>
                  <div class="publisher-info">
                    <div class="publisher-name">{{ item.publisher?.nickname || '用户' }}</div>
                    <div class="publisher-stats">已发布 {{ item.publisher?.itemCount || 0 }} 件物品</div>
                  </div>
                </div>
              </div>

              <div class="action-buttons">
                <el-button 
                  type="primary" 
                  size="large" 
                  :disabled="!canOffer"
                  @click="handleOpenOfferDialog"
                >
                  <el-icon><Switch /></el-icon>
                  {{ offerButtonText }}
                </el-button>
                <el-button size="large">
                  <el-icon><Message /></el-icon>
                  私信
                </el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <el-dialog v-model="showOfferDialog" title="发起互换邀约" width="500px">
        <el-form :model="offerForm" label-width="100px">
          <el-form-item label="我的物品">
            <el-select v-model="offerForm.myItemId" placeholder="选择您要交换的物品">
              <el-option 
                v-for="item in myItems" 
                :key="item.id" 
                :label="item.title" 
                :value="item.id" 
              />
            </el-select>
          </el-form-item>
          <el-form-item label="交换说明">
            <el-input 
              v-model="offerForm.message" 
              type="textarea" 
              :rows="3"
              placeholder="说明您的交换意向"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showOfferDialog = false">取消</el-button>
          <el-button type="primary" @click="submitOffer">发送邀约</el-button>
        </template>
      </el-dialog>
    </template>

    <el-empty v-else description="物品详情加载失败" />

    <ImagePreview
      v-model:visible="showPreview"
      :images="previewImageList"
      :initial-index="previewIndex"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { getCategoryName } from '@/utils/category'
import { useFavoriteStore } from '@/stores/favorite'
import { useUserStore } from '@/stores/user'
import { useHistoryStore } from '@/stores/history'
import ImagePreview from '@/components/ImagePreview.vue'

const router = useRouter()
const route = useRoute()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()
const historyStore = useHistoryStore()

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const item = ref(null)
const currentImage = ref('')
const showOfferDialog = ref(false)
const failedThumbnailIndices = ref(new Set())
const showPreview = ref(false)
const previewIndex = ref(0)

const previewImageList = computed(() => {
  if (!item.value || !item.value.images) return [PLACEHOLDER_IMAGE]
  const validImages = item.value.images.filter((_, idx) => !failedThumbnailIndices.value.has(idx))
  return validImages.length > 0 ? validImages : [PLACEHOLDER_IMAGE]
})

const offerForm = ref({
  myItemId: null,
  message: ''
})

const myItems = ref([])

const isFavorited = (itemId) => favoriteStore.isFavorited(itemId)

const canOffer = computed(() => {
  if (!item.value) return false
  if (!userStore.isLoggedIn || !userStore.userInfo.id) return false
  if (item.value.userId === userStore.userInfo.id) return false
  return item.value.status === 'published'
})

const offerButtonText = computed(() => {
  if (!item.value) return '发起互换邀约'
  if (!userStore.isLoggedIn || !userStore.userInfo.id) return '请先登录'
  if (item.value.userId === userStore.userInfo.id) return '不能互换自己的物品'
  if (item.value.status === 'offline') return '物品已下架'
  if (item.value.status === 'completed') return '物品已成交'
  return '发起互换邀约'
})

const handleOpenOfferDialog = () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  if (!item.value) {
    ElMessage.warning('物品信息加载失败')
    return
  }
  if (item.value.userId === userStore.userInfo.id) {
    ElMessage.warning('不能对自己的物品发起互换邀约')
    return
  }
  if (item.value.status === 'offline') {
    ElMessage.warning('物品已下架，无法发起互换邀约')
    return
  }
  if (item.value.status === 'completed') {
    ElMessage.warning('物品已成交，无法发起互换邀约')
    return
  }
  if (item.value.status !== 'published') {
    ElMessage.warning('该物品当前不可互换')
    return
  }
  loadMyItems()
  showOfferDialog.value = true
}

const handleThumbnailClick = (img, index) => {
  if (failedThumbnailIndices.value.has(index)) {
    currentImage.value = PLACEHOLDER_IMAGE
  } else {
    currentImage.value = img
  }
}

const handlePreviewOpen = () => {
  const validImages = previewImageList.value
  const idx = validImages.indexOf(currentImage.value)
  previewIndex.value = idx >= 0 ? idx : 0
  showPreview.value = true
}

const handleThumbnailError = (event, index) => {
  failedThumbnailIndices.value.add(index)
  event.target.src = PLACEHOLDER_IMAGE
  if (currentImage.value === item.value?.images?.[index]) {
    const validImages = item.value.images.filter((_, idx) => !failedThumbnailIndices.value.has(idx))
    currentImage.value = validImages.length > 0 ? validImages[0] : PLACEHOLDER_IMAGE
  }
}

const handleToggleFavorite = async () => {
  if (item.value) {
    const result = await favoriteStore.toggleFavorite(item.value.id)
    if (result !== false && item.value) {
      item.value.favorited = favoriteStore.isFavorited(item.value.id)
    }
  }
}

const loadDetail = async () => {
  try {
    const params = {}
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    const res = await api.get(`/item/${route.params.id}`, { params })
    if (res.data.success) {
      item.value = res.data.data
      failedThumbnailIndices.value.clear()
      const images = res.data.data.images || []
      if (images.length > 0) {
        currentImage.value = images[0]
      } else {
        currentImage.value = PLACEHOLDER_IMAGE
      }
      if (res.data.data.favorited) {
        favoriteStore.setItemFavorited(res.data.data.id, true)
      }
      historyStore.addHistory(res.data.data)
    }
  } catch (e) {
    item.value = null
    currentImage.value = PLACEHOLDER_IMAGE
    ElMessage.error('加载物品详情失败')
  }
}

const loadMyItems = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    myItems.value = []
    return
  }
  try {
    const res = await api.get('/item/my', {
      params: {
        userId: userStore.userInfo.id,
        status: 'published'
      }
    })
    if (res.data.success) {
      myItems.value = (res.data.data || []).filter(myItem => Number(myItem.id) !== Number(route.params.id))
    }
  } catch (e) {
    myItems.value = []
  }
}

const submitOffer = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  if (!offerForm.value.myItemId) {
    ElMessage.warning('请选择要交换的物品')
    return
  }
  if (!item.value) {
    ElMessage.warning('物品信息加载失败')
    return
  }
  if (item.value.userId === userStore.userInfo.id) {
    ElMessage.warning('不能对自己的物品发起互换邀约')
    return
  }
  if (item.value.status !== 'published') {
    if (item.value.status === 'offline') {
      ElMessage.warning('对方物品已下架，无法发起互换邀约')
    } else if (item.value.status === 'completed') {
      ElMessage.warning('对方物品已成交，无法发起互换邀约')
    } else {
      ElMessage.warning('对方物品当前不可互换')
    }
    return
  }
  const selectedMyItem = myItems.value.find(i => Number(i.id) === Number(offerForm.value.myItemId))
  if (selectedMyItem && selectedMyItem.status !== 'published') {
    if (selectedMyItem.status === 'offline') {
      ElMessage.warning('您选择的物品已下架，无法发起互换邀约')
    } else if (selectedMyItem.status === 'completed') {
      ElMessage.warning('您选择的物品已成交，无法发起互换邀约')
    } else {
      ElMessage.warning('您选择的物品当前不可互换')
    }
    return
  }
  try {
    const res = await api.post('/offer/create', {
      fromUserId: userStore.userInfo.id,
      fromItemId: offerForm.value.myItemId,
      toItemId: Number(route.params.id),
      message: offerForm.value.message
    })
    if (res.data.success) {
      ElMessage.success('邀约已发送！')
      showOfferDialog.value = false
      offerForm.value.myItemId = null
      offerForm.value.message = ''
    }
  } catch (e) {
    ElMessage.error('发送失败，请稍后重试')
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDetail()
  loadMyItems()
})
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 20px;
}

.detail-container {
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.main-image {
  width: 100%;
  height: 500px;
  border-radius: 8px;
  background: #f5f7fa;
}

.image-slot {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
  font-size: 14px;
  gap: 12px;
}

.image-thumbnails {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  flex-wrap: wrap;

  .thumbnail {
    width: 80px;
    height: 80px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;
    flex-shrink: 0;
    background: #f5f7fa;

    &.active {
      border-color: #409eff;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }
}

.item-info {
  .title-row {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;
    gap: 16px;
  }

  .item-title {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    margin: 0;
    flex: 1;
    min-width: 0;
    word-break: break-word;
    overflow-wrap: break-word;
    line-height: 1.4;
  }

  .favorite-btn {
    background: transparent;
    border: none;
    cursor: pointer;
    padding: 8px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
    color: #c0c4cc;
    flex-shrink: 0;

    &:hover {
      background: #fef0f0;
      color: #f56c6c;
      transform: scale(1.1);
    }

    &.favorited {
      color: #f56c6c;

      &:hover {
        background: #fef0f0;
        color: #f78989;
      }
    }
  }

  .item-meta {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 24px;
    padding-bottom: 24px;
    border-bottom: 1px solid #ebeef5;
    flex-wrap: wrap;

    .publish-time {
      font-size: 14px;
      color: #909399;
    }
  }

  .item-section {
    margin-bottom: 24px;

    h3 {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
    }

    .item-desc,
    .expected-swap {
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
      word-break: break-word;
      overflow-wrap: break-word;
      white-space: pre-wrap;
    }
  }

  .publisher {
    display: flex;
    align-items: center;
    gap: 12px;

    .publisher-info {
      .publisher-name {
        font-size: 15px;
        font-weight: 500;
        color: #303133;
        word-break: break-word;
        overflow-wrap: break-word;
      }

      .publisher-stats {
        font-size: 13px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .action-buttons {
    margin-top: 32px;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
  }
}
</style>
