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
                <div class="title-actions">
                  <button
                    class="like-btn"
                    :class="{ liked: isLiked(item.id), disabled: isLikeLoading(item.id) }"
                    :disabled="isLikeLoading(item.id)"
                    @click.stop="handleLike"
                  >
                    <el-icon :size="24">
                      <component :is="isLiked(item.id) ? 'StarFilled' : 'Star'" />
                    </el-icon>
                    <span class="like-count">{{ item.likeCount || 0 }}</span>
                  </button>
                  <button
                    class="favorite-btn"
                    :class="{ favorited: isFavorited(item.id) }"
                    @click.stop="handleToggleFavorite"
                  >
                    <el-icon :size="28">
                      <component :is="isFavorited(item.id) ? 'HeartFilled' : 'Heart'" />
                    </el-icon>
                  </button>
                  <button
                    class="share-btn"
                    @click.stop="handleOpenShareDialog"
                  >
                    <el-icon :size="28">
                      <Share />
                    </el-icon>
                  </button>
                </div>
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
                    <div class="publisher-credit" v-if="item.publisher?.creditScore !== undefined">
                      <el-rate :model-value="Number(item.publisher.creditScore)" disabled size="small" />
                      <span class="credit-score">{{ Number(item.publisher.creditScore).toFixed(1) }}分</span>
                      <span class="review-count">({{ item.publisher.reviewCount || 0 }}条评价)</span>
                    </div>
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
                <el-button 
                  size="large" 
                  type="danger" 
                  plain
                  :disabled="!canReport"
                  @click="handleOpenReportDialog"
                >
                  <el-icon><Warning /></el-icon>
                  {{ reportButtonText }}
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

      <el-dialog v-model="showReportDialog" title="举报物品" width="500px">
        <el-form :model="reportForm" label-width="100px">
          <el-form-item label="举报原因" required>
            <el-select v-model="reportForm.reasonType" placeholder="请选择举报原因">
              <el-option label="虚假信息" value="fake_info" />
              <el-option label="违禁物品" value="prohibited" />
              <el-option label="重复发布" value="duplicate" />
              <el-option label="图片与实物不符" value="image_mismatch" />
              <el-option label="欺诈行为" value="fraud" />
              <el-option label="其他原因" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细描述">
            <el-input 
              v-model="reportForm.description" 
              type="textarea" 
              :rows="4"
              placeholder="请详细描述举报原因（选填）"
              :maxlength="500"
              show-word-limit
            />
          </el-form-item>
          <el-form-item label="图片证据">
            <el-upload
              v-model:file-list="reportForm.imageFiles"
              action=""
              :auto-upload="false"
              list-type="picture-card"
              accept="image/*"
              :limit="3"
              :on-exceed="() => ElMessage.warning('最多上传3张图片')"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showReportDialog = false">取消</el-button>
          <el-button type="danger" @click="submitReport" :loading="reportSubmitting">提交举报</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="showShareDialog" title="分享物品" width="500px" class="share-dialog">
        <div class="share-content">
          <div class="share-preview" v-if="item">
            <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="share-preview-img" @error="handleShareImageError" />
            <div class="share-preview-info">
              <div class="share-preview-title">{{ item.title }}</div>
              <div class="share-preview-meta">
                <el-tag size="small" type="primary" effect="plain">{{ item.categoryName }}</el-tag>
                <span>{{ item.condition }}</span>
              </div>
            </div>
          </div>

          <el-divider />

          <div class="share-actions">
            <el-button type="primary" size="large" @click="handleCopyLink" :loading="copyingLink">
              <el-icon><Link /></el-icon>
              复制分享链接
            </el-button>
            <el-button type="success" size="large" @click="handleGeneratePoster" :loading="generatingPoster">
              <el-icon><Picture /></el-icon>
              生成分享海报
            </el-button>
          </div>

          <el-divider />

          <div class="share-link-section" v-if="shareLink">
            <div class="share-link-label">分享链接：</div>
            <div class="share-link-box">
              <span class="share-link-text">{{ shareLink }}</span>
              <el-button type="primary" text @click="handleCopyLink">
                <el-icon><DocumentCopy /></el-icon>
                复制
              </el-button>
            </div>
          </div>

          <div class="share-tip">
            <el-icon color="#e6a23c"><InfoFilled /></el-icon>
            <span>分享链接包含您的专属标识，好友通过链接访问时将自动记录为您的分享</span>
          </div>
        </div>
      </el-dialog>

      <el-dialog v-model="showPosterDialog" title="分享海报" width="420px" class="poster-dialog">
        <div class="poster-content">
          <div class="poster-container" ref="posterContainer">
            <div class="poster-header">
              <div class="poster-logo">
                <el-icon :size="32" color="#409eff"><ShoppingCart /></el-icon>
                <span class="poster-title">闲置互换</span>
              </div>
              <div class="poster-subtitle">发现好物，以物换物</div>
            </div>
            <div class="poster-body" v-if="item">
              <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="poster-image" @error="handlePosterImageError" />
              <div class="poster-item-info">
                <div class="poster-item-title">{{ item.title }}</div>
                <div class="poster-item-meta">
                  <el-tag size="small" type="primary" effect="plain">{{ item.categoryName }}</el-tag>
                  <el-tag size="small" effect="plain">{{ item.condition }}</el-tag>
                </div>
                <div class="poster-item-desc">{{ item.description }}</div>
              </div>
              <div class="poster-qr-section">
                <div class="poster-qr-code" ref="qrCodeContainer"></div>
                <div class="poster-qr-tip">
                  <div>扫码查看详情</div>
                  <div class="poster-share-by">分享者：{{ userStore.userInfo?.nickname || '用户' }}</div>
                </div>
              </div>
            </div>
          </div>
          <div class="poster-actions">
            <el-button type="primary" size="large" @click="handleDownloadPoster" :loading="downloadingPoster">
              <el-icon><Download /></el-icon>
              保存海报
            </el-button>
          </div>
        </div>
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
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, Star, StarFilled, Plus, Share, Link, DocumentCopy, InfoFilled, ShoppingCart, Download } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { getCategoryName } from '@/utils/category'
import { useFavoriteStore } from '@/stores/favorite'
import { useLikeStore } from '@/stores/like'
import { useUserStore } from '@/stores/user'
import { useHistoryStore } from '@/stores/history'
import ImagePreview from '@/components/ImagePreview.vue'
import QRCode from 'qrcode'
import html2canvas from 'html2canvas'

