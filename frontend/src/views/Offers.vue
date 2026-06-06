<template>
  <div class="page-container">
    <h1 class="page-title">互换邀约</h1>

    <el-tabs v-model="activeTab" class="offer-tabs">
      <el-tab-pane label="收到的邀约" name="received">
        <el-card class="offer-card" v-for="offer in receivedOffers" :key="offer.id">
          <div class="offer-content">
            <div class="offer-items">
              <div class="offer-item">
                <img :src="offer.myItem.images?.[0]" class="item-thumb" />
                <div class="item-info">
                  <div class="item-title">{{ offer.myItem.title }}</div>
                  <div class="item-label">我的物品</div>
                </div>
              </div>
              <div class="offer-arrow">
                <el-icon size="24"><Switch /></el-icon>
              </div>
              <div class="offer-item">
                <img :src="offer.targetItem.images?.[0]" class="item-thumb" />
                <div class="item-info">
                  <div class="item-title">{{ offer.targetItem.title }}</div>
                  <div class="item-label">对方物品</div>
                </div>
              </div>
            </div>
            <div class="offer-detail">
              <div class="offer-from">
                <el-avatar :size="32">{{ offer.fromUser?.nickname?.[0] }}</el-avatar>
                <span>{{ offer.fromUser?.nickname }}</span>
                <span class="offer-time">{{ offer.createTime }}</span>
              </div>
              <div class="offer-message" v-if="offer.message">
                {{ offer.message }}
              </div>
              <div class="offer-actions" v-if="offer.status === 'pending'">
                <el-button type="success" size="small" @click="acceptOffer(offer)">同意</el-button>
                <el-button type="danger" size="small" @click="rejectOffer(offer)">驳回</el-button>
              </div>
              <div class="offer-status" v-else>
                <el-tag :type="offer.status === 'accepted' ? 'success' : 'danger'">
                  {{ offer.status === 'accepted' ? '已同意' : '已驳回' }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
        <el-empty v-if="receivedOffers.length === 0" description="暂无收到的邀约" />
      </el-tab-pane>

      <el-tab-pane label="发出的邀约" name="sent">
        <el-card class="offer-card" v-for="offer in sentOffers" :key="offer.id">
          <div class="offer-content">
            <div class="offer-items">
              <div class="offer-item">
                <img :src="offer.myItem.images?.[0]" class="item-thumb" />
                <div class="item-info">
                  <div class="item-title">{{ offer.myItem.title }}</div>
                  <div class="item-label">我的物品</div>
                </div>
              </div>
              <div class="offer-arrow">
                <el-icon size="24"><Switch /></el-icon>
              </div>
              <div class="offer-item">
                <img :src="offer.targetItem.images?.[0]" class="item-thumb" />
                <div class="item-info">
                  <div class="item-title">{{ offer.targetItem.title }}</div>
                  <div class="item-label">对方物品</div>
                </div>
              </div>
            </div>
            <div class="offer-detail">
              <div class="offer-from">
                <span class="offer-time">发送于 {{ offer.createTime }}</span>
              </div>
              <div class="offer-message" v-if="offer.message">
                {{ offer.message }}
              </div>
              <div class="offer-status">
                <el-tag :type="getStatusType(offer.status)">
                  {{ getStatusText(offer.status) }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
        <el-empty v-if="sentOffers.length === 0" description="暂无发出的邀约" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'

const activeTab = ref('received')

const receivedOffers = ref([
  {
    id: 1,
    myItem: {
      title: '小米空气净化器',
      images: ['https://picsum.photos/100/100?random=301']
    },
    targetItem: {
      title: '儿童绘本套装',
      images: ['https://picsum.photos/100/100?random=302']
    },
    fromUser: { nickname: '邻居小李' },
    message: '我家孩子很喜欢看书，想用这套绘本换您的净化器',
    createTime: '2024-01-15 10:30',
    status: 'pending'
  },
  {
    id: 2,
    myItem: {
      title: '宜家懒人沙发',
      images: ['https://picsum.photos/100/100?random=303']
    },
    targetItem: {
      title: '羽毛球拍一对',
      images: ['https://picsum.photos/100/100?random=304']
    },
    fromUser: { nickname: '运动达人' },
    message: '球拍几乎全新，只打过几次',
    createTime: '2024-01-14 15:20',
    status: 'accepted'
  }
])

const sentOffers = ref([
  {
    id: 3,
    myItem: {
      title: '旧笔记本电脑',
      images: ['https://picsum.photos/100/100?random=305']
    },
    targetItem: {
      title: 'iPad Air',
      images: ['https://picsum.photos/100/100?random=306']
    },
    message: '电脑还能用，办公没问题，想换个平板',
    createTime: '2024-01-13 09:15',
    status: 'pending'
  },
  {
    id: 4,
    myItem: {
      title: '微波炉',
      images: ['https://picsum.photos/100/100?random=307']
    },
    targetItem: {
      title: '电饭煲',
      images: ['https://picsum.photos/100/100?random=308']
    },
    message: '微波炉功能完好',
    createTime: '2024-01-10 14:30',
    status: 'rejected'
  }
])

const acceptOffer = async (offer) => {
  try {
    await api.post(`/offer/accept/${offer.id}`)
    offer.status = 'accepted'
    ElMessage.success('已同意邀约')
  } catch (e) {
    offer.status = 'accepted'
    ElMessage.success('已同意邀约（模拟）')
  }
}

const rejectOffer = async (offer) => {
  try {
    await api.post(`/offer/reject/${offer.id}`)
    offer.status = 'rejected'
    ElMessage.success('已驳回邀约')
  } catch (e) {
    offer.status = 'rejected'
    ElMessage.success('已驳回邀约（模拟）')
  }
}

const getStatusType = (status) => {
  const map = { pending: 'warning', accepted: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待回复', accepted: '已同意', rejected: '已驳回' }
  return map[status] || status
}

onMounted(async () => {
  try {
    const res = await api.get('/offer/list')
    if (res.data.success) {
      receivedOffers.value = res.data.data.filter(o => o.type === 'received')
      sentOffers.value = res.data.data.filter(o => o.type === 'sent')
    }
  } catch (e) {
    console.log('使用模拟数据')
  }
})
</script>

<style lang="scss" scoped>
.offer-tabs {
  :deep(.el-tabs__content) {
    padding-top: 20px;
  }
}

.offer-card {
  margin-bottom: 16px;

  .offer-content {
    display: flex;
    gap: 30px;
  }

  .offer-items {
    display: flex;
    align-items: center;
    gap: 20px;

    .offer-item {
      display: flex;
      align-items: center;
      gap: 12px;

      .item-thumb {
        width: 80px;
        height: 80px;
        border-radius: 8px;
        object-fit: cover;
      }

      .item-info {
        .item-title {
          font-size: 15px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 4px;
        }

        .item-label {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .offer-arrow {
      color: #409eff;
    }
  }

  .offer-detail {
    flex: 1;

    .offer-from {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-size: 14px;
      color: #606266;

      .offer-time {
        color: #909399;
        font-size: 13px;
        margin-left: auto;
      }
    }

    .offer-message {
      padding: 12px;
      background: #f5f7fa;
      border-radius: 6px;
      font-size: 14px;
      color: #606266;
      margin-bottom: 12px;
    }

    .offer-actions {
      display: flex;
      gap: 12px;
    }
  }
}
</style>
