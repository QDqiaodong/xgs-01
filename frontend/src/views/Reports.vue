<template>
  <div class="page-container">
    <el-page-header content="举报管理" class="page-header" />

    <div class="filter-container">
      <el-select v-model="filterStatus" placeholder="举报状态" clearable style="width: 160px" @change="loadReports">
        <el-option label="待处理" value="pending" />
        <el-option label="已通过" value="approved" />
        <el-option label="已驳回" value="rejected" />
      </el-select>
      <el-date-picker
        v-model="filterDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DDTHH:mm:ss"
        style="width: 360px"
        @change="loadReports"
      />
      <el-button type="primary" @click="loadReports">
        <el-icon><Search /></el-icon>
        查询
      </el-button>
    </div>

    <div class="report-table" v-loading="loading">
      <el-table :data="reports" stripe border style="width: 100%">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="itemTitle" label="被举报物品" min-width="160" show-overflow-tooltip />
        <el-table-column prop="reporterNickname" label="举报人" width="120" />
        <el-table-column prop="reasonType" label="举报原因" width="140">
          <template #default="{ row }">
            <el-tag :type="getReasonTagType(row.reasonType)">
              {{ getReasonLabel(row.reasonType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="详细描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="图片证据" width="120">
          <template #default="{ row }">
            <template v-if="row.images">
              <el-image
                v-for="(img, idx) in row.images.split(',')"
                :key="idx"
                :src="img"
                :preview-src-list="row.images.split(',')"
                :initial-index="idx"
                fit="cover"
                style="width: 30px; height: 30px; margin-right: 4px; border-radius: 4px; cursor: pointer"
              />
            </template>
            <span v-else style="color: #c0c4cc; font-size: 12px">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="举报时间" width="180" />
        <el-table-column prop="handlerNickname" label="处理人" width="120">
          <template #default="{ row }">
            {{ row.handlerNickname || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="handleRemark" label="处理备注" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.handleRemark || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'pending'">
              <el-button type="success" size="small" @click="handleAction(row, 'approve')">
                通过
              </el-button>
              <el-button type="danger" size="small" @click="handleAction(row, 'reject')">
                驳回
              </el-button>
            </template>
            <span v-else style="color: #909399; font-size: 13px">已处理</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadReports"
          @current-change="loadReports"
        />
      </div>
    </div>

    <el-dialog v-model="showHandleDialog" :title="handleActionType === 'approve' ? '通过举报' : '驳回举报'" width="450px">
      <el-form label-width="80px">
        <el-form-item label="处理备注">
          <el-input
            v-model="handleRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入处理备注（选填）"
            :maxlength="500"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandleDialog = false">取消</el-button>
        <el-button
          :type="handleActionType === 'approve' ? 'success' : 'danger'"
          @click="confirmHandle"
          :loading="handleLoading"
        >
          确认{{ handleActionType === 'approve' ? '通过' : '驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const loading = ref(false)
const reports = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref('')
const filterDateRange = ref(null)

const showHandleDialog = ref(false)
const handleActionType = ref('')
const handleRemark = ref('')
const handleTargetReport = ref(null)
const handleLoading = ref(false)

const getReasonLabel = (type) => {
  const map = {
    fake_info: '虚假信息',
    prohibited: '违禁物品',
    duplicate: '重复发布',
    image_mismatch: '图片与实物不符',
    fraud: '欺诈行为',
    other: '其他原因'
  }
  return map[type] || type
}

const getReasonTagType = (type) => {
  const map = {
    fake_info: 'warning',
    prohibited: 'danger',
    duplicate: 'info',
    image_mismatch: 'warning',
    fraud: 'danger',
    other: ''
  }
  return map[type] || ''
}

const getStatusLabel = (status) => {
  const map = { pending: '待处理', approved: '已通过', rejected: '已驳回' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { pending: 'warning', approved: 'success', rejected: 'danger' }
  return map[status] || ''
}

const loadReports = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (filterStatus.value) {
      params.status = filterStatus.value
    }
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      params.startTime = filterDateRange.value[0]
      params.endTime = filterDateRange.value[1]
    }
    const res = await api.get('/report/list', { params })
    if (res.data.success) {
      reports.value = res.data.data.list || []
      total.value = res.data.data.total || 0
    }
  } catch (e) {
    ElMessage.error('加载举报列表失败')
  } finally {
    loading.value = false
  }
}

const handleAction = (report, action) => {
  handleTargetReport.value = report
  handleActionType.value = action
  handleRemark.value = ''
  showHandleDialog.value = true
}

const confirmHandle = async () => {
  if (!handleTargetReport.value) return
  handleLoading.value = true
  try {
    const params = {
      handlerId: userStore.userInfo.id || 1,
      action: handleActionType.value
    }
    if (handleRemark.value) {
      params.handleRemark = handleRemark.value
    }
    const res = await api.post(`/report/handle/${handleTargetReport.value.id}`, null, { params })
    if (res.data.success) {
      ElMessage.success(handleActionType.value === 'approve' ? '举报已通过，物品已下架' : '举报已驳回')
      showHandleDialog.value = false
      loadReports()
    }
  } catch (e) {
    if (e.response?.data?.message) {
      ElMessage.error(e.response.data.message)
    } else {
      ElMessage.error('处理失败，请稍后重试')
    }
  } finally {
    handleLoading.value = false
  }
}

onMounted(() => {
  loadReports()
})
</script>

<style lang="scss" scoped>
.page-header {
  margin-bottom: 20px;
}

.filter-container {
  background: white;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.report-table {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