const router = useRouter()
const route = useRoute()
const favoriteStore = useFavoriteStore()
const likeStore = useLikeStore()
const userStore = useUserStore()
const historyStore = useHistoryStore()

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const item = ref(null)
const currentImage = ref('')
const showOfferDialog = ref(false)
const failedThumbnailIndices = ref(new Set())
const showPreview = ref(false)
const previewIndex = ref(0)
const showReportDialog = ref(false)
const reportSubmitting = ref(false)
const showShareDialog = ref(false)
const showPosterDialog = ref(false)
const shareLink = ref('')
const copyingLink = ref(false)
const generatingPoster = ref(false)
const downloadingPoster = ref(false)
const posterContainer = ref(null)
const qrCodeContainer = ref(null)

const reportForm = ref({
  reasonType: '',
  description: '',
  imageFiles: []
})

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

const favoritedMap = computed(() => {
  favoriteStore.updateVersion
  return favoriteStore.favoriteIds
})

const isFavorited = (itemId) => favoritedMap.value.has(Number(itemId))

const likedMap = computed(() => {
  likeStore.updateVersion
  return likeStore.likeIds
})

const isLiked = (itemId) => likedMap.value.has(Number(itemId))

const isLikeLoading = (itemId) => likeStore.isLikeLoading(itemId)

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
    if (result !== undefined && item.value) {
      item.value.favorited = isFavorited(item.value.id)
    }
  }
}

const handleLike = async () => {
  if (item.value) {
    const result = await likeStore.toggleLike(item.value.id)
    if (result !== undefined && item.value) {
      item.value.liked = isLiked(item.value.id)
      if (item.value.likeCount === undefined || item.value.likeCount === null) {
        item.value.likeCount = 0
      }
      item.value.likeCount += result ? 1 : -1
      if (item.value.likeCount < 0) item.value.likeCount = 0
    }
  }
}

const loadDetail = async () => {
  try {
    const params = {}
    if (userStore.isLoggedIn && userStore.userInfo.id) {
      params.userId = userStore.userInfo.id
    }
    if (route.query.sharerId) {
      params.sharerId = route.query.sharerId
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
      if (res.data.data.liked) {
        likeStore.setItemLiked(res.data.data.id, true)
      }
      historyStore.addHistory(res.data.data)
    }
  } catch (e) {
    item.value = null
    currentImage.value = PLACEHOLDER_IMAGE
    ElMessage.error('加载物品详情失败')
  }
}

