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
              <span class="item-category">{{ item.categoryName }}</span>
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
              <span class="item-category">{{ item.categoryName }}</span>
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
              <span class="item-category">{{ item.categoryName }}</span>
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/utils/api'

const activeTab = ref('published')
const showEditDialog = ref(false)
const editForm = ref({})

const publishedItems = ref([
  {
    id: 1,
    title: '小米空气净化器Pro H',
    categoryName: '数码家电',
    description: '九成新，使用一年，功能完好',
    condition: '九成新',
    images: ['https://picsum.photos/400/300?random=201']
  },
  {
    id: 2,
    title: '儿童绘本套装',
    categoryName: '图书文具',
    description: '适合3-6岁儿童阅读',
    condition: '全新',
    images: ['https://picsum.photos/400/300?random=202']
  }
])

const completedItems = ref([
  {
    id: 3,
    title: '宜家懒人沙发',
    categoryName: '家居用品',
    description: '舒适休闲，可折叠',
    condition: '八成新',
    completeTime: '2024-01-10',
    images: ['https://picsum.photos/400/300?random=203']
  }
])

const offlineItems = ref([
  {
    id: 4,
    title: '旧笔记本电脑',
    categoryName: '数码家电',
    description: '联想ThinkPad，i5处理器',
    condition: '七成新',
    images: ['https://picsum.photos/400/300?random=204']
  }
])

const editItem = (item) => {
  editForm.value = { ...item }
  showEditDialog.value = true
}

const saveEdit = async () => {
  try {
    await api.put('/item/update', editForm.value)
    ElMessage.success('保存成功')
    showEditDialog.value = false
  } catch (e) {
    ElMessage.success('保存成功（模拟）')
    showEditDialog.value = false
  }
}

const offlineItem = async (item) => {
  try {
    await ElMessageBox.confirm('确定要下架该物品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.post(`/item/offline/${item.id}`)
    const index = publishedItems.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      offlineItems.value.unshift(publishedItems.value[index])
      publishedItems.value.splice(index, 1)
    }
    ElMessage.success('已下架')
  } catch (e) {
    if (e !== 'cancel') {
      const index = publishedItems.value.findIndex(i => i.id === item.id)
      if (index > -1) {
        offlineItems.value.unshift(publishedItems.value[index])
        publishedItems.value.splice(index, 1)
      }
      ElMessage.success('已下架（模拟）')
    }
  }
}

const rePublish = async (item) => {
  try {
    await api.post(`/item/publish/${item.id}`)
    const index = offlineItems.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      publishedItems.value.unshift(offlineItems.value[index])
      offlineItems.value.splice(index, 1)
    }
    ElMessage.success('已重新上架')
  } catch (e) {
    const index = offlineItems.value.findIndex(i => i.id === item.id)
    if (index > -1) {
      publishedItems.value.unshift(offlineItems.value[index])
      offlineItems.value.splice(index, 1)
    }
    ElMessage.success('已重新上架（模拟）')
  }
}

onMounted(async () => {
  try {
    const res = await api.get('/item/my')
    if (res.data.success) {
      publishedItems.value = res.data.data
    }
  } catch (e) {
    console.log('使用模拟数据')
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

  .item-actions {
    display: flex;
    gap: 8px;
  }
}
</style>
