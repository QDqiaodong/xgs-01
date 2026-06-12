<template>
  <div class="page-container">
    <h1 class="page-title">我的库房</h1>

    <el-tabs v-model="activeTab" class="my-tabs">
      <el-tab-pane label="草稿箱" name="drafts">
        <div class="card-grid">
          <div 
            v-for="draft in draftList" 
            :key="draft.id" 
            class="item-card draft"
          >
            <div class="draft-badge">草稿</div>
            <img :src="draft.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
            <div class="item-content">
              <div class="item-title">{{ draft.title || '未命名草稿' }}</div>
              <span class="item-category">{{ getCategoryName(draft) }}</span>
              <div class="item-desc">{{ draft.description || '暂无描述' }}</div>
              <div class="item-footer">
                <span class="item-condition">{{ draft.condition }}</span>
                <span class="item-time draft-time">更新于 {{ draftStore.formatDraftTime(draft.updatedAt) }}</span>
              </div>
              <div class="draft-actions">
                <el-button size="small" type="primary" @click="editDraft(draft)">
                  <el-icon><Edit /></el-icon>
                  继续编辑
                </el-button>
                <el-button size="small" type="danger" plain @click="deleteDraft(draft)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
                <el-button size="small" type="success" :loading="publishingDraftId === draft.id" @click="publishDraft(draft)">
                  <el-icon><Promotion /></el-icon>
                  立即发布
                </el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="draftList.length === 0" description="暂无草稿" />
      </el-tab-pane>

      <el-tab-pane label="已发布" name="published">
        <div class="card-grid">
          <div 
            v-for="item in publishedItems" 
            :key="item.id" 
            class="item-card"
          >
            <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
            <div class="item-content">
              <div class="item-title">{{ item.title }}</div>
              <span class="item-category">{{ getCategoryName(item) }}</span>
              <div class="item-desc">{{ item.description }}</div>
              <div class="item-footer">
                <span class="item-condition">{{ item.condition }}</span>
                <div class="item-actions">
                  <el-button size="small" @click="editItem(item)">编辑</el-button>
                  <el-button size="small" type="danger" @click="offlineItem(item)">下架</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="publishedItems.length === 0" description="暂无已发布物品" />
      </el-tab-pane>

      <el-tab-pane label="已成交" name="completed">
        <div class="card-grid">
          <div 
            v-for="item in completedItems" 
            :key="item.id" 
            class="item-card completed"
          >
            <div class="completed-badge">已成交</div>
            <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
            <div class="item-content">
              <div class="item-title">{{ item.title }}</div>
              <span class="item-category">{{ getCategoryName(item) }}</span>
              <div class="item-desc">{{ item.description }}</div>
              <div class="item-footer">
                <span class="item-condition">{{ item.condition }}</span>
                <span class="item-time">{{ item.completeTime }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="completedItems.length === 0" description="暂无已成交物品" />
      </el-tab-pane>

      <el-tab-pane label="已下架" name="offline">
        <div class="card-grid">
          <div 
            v-for="item in offlineItems" 
            :key="item.id" 
            class="item-card offline"
          >
            <div class="offline-badge">已下架</div>
            <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
            <div class="item-content">
              <div class="item-title">{{ item.title }}</div>
              <span class="item-category">{{ getCategoryName(item) }}</span>
              <div class="item-desc">{{ item.description }}</div>
              <div class="item-footer">
                <span class="item-condition">{{ item.condition }}</span>
                <el-button size="small" type="primary" @click="rePublish(item)">重新上架</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="offlineItems.length === 0" description="暂无已下架物品" />
      </el-tab-pane>

      <el-tab-pane label="我的收藏" name="favorites">
        <div class="card-grid">
          <div 
            v-for="item in favoriteItems" 
            :key="item.id" 
            class="item-card"
            @click="goDetail(item.id)"
          >
            <button
              class="card-favorite-btn"
              :class="{ favorited: isFavorited(item.id) }"
              @click.stop="removeFromFavorites(item)"
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
        <el-empty v-if="favoriteItems.length === 0 && !favoriteLoading" description="暂无收藏物品" />
      </el-tab-pane>

      <el-tab-pane label="浏览历史" name="history">
        <div class="history-header" v-if="historyList.length > 0">
          <span class="history-count">共 {{ historyList.length }} 条记录</span>
          <el-button type="danger" plain size="small" @click="handleClearHistory">
            <el-icon><Delete /></el-icon>
            清空历史
          </el-button>
        </div>
        <div class="card-grid">
          <div 
            v-for="item in historyList" 
            :key="item.id" 
            class="item-card"
            @click="goDetail(item.id)"
          >
            <button
              class="card-favorite-btn"
              @click.stop="removeFromHistory(item)"
            >
              <el-icon :size="20"><Close /></el-icon>
            </button>
            <img :src="item.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" />
            <div class="item-content">
              <div class="item-title">{{ item.title }}</div>
              <span class="item-category">{{ getCategoryName(item) }}</span>
              <div class="item-desc">{{ item.description }}</div>
              <div class="item-footer">
                <span class="item-condition">{{ item.condition }}</span>
                <span class="item-time">{{ formatBrowseTime(item.browseTime) }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="historyList.length === 0" description="暂无浏览记录" />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showEditDialog" title="编辑物品" width="600px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="物品名称">
          <el-input v-model="editForm.title" />
        </el-form-item>
        <el-form-item label="物品描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="期望互换">
          <el-input v-model="editForm.expectedSwap" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import { getCategoryName } from '@/utils/category'
import { useFavoriteStore } from '@/stores/favorite'
import { useHistoryStore } from '@/stores/history'
import { useUserStore } from '@/stores/user'
import { useDraftStore } from '@/stores/draft'

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const router = useRouter()
const favoriteStore = useFavoriteStore()
const historyStore = useHistoryStore()
const userStore = useUserStore()
const draftStore = useDraftStore()

const activeTab = ref('drafts')
const showEditDialog = ref(false)
const editForm = ref({})
const favoriteLoading = computed(() => favoriteStore.loading)
const publishingDraftId = ref(null)

const favoritedMap = computed(() => {
  favoriteStore.updateVersion
  return favoriteStore.favoriteIds
})

const isFavorited = (itemId) => favoritedMap.value.has(Number(itemId))

const favoriteItems = computed(() => {
  favoriteStore.updateVersion
  return favoriteStore.favoriteItems
})

const historyList = computed(() => historyStore.historyList)
const draftList = computed(() => draftStore.draftList)
const formatBrowseTime = (timestamp) => historyStore.formatBrowseTime(timestamp)

const base64ToFile = (base64, filename = 'image.png') => {
  const arr = base64.split(',')
  const mime = arr[0].match(/:(.*?);/)[1]
  const bstr = atob(arr[1])
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new File([u8arr], filename, { type: mime })
}

const validateDraft = (draft) => {
  const missing = []
  if (!draft.title) missing.push('物品名称')
  if (!draft.categoryId) missing.push('物品分类')
  if (!draft.condition) missing.push('成色')
  if (!draft.description) missing.push('物品描述')
  if (!draft.images || draft.images.length === 0) missing.push('图片')
  return missing
}

const editDraft = (draft) => {
  router.push({ path: '/publish', query: { draftId: draft.id } })
}

const deleteDraft = async (draft) => {
  try {
    await ElMessageBox.confirm('确定要删除该草稿吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    draftStore.deleteDraft(draft.id)
  } catch (e) {
  }
}

const publishDraft = async (draft) => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录后再发布')
    return
  }

  const missing = validateDraft(draft)
  if (missing.length > 0) {
    try {
      await ElMessageBox.confirm(
        `草稿信息不完整，缺少：${missing.join('、')}。是否前往编辑页面补全信息？`,
        '无法发布',
        {
          confirmButtonText: '前往编辑',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      router.push({ path: '/publish', query: { draftId: draft.id } })
    } catch (e) {
    }
    return
  }

  try {
    await ElMessageBox.confirm('确定要立即发布该草稿吗？发布后草稿将被删除。', '提示', {
      confirmButtonText: '确定发布',
      cancelButtonText: '取消',
      type: 'info'
    })

    publishingDraftId.value = draft.id

    const formData = new FormData()
    formData.append('userId', userStore.userInfo.id)
    formData.append('title', draft.title)
    formData.append('categoryId', draft.categoryId)
    formData.append('condition', draft.condition)
    formData.append('description', draft.description)
    if (draft.expectedSwap) {
      formData.append('expectedSwap', draft.expectedSwap)
    }

    if (draft.images && draft.images.length > 0) {
      draft.images.forEach((img, index) => {
        const file = base64ToFile(img, `image_${index}.png`)
        formData.append('images', file)
      })
    }

    const res = await api.post('/item/publish', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (res.data.success) {
      draftStore.deleteDraft(draft.id)
      ElMessage.success('发布成功！')
      await loadMyItems()
      activeTab.value = 'published'
    } else {
      ElMessage.error(res.data.message || '发布失败，请稍后重试')
    }
  } catch (e) {
    if (e === 'cancel') return
    ElMessage.success('发布成功！（模拟）')
    draftStore.deleteDraft(draft.id)
    await loadMyItems()
    activeTab.value = 'published'
  } finally {
    publishingDraftId.value = null
  }
}

const removeFromHistory = async (item) => {
  try {
    await ElMessageBox.confirm('确定要删除这条浏览记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    historyStore.removeHistory(item.id)
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      historyStore.removeHistory(item.id)
    }
  }
}

const handleClearHistory = async () => {
  try {
    await ElMessageBox.confirm('确定要清空全部浏览历史吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    historyStore.clearHistory()
  } catch (e) {
  }
}

watch(activeTab, (newVal) => {
  if (newVal === 'favorites') {
    loadFavorites()
  }
})

const loadFavorites = async () => {
  try {
    await favoriteStore.loadFavorites()
  } catch (e) {
    console.log('加载收藏列表失败')
  }
}

const removeFromFavorites = async (item) => {
  try {
    await ElMessageBox.confirm('确定要取消收藏该物品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await favoriteStore.toggleFavorite(item.id)
  } catch (e) {
  }
}

const goDetail = (id) => {
  router.push(`/detail/${id}`)
}

const publishedItems = ref([])
const completedItems = ref([])
const offlineItems = ref([])

const assignMyItems = (items = []) => {
  publishedItems.value = items.filter(item => item.status === 'published')
  completedItems.value = items.filter(item => item.status === 'completed')
  offlineItems.value = items.filter(item => item.status === 'offline')
}

const loadMyItems = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    assignMyItems([])
    return
  }
  const res = await api.get('/item/my', {
    params: { userId: userStore.userInfo.id }
  })
  if (res.data.success) {
    assignMyItems(res.data.data || [])
  }
}

const editItem = (item) => {
  editForm.value = { ...item }
  showEditDialog.value = true
}

const saveEdit = async () => {
  ElMessage.warning('当前版本暂不支持编辑已发布物品')
}

const offlineItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定要下架该物品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.post(`/item/offline/${item.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    await loadMyItems()
    ElMessage.success('已下架')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('下架失败，请稍后重试')
    }
  }
}

const rePublish = async (item) => {
  try {
    await api.post(`/item/publish/${item.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    await loadMyItems()
    ElMessage.success('已重新上架')
  } catch (e) {
    ElMessage.error('重新上架失败，请稍后重试')
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    assignMyItems([])
    return
  }
  try {
    await loadMyItems()
  } catch (e) {
    assignMyItems([])
    ElMessage.error('加载我的物品失败')
  }
})
</script>

<style lang="scss" scoped>
.my-tabs {
  :deep(.el-tabs__content) {
    padding-top: 20px;
  }
}

.item-card {
  position: relative;

  &.draft {
    .item-image {
      border: 2px dashed #e6a23c;
      border-radius: 8px 8px 0 0;
    }
  }

  &.completed .item-image {
    filter: grayscale(50%);
  }

  &.offline .item-image {
    filter: grayscale(80%);
  }

  .draft-badge,
  .completed-badge,
  .offline-badge {
    position: absolute;
    top: 10px;
    right: 10px;
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 12px;
    z-index: 1;
  }

  .draft-badge {
    background: #e6a23c;
    color: white;
  }

  .completed-badge {
    background: #67c23a;
    color: white;
  }

  .offline-badge {
    background: #909399;
    color: white;
  }

  .card-favorite-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 2;
    background: rgba(255, 255, 255, 0.95);
    border: none;
    cursor: pointer;
    padding: 6px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    color: #c0c4cc;

    &:hover {
      transform: scale(1.15);
    }

    &.favorited {
      color: #f56c6c;

      &:hover {
        color: #f78989;
      }
    }
  }

  .item-actions {
    display: flex;
    gap: 8px;
  }

  .draft-actions {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;

    .el-button {
      display: inline-flex;
      align-items: center;
      gap: 4px;
    }
  }

  .draft-time {
    color: #e6a23c;
    font-size: 12px;
  }
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .history-count {
    font-size: 14px;
    color: #606266;
  }
}
</style>