const generateShareLink = () => {
  if (!item.value || !userStore.isLoggedIn || !userStore.userInfo.id) {
    return ''
  }
  const baseUrl = window.location.origin
  return `${baseUrl}/detail/${item.value.id}?sharerId=${userStore.userInfo.id}`
}

const handleOpenShareDialog = () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录后再分享')
    return
  }
  shareLink.value = generateShareLink()
  showShareDialog.value = true
}

const handleCopyLink = async () => {
  if (!shareLink.value) {
    shareLink.value = generateShareLink()
  }
  if (!shareLink.value) {
    ElMessage.warning('请先登录')
    return
  }
  copyingLink.value = true
  try {
    await navigator.clipboard.writeText(shareLink.value)
    await recordShare('link')
    ElMessage.success('分享链接已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  } finally {
    copyingLink.value = false
  }
}

const handleGeneratePoster = async () => {
  if (!item.value || !userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  generatingPoster.value = true
  try {
    shareLink.value = generateShareLink()
    showShareDialog.value = false
    showPosterDialog.value = true
    await nextTick()
    await generateQRCode()
    await recordShare('poster')
  } catch (e) {
    ElMessage.error('生成海报失败，请重试')
  } finally {
    generatingPoster.value = false
  }
}

const generateQRCode = async () => {
  if (!qrCodeContainer.value || !shareLink.value) return
  try {
    qrCodeContainer.value.innerHTML = ''
    await QRCode.toCanvas(qrCodeContainer.value, shareLink.value, {
      width: 120,
      margin: 1,
      color: {
        dark: '#303133',
        light: '#ffffff'
      }
    })
  } catch (e) {
    console.error('QR code generation failed:', e)
  }
}

const handleDownloadPoster = async () => {
  if (!posterContainer.value) return
  downloadingPoster.value = true
  try {
    const canvas = await html2canvas(posterContainer.value, {
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      scale: 2
    })
    const link = document.createElement('a')
    link.download = `分享海报_${item.value?.title || '物品'}_${Date.now()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('海报已保存到本地')
  } catch (e) {
    console.error('Poster download failed:', e)
    ElMessage.error('保存海报失败，请重试')
  } finally {
    downloadingPoster.value = false
  }
}

const recordShare = async (shareType) => {
  if (!item.value || !userStore.isLoggedIn || !userStore.userInfo.id) return
  try {
    await api.post(`/item/share/${item.value.id}`, null, {
      params: {
        userId: userStore.userInfo.id,
        shareType: shareType
      }
    })
    if (item.value) {
      item.value.shareCount = (item.value.shareCount || 0) + 1
    }
  } catch (e) {
    console.error('Record share failed:', e)
  }
}

const handleShareImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const handlePosterImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
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

const canReport = computed(() => {
  if (!item.value) return false
  if (!userStore.isLoggedIn || !userStore.userInfo.id) return false
  if (item.value.userId === userStore.userInfo.id) return false
  return true
})

const reportButtonText = computed(() => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) return '请先登录'
  if (item.value && item.value.userId === userStore.userInfo.id) return '不能举报自己的物品'
  return '举报'
})

const handleOpenReportDialog = () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  if (!item.value) {
    ElMessage.warning('物品信息加载失败')
    return
  }
  if (item.value.userId === userStore.userInfo.id) {
    ElMessage.warning('不能举报自己的物品')
    return
  }
  reportForm.value = { reasonType: '', description: '', imageFiles: [] }
  showReportDialog.value = true
}

const submitReport = async () => {
  if (!reportForm.value.reasonType) {
    ElMessage.warning('请选择举报原因')
    return
  }
  if (!item.value) return
  reportSubmitting.value = true
  try {
    const formData = new FormData()
    formData.append('userId', userStore.userInfo.id)
    formData.append('itemId', item.value.id)
    formData.append('reasonType', reportForm.value.reasonType)
    if (reportForm.value.description) {
      formData.append('description', reportForm.value.description)
    }
    if (reportForm.value.imageFiles && reportForm.value.imageFiles.length > 0) {
      for (const file of reportForm.value.imageFiles) {
        if (file.raw) {
          formData.append('images', file.raw)
        }
      }
    }
    const res = await api.post('/report/submit', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success) {
      ElMessage.success('举报已提交，我们会尽快处理')
      showReportDialog.value = false
    }
  } catch (e) {
    if (e.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    } else {
      ElMessage.error('提交举报失败，请稍后重试')
    }
  } finally {
    reportSubmitting.value = false
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

  .title-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
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

  .like-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    background: transparent;
    border: none;
    cursor: pointer;
    padding: 8px 12px;
    border-radius: 20px;
    transition: all 0.3s;
    color: #909399;
    font-size: 14px;

    &:hover:not(.disabled) {
      color: #e6a23c;
      background: #fdf6ec;
    }

    &.liked {
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

    .like-count {
      font-weight: 500;
      min-width: 16px;
      text-align: center;
    }
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

  .share-btn {
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
      background: #ecf5ff;
      color: #409eff;
      transform: scale(1.1);
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

      .publisher-credit {
        display: flex;
        align-items: center;
        gap: 6px;
        margin-top: 6px;

        .credit-score {
          font-size: 13px;
          font-weight: 500;
          color: #e6a23c;
        }

        .review-count {
          font-size: 12px;
          color: #909399;
        }
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

:deep(.share-dialog) {
  .share-content {
    .share-preview {
      display: flex;
      gap: 16px;
      align-items: center;

      .share-preview-img {
        width: 80px;
        height: 80px;
        object-fit: cover;
        border-radius: 8px;
        background: #f5f7fa;
        flex-shrink: 0;
      }

      .share-preview-info {
        flex: 1;
        min-width: 0;

        .share-preview-title {
          font-size: 16px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 8px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .share-preview-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 13px;
          color: #909399;
        }
      }
    }

    .share-actions {
      display: flex;
      gap: 16px;
      justify-content: center;

      .el-button {
        flex: 1;
        max-width: 200px;
      }
    }

    .share-link-section {
      .share-link-label {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        margin-bottom: 8px;
      }

      .share-link-box {
        display: flex;
        align-items: center;
        gap: 12px;
        padding: 12px 16px;
        background: #f5f7fa;
        border-radius: 8px;

        .share-link-text {
          flex: 1;
          font-size: 13px;
          color: #606266;
          word-break: break-all;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
      }
    }

    .share-tip {
      display: flex;
      align-items: flex-start;
      gap: 8px;
      padding: 12px 16px;
      background: #fdf6ec;
      border-radius: 8px;
      font-size: 13px;
      color: #e6a23c;
      line-height: 1.6;
    }
  }
}

:deep(.poster-dialog) {
  .poster-content {
    .poster-container {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      border-radius: 12px;
      padding: 20px;
      margin-bottom: 20px;

      .poster-header {
        text-align: center;
        margin-bottom: 16px;
        color: white;

        .poster-logo {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 8px;
          margin-bottom: 4px;

          .poster-title {
            font-size: 20px;
            font-weight: 700;
          }
        }

        .poster-subtitle {
          font-size: 13px;
          opacity: 0.9;
        }
      }

      .poster-body {
        background: white;
        border-radius: 8px;
        padding: 16px;

        .poster-image {
          width: 100%;
          height: 200px;
          object-fit: cover;
          border-radius: 8px;
          background: #f5f7fa;
          margin-bottom: 12px;
        }

        .poster-item-info {
          margin-bottom: 16px;

          .poster-item-title {
            font-size: 16px;
            font-weight: 600;
            color: #303133;
            margin-bottom: 8px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .poster-item-meta {
            display: flex;
            align-items: center;
            gap: 8px;
            margin-bottom: 8px;
          }

          .poster-item-desc {
            font-size: 13px;
            color: #606266;
            line-height: 1.6;
            overflow: hidden;
            text-overflow: ellipsis;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
          }
        }

        .poster-qr-section {
          display: flex;
          align-items: center;
          gap: 16px;
          padding-top: 16px;
          border-top: 1px dashed #ebeef5;

          .poster-qr-code {
            flex-shrink: 0;
            width: 120px;
            height: 120px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: white;
            border: 1px solid #ebeef5;
            border-radius: 8px;

            canvas {
              width: 100% !important;
              height: 100% !important;
            }
          }

          .poster-qr-tip {
            flex: 1;
            font-size: 13px;
            color: #606266;
            line-height: 1.8;

            .poster-share-by {
              margin-top: 8px;
              font-weight: 500;
              color: #409eff;
            }
          }
        }
      }
    }

    .poster-actions {
      display: flex;
      justify-content: center;

      .el-button {
        min-width: 200px;
      }
    }
  }
}
</style>
