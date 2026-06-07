<template>
  <div class="page-container">
    <el-page-header @back="goBack" content="邀约详情" class="page-header" />

    <div class="offer-detail-container" v-loading="loading" v-if="offer">
      <el-card shadow="hover">
        <div class="offer-status-row">
          <span class="status-label">邀约状态：</span>
          <el-tag :type="getStatusType(offer.status)" size="large">
            {{ getStatusText(offer.status) }}
          </el-tag>
        </div>

        <el-divider />

        <div class="offer-items-section">
          <div class="offer-item-card">
            <div class="item-label">我的物品</div>
            <div class="item-content" v-if="myItem">
              <img :src="myItem.images?.[0]" class="item-image" @click="goToItemDetail(myItem.id)" />
              <div class="item-info">
                <div class="item-title" @click="goToItemDetail(myItem.id)">{{ myItem.title }}</div>
                <div class="item-desc">{{ myItem.description }}</div>
              </div>
            </div>
            <el-empty v-else description="物品已下架" :image-size="60" />
          </div>

          <div class="swap-arrow">
            <el-icon :size="32" color="#409eff"><Switch /></el-icon>
            <span>换</span>
          </div>

          <div class="offer-item-card">
            <div class="item-label">对方物品</div>
            <div class="item-content" v-if="targetItem">
              <img :src="targetItem.images?.[0]" class="item-image" @click="goToItemDetail(targetItem.id)" />
              <div class="item-info">
                <div class="item-title" @click="goToItemDetail(targetItem.id)">{{ targetItem.title }}</div>
                <div class="item-desc">{{ targetItem.description }}</div>
              </div>
            </div>
            <el-empty v-else description="物品已下架" :image-size="60" />
          </div>
        </div>

        <el-divider />

        <div class="offer-meta">
          <div class="meta-row">
            <div class="meta-item">
              <el-avatar :size="36">{{ offer.fromUser?.nickname?.[0] || 'U' }}</el-avatar>
              <div class="meta-info">
                <div class="meta-label">发起方</div>
                <div class="meta-value">{{ offer.fromUser?.nickname || '未知用户' }}</div>
              </div>
            </div>
            <div class="meta-item">
              <div class="meta-info right">
                <div class="meta-label">发起时间</div>
                <div class="meta-value">{{ offer.createTime }}</div>
              </div>
              <el-icon :size="24"><Clock /></el-icon>
            </div>
          </div>
        </div>

        <div class="offer-message" v-if="offer.message">
          <div class="message-label">交换说明：</div>
          <div class="message-content">{{ offer.message }}</div>
        </div>

        <div class="offer-actions" v-if="isReceived && offer.status === 'pending'">
          <el-button type="success" size="large" @click="handleAccept">同意邀约</el-button>
          <el-button type="danger" size="large" @click="handleReject">驳回邀约</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const offer = ref(null)
const loading = ref(false)

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

const isReceived = computed(() => {
  if (!offer.value) return false
  return offer.value.toUserId === userStore.userInfo.id
})

const myItem = computed(() => {
  if (!offer.value) return null
  return isReceived.value ? offer.value.toItem : offer.value.fromItem
})

const targetItem = computed(() => {
  if (!offer.value) return null
  return isReceived.value ? offer.value.fromItem : offer.value.toItem
})

const getStatusType = (status) => {
  const map = { pending: 'warning', accepted: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待回复', accepted: '已同意', rejected: '已驳回' }
  return map[status] || status
}

const goBack = () => {
  router.back()
}

const goToItemDetail = (id) => {
  if (id) {
    router.push(`/detail/${id}`)
  }
}

const handleAccept = async () => {
  try {
    await ElMessageBox.confirm('确定同意该邀约吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })
  } catch {
    return
  }
  try {
    await api.post(`/offer/accept/${route.params.id}`)
    ElMessage.success('已同意邀约')
    loadDetail()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleReject = async () => {
  try {
    await ElMessageBox.confirm('确定驳回该邀约吗？', '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  try {
    await api.post(`/offer/reject/${route.params.id}`)
    ElMessage.success('已驳回邀约')
    loadDetail()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const loadDetail = async () => {
  loading.value = true
  try {
    const res = await api.get(`/offer/${route.params.id}`, {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.success) {
      offer.value = {
        ...res.data.data,
        createTime: formatTime(res.data.data.createTime)
      }
    }
  } catch (e) {
    ElMessage.error('加载邀约详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 20px;
}

.offer-detail-container {
  background: white;
  border-radius: 12px;

  :deep(.el-card__body) {
    padding: 30px;
  }
}

.offer-status-row {
  display: flex;
  align-items: center;
  gap: 12px;

  .status-label {
    font-size: 16px;
    font-weight: 500;
    color: #303133;
  }
}

.offer-items-section {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 0;

  .offer-item-card {
    flex: 1;
    padding: 20px;
    background: #fafbfc;
    border-radius: 10px;
    border: 1px solid #ebeef5;

    .item-label {
      font-size: 14px;
      font-weight: 500;
      color: #909399;
      margin-bottom: 16px;
    }

    .item-content {
      display: flex;
      gap: 16px;

      .item-image {
        width: 100px;
        height: 100px;
        border-radius: 8px;
        object-fit: cover;
        cursor: pointer;
      }

      .item-info {
        flex: 1;
        min-width: 0;

        .item-title {
          font-size: 16px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 8px;
          cursor: pointer;

          &:hover {
            color: #409eff;
          }
        }

        .item-desc {
          font-size: 14px;
          color: #606266;
          line-height: 1.6;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 3;
          -webkit-box-orient: vertical;
        }
      }
    }
  }

  .swap-arrow {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    color: #909399;
    font-size: 14px;
  }
}

.offer-meta {
  padding: 16px 0;

  .meta-row {
    display: flex;
    justify-content: space-between;
    gap: 20px;

    .meta-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .meta-info {
        .meta-label {
          font-size: 12px;
          color: #909399;
        }

        .meta-value {
          font-size: 15px;
          font-weight: 500;
          color: #303133;
          margin-top: 2px;
        }

        &.right {
          text-align: right;
        }
      }
    }
  }
}

.offer-message {
  padding: 20px;
  background: #f5f7fa;
  border-radius: 8px;
  margin: 20px 0;

  .message-label {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 8px;
  }

  .message-content {
    font-size: 14px;
    color: #606266;
    line-height: 1.8;
  }
}

.offer-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  padding-top: 24px;
}
</style>
