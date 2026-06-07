<template>
  <div class="page-container">
    <h1 class="page-title">我的库房</h1>

    <el-tabs v-model="activeTab" class="my-tabs">
      <el-tab-pane label="已发布" name="published">
        <div class="card-grid">
          <div 
            v-for="item in publishedItems" 
            :key="item.id" 
            class="item-card"
          >
            <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
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
            <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
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
            <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
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
              class="card-favorite-btn favorited"
              @click.stop="removeFromFavorites(item)"
            >
              <el-icon :size="20"><HeartFilled /></el-icon>
            </button>
            <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
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
            <img :src="item.images?.[0] || 'https://picsum.photos/400/300'" class="item-image" />
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

const router = useRouter()
const favoriteStore = useFavoriteStore()
const historyStore = useHistoryStore()
const userStore = useUserStore()

const activeTab = ref('published')
const showEditDialog = ref(false)
const editForm = ref({})
const favoriteLoading = ref(false)

const favoriteItems = ref([])
const historyList = computed(() => historyStore.historyList)
const formatBrowseTime = (timestamp) => historyStore.formatBrowseTime(timestamp)

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
  favoriteLoading.value = true
  try {
    await favoriteStore.loadFavorites()
    favoriteItems.value = [...favoriteStore.favoriteItems]
  } catch (e) {
    console.log('加载收藏列表失败')
  } finally {
    favoriteLoading.value = false
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
    favoriteItems.value = favoriteItems.value.filter(i => Number(i.id) !== Number(item.id))
  } catch (e) {
    if (e !== 'cancel') {
      favoriteItems.value = favoriteItems.value.filter(i => Number(i.id) !== Number(item.id))
    }
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

  &.completed .item-image {
    filter: grayscale(50%);
  }

  &.offline .item-image {
    filter: grayscale(80%);
  }

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
