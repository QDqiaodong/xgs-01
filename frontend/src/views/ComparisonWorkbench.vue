<template>
  <div class="page-container">
    <el-page-header @back="goBack" content="成色对比工作台" class="page-header" />

    <div class="workbench-container" v-loading="loading" v-if="comparisonData">
      <el-card shadow="hover" class="summary-card">
        <div class="summary-header">
          <div class="summary-title">
            <el-icon :size="24" color="#409eff"><Scale /></el-icon>
            <span>对比摘要</span>
          </div>
          <div class="summary-actions">
            <el-button type="primary" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出摘要
            </el-button>
            <el-button @click="toggleCollapseDifferent">
              <el-icon v-if="hideDifferent"><Expand /></el-icon>
              <el-icon v-else><Fold /></el-icon>
              {{ hideDifferent ? '展开差异项' : '折叠相同项' }}
            </el-button>
          </div>
        </div>

        <div class="summary-stats">
          <div class="stat-item">
            <div class="stat-label">邀约状态</div>
            <el-tag :type="getStatusType(comparisonData.offerStatus)" size="large">
              {{ getStatusText(comparisonData.offerStatus) }}
            </el-tag>
          </div>
          <div class="stat-item">
            <div class="stat-label">差异项</div>
            <div class="stat-value different">
              {{ comparisonData.differentCount }} / {{ comparisonData.totalCount }}
            </div>
          </div>
          <div class="stat-item">
            <div class="stat-label">发起方</div>
            <div class="stat-value">{{ comparisonData.fromUserNickname }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">接收方</div>
            <div class="stat-value">{{ comparisonData.toUserNickname }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">发起时间</div>
            <div class="stat-value">{{ comparisonData.createTime }}</div>
          </div>
        </div>

        <el-alert
          v-if="comparisonData.offerMessage"
          :title="'交换说明：' + comparisonData.offerMessage"
          type="info"
          :closable="false"
          show-icon
        />
      </el-card>

      <el-card shadow="hover" class="items-card">
        <div class="items-header">
          <div class="item-header-item">
            <span class="item-role">发起方物品</span>
            <span class="item-title">{{ comparisonData.fromItem?.title || '-' }}</span>
          </div>
          <div class="vs-divider">
            <el-icon :size="28" color="#e6a23c"><ArrowLeftRight /></el-icon>
          </div>
          <div class="item-header-item right">
            <span class="item-role">接收方物品</span>
            <span class="item-title">{{ comparisonData.toItem?.title || '-' }}</span>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="checklist-card" v-for="(item, index) in displayChecklist" :key="item.fieldName">
        <div class="checklist-item" :class="{ 'different-row': item.isDifferent }">
          <div class="checklist-header">
            <el-checkbox v-model="checkedItems[index]" :label="item.fieldName">
              <span class="field-label">{{ item.fieldLabel }}</span>
            </el-checkbox>
            <el-tag v-if="item.isDifferent" type="danger" size="small" effect="dark">
              <el-icon><Warning /></el-icon>
              有差异
            </el-tag>
          </div>

          <div class="checklist-content">
            <div class="content-col from-col">
              <template v-if="item.isImageField">
                <div class="image-gallery">
                  <el-image
                    v-for="(img, imgIndex) in getFromImages(item)"
                    :key="'from-' + imgIndex"
                    :src="img"
                    :preview-src-list="getFromImages(item)"
                    :initial-index="imgIndex"
                    fit="cover"
                    class="compare-image"
                    @error="handleImageError"
                  />
                  <el-empty v-if="!getFromImages(item).length" description="暂无图片" :image-size="60" />
                </div>
              </template>
              <template v-else>
                <div class="text-content" :class="{ 'empty-content': !item.fromValue }">
                  {{ item.fromValue || '未填写' }}
                </div>
              </template>
            </div>

            <div class="content-divider">
              <el-divider direction="vertical" />
            </div>

            <div class="content-col to-col">
              <template v-if="item.isImageField">
                <div class="image-gallery">
                  <el-image
                    v-for="(img, imgIndex) in getToImages(item)"
                    :key="'to-' + imgIndex"
                    :src="img"
                    :preview-src-list="getToImages(item)"
                    :initial-index="imgIndex"
                    fit="cover"
                    class="compare-image"
                    @error="handleImageError"
                  />
                  <el-empty v-if="!getToImages(item).length" description="暂无图片" :image-size="60" />
                </div>
              </template>
              <template v-else>
                <div class="text-content" :class="{ 'empty-content': !item.toValue }">
                  {{ item.toValue || '未填写' }}
                </div>
              </template>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="hover" class="action-card" v-if="comparisonData.offerStatus === 'pending'">
        <div class="action-info">
          <el-icon :size="20" color="#e6a23c"><InfoFilled /></el-icon>
          <span>请仔细核对以上信息，确认无误后再进行操作</span>
        </div>
        <div class="action-buttons">
          <el-button type="success" size="large" @click="handleAccept">
            <el-icon><Check /></el-icon>
            同意互换
          </el-button>
          <el-button type="danger" size="large" @click="handleReject">
            <el-icon><Close /></el-icon>
            拒绝互换
          </el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="exportDialogVisible" title="导出对比摘要" width="600px">
      <div class="export-content" ref="exportContent">
        <h2 class="export-title">成色对比摘要</h2>
        <div class="export-meta">
          <p><strong>邀约ID：</strong>{{ comparisonData?.offerId }}</p>
          <p><strong>状态：</strong>{{ getStatusText(comparisonData?.offerStatus) }}</p>
          <p><strong>发起时间：</strong>{{ comparisonData?.createTime }}</p>
          <p><strong>发起方：</strong>{{ comparisonData?.fromUserNickname }}</p>
          <p><strong>接收方：</strong>{{ comparisonData?.toUserNickname }}</p>
          <p><strong>差异项：</strong>{{ comparisonData?.differentCount }} / {{ comparisonData?.totalCount }}</p>
        </div>
        <el-divider />
        <div class="export-items">
          <p><strong>发起方物品：</strong>{{ comparisonData?.fromItem?.title }}</p>
          <p><strong>接收方物品：</strong>{{ comparisonData?.toItem?.title }}</p>
        </div>
        <el-divider />
        <h3 class="export-subtitle">核对清单</h3>
        <table class="export-table">
          <thead>
            <tr>
              <th>核对项</th>
              <th>发起方</th>
              <th>接收方</th>
              <th>是否有差异</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in comparisonData?.checklist" :key="item.fieldName">
              <td>{{ item.fieldLabel }}</td>
              <td>{{ item.isImageField ? '[图片]' : (item.fromValue || '未填写') }}</td>
              <td>{{ item.isImageField ? '[图片]' : (item.toValue || '未填写') }}</td>
              <td :class="{ 'diff-cell': item.isDifferent }">
                {{ item.isDifferent ? '是' : '否' }}
              </td>
            </tr>
          </tbody>
        </table>
        <el-divider v-if="comparisonData?.offerMessage" />
        <div class="export-message" v-if="comparisonData?.offerMessage">
          <p><strong>交换说明：</strong>{{ comparisonData.offerMessage }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="exportDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="copyToClipboard">
          <el-icon><DocumentCopy /></el-icon>
          复制到剪贴板
        </el-button>
        <el-button type="success" @click="downloadAsText">
          <el-icon><Download /></el-icon>
          下载为文本
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

const PLACEHOLDER_IMAGE = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0MDAiIGhlaWdodD0iMzAwIiB2aWV3Qm94PSIwIDAgNDAwIDMwMCI+PHJlY3Qgd2lkdGg9IjQwMCIgaGVpZ2h0PSIzMDAiIGZpbGw9IiNmNWY3ZmEiLz48dGV4dCB4PSI1MCUiIHk9IjUwJSIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjYzBjNGNjIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+5Zu+54mH5Liq5pWl5aSn5pWwPC90ZXh0Pjwvc3ZnPg=='

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const comparisonData = ref(null)
const hideDifferent = ref(false)
const checkedItems = ref({})
const exportDialogVisible = ref(false)
const exportContent = ref(null)

const isReceived = computed(() => {
  if (!comparisonData.value) return false
  return route.params.id && comparisonData.value.toUserNickname === userStore.userInfo.nickname
})

const displayChecklist = computed(() => {
  if (!comparisonData.value?.checklist) return []
  if (hideDifferent.value) {
    return comparisonData.value.checklist.filter(item => item.isDifferent)
  }
  return comparisonData.value.checklist
})

const getFromImages = (item) => {
  if (!item.fromValue) return []
  return item.fromValue.split('|').filter(Boolean)
}

const getToImages = (item) => {
  if (!item.toValue) return []
  return item.toValue.split('|').filter(Boolean)
}

const getStatusType = (status) => {
  const map = { pending: 'warning', accepted: 'success', rejected: 'danger' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { pending: '待回复', accepted: '已同意', rejected: '已驳回' }
  return map[status] || status
}

const handleImageError = (e) => {
  e.target.src = PLACEHOLDER_IMAGE
}

const goBack = () => {
  router.back()
}

const toggleCollapseDifferent = () => {
  hideDifferent.value = !hideDifferent.value
}

const loadComparisonData = async () => {
  loading.value = true
  try {
    const res = await api.get(`/offer/${route.params.id}/comparison`, {
      params: { userId: userStore.userInfo.id }
    })
    if (res.data.success) {
      comparisonData.value = res.data.data
      comparisonData.value.checklist.forEach((item, index) => {
        checkedItems.value[index] = false
      })
    }
  } catch (e) {
    ElMessage.error('加载对比数据失败')
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  exportDialogVisible.value = true
}

const generateTextSummary = () => {
  if (!comparisonData.value) return ''
  
  let text = '========== 成色对比摘要 ==========\n\n'
  text += `邀约ID：${comparisonData.value.offerId}\n`
  text += `状态：${getStatusText(comparisonData.value.offerStatus)}\n`
  text += `发起时间：${comparisonData.value.createTime}\n`
  text += `发起方：${comparisonData.value.fromUserNickname}\n`
  text += `接收方：${comparisonData.value.toUserNickname}\n`
  text += `差异项：${comparisonData.value.differentCount} / ${comparisonData.value.totalCount}\n\n`
  text += `发起方物品：${comparisonData.value.fromItem?.title || '-'}\n`
  text += `接收方物品：${comparisonData.value.toItem?.title || '-'}\n\n`
  text += '---------- 核对清单 ----------\n\n'
  
  comparisonData.value.checklist.forEach(item => {
    text += `【${item.fieldLabel}】\n`
    text += `  发起方：${item.isImageField ? '[图片]' : (item.fromValue || '未填写')}\n`
    text += `  接收方：${item.isImageField ? '[图片]' : (item.toValue || '未填写')}\n`
    text += `  差异：${item.isDifferent ? '是' : '否'}\n\n`
  })
  
  if (comparisonData.value.offerMessage) {
    text += '---------- 交换说明 ----------\n\n'
    text += `${comparisonData.value.offerMessage}\n`
  }
  
  text += '\n================================'
  return text
}

const copyToClipboard = async () => {
  const text = generateTextSummary()
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (e) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const downloadAsText = () => {
  const text = generateTextSummary()
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `对比摘要_邀约${comparisonData.value.offerId}_${new Date().toISOString().slice(0, 10)}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  ElMessage.success('下载已开始')
}

const handleAccept = async () => {
  if (!userStore.isLoggedIn || !userStore.userInfo.id) {
    ElMessage.warning('请先登录')
    return
  }
  
  const checkedCount = Object.values(checkedItems.value).filter(Boolean).length
  const totalItems = comparisonData.value.checklist.length
  
  if (checkedCount < totalItems) {
    ElMessage.warning(`请勾选所有核对项（已勾选 ${checkedCount}/${totalItems}）`)
    return
  }
  
  try {
    await ElMessageBox.confirm('确定同意该互换邀约吗？请确认已核对所有信息。', '确认操作', {
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
    loadComparisonData()
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
    await ElMessageBox.confirm('确定拒绝该互换邀约吗？', '确认操作', {
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
    ElMessage.success('已拒绝邀约')
    loadComparisonData()
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

onMounted(() => {
  loadComparisonData()
})
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 20px;
}

.workbench-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.summary-card {
  :deep(.el-card__body) {
    padding: 24px;
  }

  .summary-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .summary-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }

    .summary-actions {
      display: flex;
      gap: 12px;
    }
  }

  .summary-stats {
    display: flex;
    gap: 40px;
    flex-wrap: wrap;
    margin-bottom: 20px;

    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 6px;

      .stat-label {
        font-size: 13px;
        color: #909399;
      }

      .stat-value {
        font-size: 16px;
        font-weight: 500;
        color: #303133;

        &.different {
          color: #f56c6c;
          font-weight: 600;
        }
      }
    }
  }
}

.items-card {
  :deep(.el-card__body) {
    padding: 0;
  }

  .items-header {
    display: flex;
    align-items: center;

    .item-header-item {
      flex: 1;
      padding: 20px 24px;
      display: flex;
      flex-direction: column;
      gap: 8px;

      &.right {
        text-align: right;
        background: #f0f9eb;
      }

      &:not(.right) {
        background: #ecf5ff;
      }

      .item-role {
        font-size: 13px;
        color: #909399;
        font-weight: 500;
      }

      .item-title {
        font-size: 18px;
        font-weight: 600;
        color: #303133;
      }
    }

    .vs-divider {
      padding: 0 16px;
    }
  }
}

.checklist-card {
  :deep(.el-card__body) {
    padding: 0;
  }

  .checklist-item {
    padding: 20px 24px;
    transition: background-color 0.3s;

    &.different-row {
      background: #fef0f0;
    }

    .checklist-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .field-label {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .checklist-content {
      display: flex;
      align-items: flex-start;
      gap: 16px;

      .content-col {
        flex: 1;
        min-width: 0;

        .image-gallery {
          display: flex;
          flex-wrap: wrap;
          gap: 10px;

          .compare-image {
            width: 120px;
            height: 120px;
            border-radius: 8px;
            cursor: pointer;
            border: 2px solid #ebeef5;
            transition: border-color 0.3s;

            &:hover {
              border-color: #409eff;
            }
          }
        }

        .text-content {
          font-size: 14px;
          line-height: 1.8;
          color: #606266;
          padding: 12px 16px;
          background: #f5f7fa;
          border-radius: 8px;
          white-space: pre-wrap;
          word-break: break-all;

          &.empty-content {
            color: #c0c4cc;
            font-style: italic;
          }
        }
      }

      .content-divider {
        height: auto;
        align-self: stretch;
        display: flex;
        align-items: stretch;
      }
    }
  }
}

.action-card {
  :deep(.el-card__body) {
    padding: 24px;
  }

  .action-info {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 20px;
    font-size: 14px;
    color: #606266;
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 24px;

    .el-button {
      min-width: 140px;
    }
  }
}

.export-content {
  padding: 10px;

  .export-title {
    text-align: center;
    font-size: 20px;
    color: #303133;
    margin-bottom: 20px;
  }

  .export-meta {
    p {
      margin: 8px 0;
      font-size: 14px;
      color: #606266;
    }
  }

  .export-items {
    p {
      margin: 8px 0;
      font-size: 14px;
      color: #606266;
    }
  }

  .export-subtitle {
    font-size: 16px;
    color: #303133;
    margin: 16px 0 12px;
  }

  .export-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;

    th, td {
      border: 1px solid #ebeef5;
      padding: 10px 12px;
      text-align: left;
    }

    th {
      background: #f5f7fa;
      font-weight: 600;
      color: #303133;
    }

    td {
      color: #606266;

      &.diff-cell {
        color: #f56c6c;
        font-weight: 600;
      }
    }
  }

  .export-message {
    p {
      margin: 8px 0;
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
    }
  }
}
</style>
