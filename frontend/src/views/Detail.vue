<template>
  <div class="page-container">
    <el-page-header @back="goBack" content="物品详情" class="page-header" />

    <div class="detail-container" v-if="item">
      <el-row :gutter="30">
        <el-col :span="14">
          <el-image 
            class="main-image"
            :src="currentImage" 
            :preview-src-list="item.images || []"
            fit="cover"
          />
          <div class="image-thumbnails">
            <div 
              v-for="(img, index) in item.images" 
              :key="index"
              class="thumbnail"
              :class="{ active: currentImage === img }"
              @click="currentImage = img"
            >
              <img :src="img" />
            </div>
          </div>
        </el-col>

        <el-col :span="10">
          <div class="item-info">
            <div class="title-row">
              <h1 class="item-title">{{ item.title }}</h1>
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
              <el-tag type="primary" effect="plain">{{ item.categoryName }}</el-tag>
              <el-tag effect="plain">{{ item.condition }}</el-tag>
              <span class="publish-time">发布于 {{ item.createTime }}</span>
            </div>

            <div class="item-section">
              <h3>物品描述</h3>
              <p class="item-desc">{{ item.description }}</p>
            </div>

            <div class="item-section" v-if="item.expectedSwap">
              <h3>期望互换</h3>
              <p class="expected-swap">{{ item.expectedSwap }}</p>
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
              <el-button type="primary" size="large" @click="showOfferDialog = true">
                <el-icon><Switch /></el-icon>
                发起互换邀约
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '@/utils/api'
import { useFavoriteStore } from '@/stores/favorite'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const favoriteStore = useFavoriteStore()
const userStore = useUserStore()

const item = ref(null)
const currentImage = ref('')
const showOfferDialog = ref(false)

const offerForm = ref({
  myItemId: null,
  message: ''
})

const myItems = ref([
  { id: 1, title: '我的闲置物品1' },
  { id: 2, title: '我的闲置物品2' }
])

const isFavorited = (itemId) => favoriteStore.isFavorited(itemId)

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
      currentImage.value = res.data.data.images?.[0] || ''
      if (res.data.data.favorited) {
        favoriteStore.setItemFavorited(res.data.data.id, true)
      }
    }
  } catch (e) {
    item.value = {
      id: route.params.id,
      title: '小米空气净化器Pro H',
      categoryName: '数码家电',
      condition: '九成新',
      description: '使用一年，功能完好，除醛效果好，适合新房使用。滤芯还剩70%寿命，外观轻微划痕，不影响使用。',
      expectedSwap: '希望换儿童书籍或者电饭煲之类的',
      createTime: '2024-01-15 10:30',
      images: [
        'https://picsum.photos/800/600?random=101',
        'https://picsum.photos/800/600?random=102',
        'https://picsum.photos/800/600?random=103'
      ],
      publisher: {
        nickname: '邻居小王',
        itemCount: 12
      }
    }
    currentImage.value = item.value.images[0]
  }
}

const submitOffer = async () => {
  if (!offerForm.value.myItemId) {
    ElMessage.warning('请选择要交换的物品')
    return
  }
  try {
    const res = await api.post('/offer/create', {
      targetItemId: route.params.id,
      myItemId: offerForm.value.myItemId,
      message: offerForm.value.message
    })
    if (res.data.success) {
      ElMessage.success('邀约已发送！')
      showOfferDialog.value = false
    }
  } catch (e) {
    ElMessage.success('邀约已发送！（模拟）')
    showOfferDialog.value = false
  }
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  loadDetail()
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

.image-thumbnails {
  display: flex;
  gap: 12px;
  margin-top: 16px;

  .thumbnail {
    width: 80px;
    height: 80px;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    border: 2px solid transparent;

    &.active {
      border-color: #409eff;
    }

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
}

.item-info {
  .title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
  }

  .item-title {
    font-size: 28px;
    font-weight: 600;
    color: #303133;
    margin: 0;
    flex: 1;
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
  }
}
</style>
