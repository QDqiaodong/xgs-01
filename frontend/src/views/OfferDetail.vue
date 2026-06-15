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

        <el-divider content-position="left">
          <span class="divider-title">互换进度</span>
        </el-divider>

        <div class="timeline-section">
          <el-timeline>
            <el-timeline-item
              v-for="(node, index) in timeline"
              :key="index"
              :timestamp="node.time || '待完成'"
              :color="node.color"
              :size="node.current ? 'large' : 'normal'"
              :icon="getTimelineIcon(node.icon)"
              :hollow="!node.done"
            >
              <div class="timeline-content" :class="{ 'is-current': node.current, 'is-pending': !node.done }">
                <div class="timeline-title">
                  <span class="title-text">{{ node.statusText }}</span>
                  <el-tag v-if="node.current" type="primary" size="small" effect="dark" class="current-tag">当前</el-tag>
                </div>
                <div class="timeline-remark" v-if="node.remark">{{ node.remark }}</div>
                <div class="timeline-operator" v-if="node.operator">
                  操作人：{{ node.operator }}
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>

        <el-divider />

        <div class="offer-items-section">
          <div class="offer-item-card">
            <div class="item-label">我的物品</div>
            <div class="item-content" v-if="myItem">
              <img :src="myItem.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" @click="goToItemDetail(myItem.id)" />
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
              <img :src="targetItem.images?.[0] || PLACEHOLDER_IMAGE" class="item-image" @error="handleImageError" @click="goToItemDetail(targetItem.id)" />
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

        <div class="offer-actions">
          <el-button type="primary" size="large" @click="goToComparison">
            <el-icon><Scale /></el-icon>
            成色对比工作台
          </el-button>
          <template v-if="isReceived && offer.status === 'pending'">
            <el-button type="success" size="large" @click="handleAccept">同意邀约</el-button>
            <el-button type="danger" size="large" @click="handleReject">驳回邀约</el-button>
          </template>
          <template v-if="offer.status === 'accepted' && !isActionLoading">
            <el-button type="warning" size="large" :loading="actionLoading" @click="handleHandover">
              <el-icon><Van /></el-icon>
              确认开始交接
            </el-button>
          </template>
          <template v-if="offer.status === 'handover' && !isActionLoading">
            <el-button type="success" size="large" :loading="actionLoading" @click="handleComplete">
              <el-icon><Finished /></el-icon>
              确认交接完成
            </el-button>
          </template>
        </div>

        <div class="review-section" v-if="['accepted', 'handover', 'completed'].includes(offer.status)">
          <el-divider content-position="left">
            <span class="divider-title">交易评价</span>
          </el-divider>

          <div class="review-status-row">
            <div class="review-status-item">
              <span class="status-text">我的评价：</span>
              <el-tag v-if="reviewStatus.currentUserReviewed" type="success" size="small">已评价</el-tag>
              <el-tag v-else type="info" size="small">未评价</el-tag>
            </div>
            <div class="review-status-item">
              <span class="status-text">对方评价：</span>
              <el-tag v-if="reviewStatus.targetUserReviewed" type="success" size="small">已评价</el-tag>
              <el-tag v-else type="info" size="small">未评价</el-tag>
            </div>
            <el-button
              v-if="!reviewStatus.currentUserReviewed"
              type="primary"
              size="small"
              @click="showReviewDialog = true"
            >
              去评价
            </el-button>
          </div>

          <div class="review-content" v-if="reviewStatus.myReview">
            <div class="review-label">我的评价：</div>
            <div class="review-card">
              <div class="review-rating">
                <el-rate :model-value="reviewStatus.myReview.rating" disabled />
                <span class="rating-text">{{ reviewStatus.myReview.rating }}星</span>
              </div>
              <div class="review-text" v-if="reviewStatus.myReview.content">
                {{ reviewStatus.myReview.content }}
              </div>
              <div class="review-time">{{ formatTime(reviewStatus.myReview.createTime) }}</div>
            </div>
          </div>

          <div class="review-content" v-if="reviewStatus.targetReview">
            <div class="review-label">对方评价：</div>
            <div class="review-card">
              <div class="review-rating">
                <el-rate :model-value="reviewStatus.targetReview.rating" disabled />
                <span class="rating-text">{{ reviewStatus.targetReview.rating }}星</span>
              </div>
              <div class="review-text" v-if="reviewStatus.targetReview.content">
                {{ reviewStatus.targetReview.content }}
              </div>
              <div class="review-time">{{ formatTime(reviewStatus.targetReview.createTime) }}</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="showReviewDialog" title="评价对方" width="500px">
      <el-form :model="reviewForm" label-width="80px">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.rating" :max="5" />
          <span class="form-hint">请选择1-5星评价</span>
        </el-form-item>
        <el-form-item label="评价内容">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入您的评价内容（选填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingReview" @click="submitReview">
          提交评价
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, markRaw } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Check, Close, Clock, Van, Finished, Star,
  Switch, Scale
} from '@element-plus/icons-vue'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const iconMap = {
  Plus: markRaw(Plus),
  Check: markRaw(Check),
  Close: markRaw(Close),
  Clock: markRaw(Clock),
  Van: markRaw(Van),
  Finished: markRaw(Finished),
  Star: markRaw(Star)
}

const getTimelineIcon = (iconName) => {
  return iconMap[iconName] || null
}

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const offer = ref(null)
const loading = ref(false)
const showReviewDialog = ref(false)
const submittingReview = ref(false)
const actionLoading = ref(false)
const timeline = ref([])
const reviewStatus = ref({
  canReview: false,
  currentUserReviewed: false,
  targetUserReviewed: false,
  fromUserReviewed: false,
  toUserReviewed: false,
  myReview: null,
  targetReview: null
})
const reviewForm = ref({
  rating: 5,
  content: ''
})

const isActionLoading = computed(() => actionLoading.value)

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
  const map = {
    pending: 'warning',
    accepted: 'success',
    rejected: 'danger',
    expired: 'info',
    handover: 'warning',
    completed: 'success'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待回复',
    accepted: '已同意',
    rejected: '已驳回',
    expired: '已失效',
    handover: '交接中',
    completed: '已完成'
  }
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

const goToComparison = () => {
  router.push(`/offer/${route.params.id}/comparison`)
}

const handleAccept = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
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
    await api.post(`/offer/accept/${route.params.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    ElMessage.success('已同意邀约')
    loadDetail()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleReject = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
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
    await api.post(`/offer/reject/${route.params.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    ElMessage.success('已驳回邀约')
    loadDetail()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

const handleHandover = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      '请确认已与对方协商好交接方式（线下见面或快递），确认后邀约将进入"交接中"状态。',
      '确认开始交接',
      {
        confirmButtonText: '确认交接',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await api.post(`/offer/handover/${route.params.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    ElMessage.success('已确认进入交接状态')
    loadDetail()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}

const handleComplete = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await ElMessageBox.confirm(
      '请确认物品已完成交接且双方均无异议，确认后邀约将标记为"已完成"，可进行相互评价。',
      '确认交接完成',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'success'
      }
    )
  } catch {
    return
  }
  actionLoading.value = true
  try {
    await api.post(`/offer/complete/${route.params.id}`, null, {
      params: { userId: userStore.userInfo.id }
    })
    ElMessage.success('已确认交接完成，快去评价对方吧！')
    loadDetail()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}

const loadTimeline = async () => {
  try {
    const res = await api.get(`/offer/${route.params.id}/timeline`, {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.success) {
      timeline.value = res.data.data
      if (['completed'].includes(offer.value?.status)) {
        const lastIdx = timeline.value.length - 1
        if (timeline.value[lastIdx]?.status === 'reviewed') {
          timeline.value[lastIdx].done = reviewStatus.value.currentUserReviewed && reviewStatus.value.targetUserReviewed
          timeline.value[lastIdx].color = timeline.value[lastIdx].done ? '#67c23a' : '#c0c4cc'
          if (reviewStatus.value.currentUserReviewed && reviewStatus.value.targetUserReviewed) {
            timeline.value[lastIdx].time = formatTime(new Date().toISOString())
            timeline.value[lastIdx].remark = '双方已完成相互评价'
          }
        }
      }
    }
  } catch (e) {
    console.log('加载时间轴失败')
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
      await loadReviewStatus()
      await loadTimeline()
    }
  } catch (e) {
    ElMessage.error('加载邀约详情失败')
  } finally {
    loading.value = false
  }
}

const loadReviewStatus = async () => {
  try {
    const res = await api.get(`/review/offer/${route.params.id}/status`, {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.success) {
      reviewStatus.value = res.data.data
    }
  } catch (e) {
    console.log('加载评价状态失败')
  }
}

const submitReview = async () => {
  if (!reviewForm.value.rating || reviewForm.value.rating < 1) {
    ElMessage.warning('请选择评分')
    return
  }
  submittingReview.value = true
  try {
    const res = await api.post('/review/create', {
      userId: userStore.userInfo.id,
      offerId: route.params.id,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content
    })
    if (res.data.success) {
      ElMessage.success('评价提交成功')
      showReviewDialog.value = false
      reviewForm.value = { rating: 5, content: '' }
      await loadReviewStatus()
      await loadTimeline()
    } else {
      ElMessage.error(res.data.message || '评价失败')
    }
  } catch (e) {
    ElMessage.error('评价失败，请稍后重试')
  } finally {
    submittingReview.value = false
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

.divider-title {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.timeline-section {
  padding: 10px 10px 20px 10px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4fd 100%);
  border-radius: 10px;
  margin: 10px 0 10px 0;

  :deep(.el-timeline) {
    padding-left: 10px;
  }

  :deep(.el-timeline-item__wrapper) {
    padding-left: 20px;
    padding-bottom: 24px;
  }

  :deep(.el-timeline-item__tail) {
    border-left: 2px solid #e4e7ed;
  }

  :deep(.el-timeline-item__node--large) {
    width: 18px;
    height: 18px;
    left: -5px;
  }
}

.timeline-content {
  padding: 8px 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;

  &.is-current {
    background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%);
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  &.is-pending {
    background: #fafafa;
    border-style: dashed;
    opacity: 0.8;
  }
}

.timeline-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;

  .title-text {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }

  .current-tag {
    animation: pulse 2s ease-in-out infinite;
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

.timeline-remark {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 4px;
}

.timeline-operator {
  font-size: 12px;
  color: #909399;
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
  flex-wrap: wrap;
}

.review-section {
  margin-top: 10px;

  .review-status-row {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 16px 0;
    flex-wrap: wrap;

    .review-status-item {
      display: flex;
      align-items: center;
      gap: 8px;

      .status-text {
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .review-content {
    margin-top: 16px;

    .review-label {
      font-size: 14px;
      font-weight: 500;
      color: #303133;
      margin-bottom: 10px;
    }

    .review-card {
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;

      .review-rating {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        .rating-text {
          font-size: 14px;
          font-weight: 500;
          color: #e6a23c;
        }
      }

      .review-text {
        font-size: 14px;
        color: #606266;
        line-height: 1.6;
        margin-bottom: 8px;
      }

      .review-time {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}

.form-hint {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
