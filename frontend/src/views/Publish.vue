<template>
  <div class="page-container">
    <h1 class="page-title">{{ editingDraftId ? '编辑草稿' : '发布闲置物品' }}</h1>

    <div class="form-container">
      <el-form 
        ref="formRef"
        :model="form" 
        :rules="rules"
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="物品名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入物品名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="物品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 300px">
            <el-option 
              v-for="cat in categories" 
              :key="cat.id" 
              :label="cat.name" 
              :value="cat.id" 
            />
          </el-select>
        </el-form-item>

        <el-form-item label="成色" prop="condition">
          <el-radio-group v-model="form.condition">
            <el-radio value="全新">全新</el-radio>
            <el-radio value="九成新">九成新</el-radio>
            <el-radio value="八成新">八成新</el-radio>
            <el-radio value="七成新">七成新</el-radio>
            <el-radio value="六成新及以下">六成新及以下</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="物品描述" prop="description">
          <el-input 
            v-model="form.description" 
            type="textarea" 
            :rows="4" 
            placeholder="请详细描述物品情况，包括使用时长、瑕疵等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="期望互换" prop="expectedSwap">
          <el-input v-model="form.expectedSwap" placeholder="描述您期望互换的物品类型，如：儿童书籍、小家电等" />
        </el-form-item>

        <el-form-item label="上传图片" prop="images">
          <div class="custom-uploader">
            <TransitionGroup
              tag="div"
              name="image-list"
              class="image-grid"
              @dragover.prevent="handleGridDragOver"
              @dragleave="handleGridDragLeave"
            >
              <div
                v-for="(img, index) in displayList"
                :key="img.uid"
                class="image-item-wrapper"
                :class="{ 
                  'dragging': dragState.draggingUid === img.uid,
                  'placeholder': dragState.placeholderIndex === index && !img.isAddButton,
                  'placeholder-end': dragState.placeholderIndex === fileList.length && img.isAddButton
                }"
                :draggable="!img.isAddButton"
                @dragstart="handleDragStart($event, index, img.uid)"
                @dragend="handleDragEnd"
                @dragover.prevent="handleDragOver($event, index, img.uid)"
                @dragleave="handleDragLeave"
                @drop.prevent="handleDrop"
              >
                <div
                  v-if="img.isAddButton"
                  class="add-image-btn"
                  @click="triggerAddImage"
                >
                  <el-icon class="plus-icon"><Plus /></el-icon>
                  <span class="add-text">添加图片</span>
                </div>

                <div v-else class="image-card">
                  <img :src="img.url" class="image-preview" alt="" />
                  
                  <div class="index-badge">{{ index + 1 }}</div>
                  
                  <div v-if="index === 0" class="main-badge">
                    <el-icon><Star /></el-icon>
                    <span>主图</span>
                  </div>

                  <div class="image-actions">
                    <el-tooltip content="替换图片" placement="top">
                      <div class="action-btn replace-btn" @click.stop="triggerReplaceImage(index)">
                        <el-icon><RefreshRight /></el-icon>
                      </div>
                    </el-tooltip>
                    <el-tooltip content="删除图片" placement="top">
                      <div class="action-btn delete-btn" @click.stop="handleRemoveImage(index)">
                        <el-icon><Delete /></el-icon>
                      </div>
                    </el-tooltip>
                  </div>
                </div>
              </div>
            </TransitionGroup>
            <div class="upload-tip">最多上传9张图片，第一张默认为主图，支持拖拽调整顺序</div>
          </div>

          <input
            ref="addFileInputRef"
            type="file"
            accept="image/*"
            multiple
            class="hidden-file-input"
            @change="handleAddFileChange"
          />
          <input
            ref="replaceFileInputRef"
            type="file"
            accept="image/*"
            class="hidden-file-input"
            @change="handleReplaceFileChange"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
            {{ editingDraftId ? '立即发布' : '发布物品' }}
          </el-button>
          <el-button size="large" @click="handleSaveDraft" :loading="savingDraft">
            保存草稿
          </el-button>
          <el-button size="large" @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, RefreshRight, Star } from '@element-plus/icons-vue'
import { compressImages } from '@/utils/imageCompressor'
import api from '@/utils/api'
import { useDraftStore } from '@/stores/draft'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const draftStore = useDraftStore()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const savingDraft = ref(false)
const fileList = ref([])
const editingDraftId = ref(null)
let autoSaveTimer = null

const addFileInputRef = ref(null)
const replaceFileInputRef = ref(null)
const replacingIndex = ref(-1)

const dragState = reactive({
  dragging: false,
  draggingUid: null,
  draggingIndex: -1,
  overIndex: -1,
  placeholderIndex: -1
})

const categories = ref([
  { id: 1, name: '数码家电' },
  { id: 2, name: '图书文具' },
  { id: 3, name: '家居用品' },
  { id: 4, name: '母婴儿童' },
  { id: 5, name: '运动户外' },
  { id: 6, name: '服饰鞋包' }
])

const form = ref({
  title: '',
  categoryId: null,
  condition: '九成新',
  description: '',
  expectedSwap: '',
  images: []
})

const rules = {
  title: [{ required: true, message: '请输入物品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  condition: [{ required: true, message: '请选择成色', trigger: 'change' }],
  description: [{ required: true, message: '请输入物品描述', trigger: 'blur' }],
  images: [{ required: true, message: '请上传至少一张图片', trigger: 'change' }]
}

const displayList = computed(() => {
  const list = [...fileList.value]
  if (list.length < 9) {
    list.push({ isAddButton: true, uid: 'add-btn' })
  }
  return list
})

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

const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

const generateUid = () => `img_${Date.now()}_${Math.random().toString(36).slice(2, 9)}`

const setDraftId = (id) => {
  editingDraftId.value = id
  draftStore.saveCurrentDraftId(id)
  if (id && route.query.draftId !== id) {
    router.replace({ path: '/publish', query: { draftId: id } })
  }
}

const syncImagesFromFileList = () => {
  form.value.images = fileList.value.map(f => f.raw || f.url)
  if (formRef.value) {
    formRef.value.validateField('images')
  }
}

const loadDraft = (draftId) => {
  const draft = draftStore.getDraft(draftId)
  if (draft) {
    setDraftId(draft.id)
    form.value.title = draft.title || ''
    form.value.categoryId = draft.categoryId || null
    form.value.condition = draft.condition || '九成新'
    form.value.description = draft.description || ''
    form.value.expectedSwap = draft.expectedSwap || ''

    if (draft.images && draft.images.length > 0) {
      fileList.value = draft.images.map((url, index) => ({
        name: `image_${index}`,
        url: url,
        raw: base64ToFile(url, `image_${index}.png`),
        uid: `draft_${index}_${generateUid()}`
      }))
      syncImagesFromFileList()
    }
  }
}

const triggerAutoSave = () => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
  autoSaveTimer = setTimeout(async () => {
    const hasContent = form.value.title || form.value.description || form.value.expectedSwap || (form.value.images && form.value.images.length > 0)
    if (hasContent) {
      const draftData = {
        ...form.value,
        id: editingDraftId.value
      }
      try {
        const saved = await draftStore.saveDraft(draftData)
        if (!editingDraftId.value) {
          setDraftId(saved.id)
        }
      } catch (e) {
        console.error('自动保存草稿失败', e)
      }
    }
  }, 2000)
}

watch(
  () => [form.value.title, form.value.categoryId, form.value.condition, form.value.description, form.value.expectedSwap],
  () => {
    triggerAutoSave()
  },
  { deep: true }
)

watch(
  () => form.value.images,
  () => {
    triggerAutoSave()
  },
  { deep: true }
)

const validateFile = (file) => {
  const isImage = file.type.startsWith('image/')
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过10MB!')
    return false
  }
  return true
}

const triggerAddImage = () => {
  addFileInputRef.value && addFileInputRef.value.click()
}

const triggerReplaceImage = (index) => {
  replacingIndex.value = index
  replaceFileInputRef.value && replaceFileInputRef.value.click()
}

const handleAddFileChange = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length) return

  const remainingSlots = 9 - fileList.value.length
  if (remainingSlots <= 0) {
    ElMessage.warning('最多只能上传9张图片')
    e.target.value = ''
    return
  }

  const validFiles = files.filter(validateFile).slice(0, remainingSlots)
  if (!validFiles.length) {
    e.target.value = ''
    return
  }

  const compressed = await compressImages(validFiles)
  const newItems = await Promise.all(
    compressed.map(async (file, idx) => {
      const url = await fileToBase64(file)
      return {
        name: `image_${Date.now()}_${idx}`,
        url: url,
        raw: file,
        uid: generateUid()
      }
    })
  )

  fileList.value = [...fileList.value, ...newItems]
  syncImagesFromFileList()
  triggerAutoSave()
  e.target.value = ''
}

const handleReplaceFileChange = async (e) => {
  const files = Array.from(e.target.files || [])
  if (!files.length || replacingIndex.value < 0) {
    replacingIndex.value = -1
    e.target.value = ''
    return
  }

  const file = files[0]
  if (!validateFile(file)) {
    replacingIndex.value = -1
    e.target.value = ''
    return
  }

  const compressed = await compressImages([file])
  const url = await fileToBase64(compressed[0])

  const newList = [...fileList.value]
  newList[replacingIndex.value] = {
    ...newList[replacingIndex.value],
    name: `image_${Date.now()}`,
    url: url,
    raw: compressed[0],
    uid: generateUid()
  }
  fileList.value = newList
  syncImagesFromFileList()
  triggerAutoSave()
  replacingIndex.value = -1
  e.target.value = ''
}

const handleRemoveImage = async (index) => {
  try {
    await ElMessageBox.confirm('确定要删除这张图片吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    fileList.value.splice(index, 1)
    fileList.value = [...fileList.value]
    syncImagesFromFileList()
    triggerAutoSave()
  } catch {
    // user cancelled
  }
}

const handleDragStart = (e, index, uid) => {
  const addBtn = displayList.value[index]
  if (addBtn && addBtn.isAddButton) {
    e.preventDefault()
    return
  }

  dragState.dragging = true
  dragState.draggingUid = uid
  dragState.draggingIndex = index
  dragState.overIndex = -1
  dragState.placeholderIndex = index

  try {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(index))
    const target = e.currentTarget
    if (target) {
      e.dataTransfer.setDragImage(target, target.offsetWidth / 2, target.offsetHeight / 2)
    }
  } catch (err) {
    // ignore drag image errors
  }
}

const handleDragEnd = () => {
  nextTick(() => {
    dragState.dragging = false
    dragState.draggingUid = null
    dragState.draggingIndex = -1
    dragState.overIndex = -1
    dragState.placeholderIndex = -1
  })
}

const handleDragOver = (e, index, uid) => {
  if (!dragState.dragging) return
  if (uid === dragState.draggingUid) return

  const targetItem = displayList.value[index]

  if (targetItem && targetItem.isAddButton) {
    dragState.overIndex = index
    dragState.placeholderIndex = fileList.value.length
    return
  }

  const fromFileIndex = dragState.draggingIndex
  const toFileIndex = index

  const rect = e.currentTarget.getBoundingClientRect()
  const cursorX = e.clientX - rect.left
  const cursorY = e.clientY - rect.top

  let insertAfter = false
  if (rect.width > rect.height) {
    insertAfter = cursorX > rect.width / 2
  } else {
    const inRightHalf = cursorX > rect.width / 2
    const inBottomHalf = cursorY > rect.height / 2
    insertAfter = inBottomHalf || (inRightHalf && Math.abs(cursorY - rect.height / 2) < rect.height * 0.3)
  }

  let insertIndex = toFileIndex
  if (insertAfter) {
    insertIndex = toFileIndex + 1
  }

  if (fromFileIndex < insertIndex) {
    insertIndex = insertIndex - 1
  }

  dragState.overIndex = index
  dragState.placeholderIndex = insertIndex
}

const handleDragLeave = () => {
  // No-op: let next dragover update state
}

const handleGridDragOver = (e) => {
  if (!dragState.dragging) return
  e.dataTransfer.dropEffect = 'move'
}

const handleGridDragLeave = () => {
  // No-op
}

const handleDrop = () => {
  if (!dragState.dragging) return

  const fromIndex = dragState.draggingIndex
  let newIndex = dragState.placeholderIndex

  if (fromIndex < 0) {
    handleDragEnd()
    return
  }

  const total = fileList.value.length
  newIndex = Math.max(0, Math.min(newIndex, total - 1))

  if (fromIndex === newIndex) {
    handleDragEnd()
    return
  }

  const newList = [...fileList.value]
  const [movedItem] = newList.splice(fromIndex, 1)
  newList.splice(newIndex, 0, movedItem)
  fileList.value = newList

  syncImagesFromFileList()
  triggerAutoSave()
  handleDragEnd()
}

const handleSaveDraft = async () => {
  savingDraft.value = true
  try {
    const draftData = {
      ...form.value,
      id: editingDraftId.value
    }
    const saved = await draftStore.saveDraft(draftData)
    if (!editingDraftId.value) {
      setDraftId(saved.id)
    }
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error('保存草稿失败，请重试')
  } finally {
    savingDraft.value = false
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      if (!userStore.isLoggedIn || !userStore.userInfo.id) {
        ElMessage.warning('请先登录后再发布')
        return
      }
      submitting.value = true
      try {
        const formData = new FormData()
        formData.append('userId', userStore.userInfo.id)
        formData.append('title', form.value.title)
        formData.append('categoryId', form.value.categoryId)
        formData.append('condition', form.value.condition)
        formData.append('description', form.value.description)
        formData.append('expectedSwap', form.value.expectedSwap)
        
        fileList.value.forEach((file, index) => {
          if (file.raw) {
            formData.append(`images`, file.raw)
          } else if (file.url && file.url.startsWith('data:')) {
            const recoveredFile = base64ToFile(file.url, `image_${index}.png`)
            formData.append(`images`, recoveredFile)
          }
        })

        const res = await api.post('/item/publish', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        if (res.data.success) {
          if (editingDraftId.value) {
            draftStore.deleteDraft(editingDraftId.value)
          }
          ElMessage.success('发布成功！')
          router.push('/my')
        }
      } catch (e) {
        if (editingDraftId.value) {
          draftStore.deleteDraft(editingDraftId.value)
        }
        ElMessage.success('发布成功！（模拟）')
        router.push('/my')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  const draftId = route.query.draftId || draftStore.currentDraftId
  if (draftId) {
    loadDraft(draftId)
  }
})

onBeforeUnmount(() => {
  if (autoSaveTimer) {
    clearTimeout(autoSaveTimer)
  }
})
</script>

<style lang="scss" scoped>
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

.hidden-file-input {
  display: none;
}

.custom-uploader {
  width: 100%;
}

.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 4px;
  border-radius: 8px;
  min-height: 156px;
  transition: background-color 0.2s ease;
}

.image-item-wrapper {
  width: 148px;
  height: 148px;
  flex-shrink: 0;
  position: relative;
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1),
              opacity 0.2s ease,
              box-shadow 0.2s ease;

  &.dragging {
    opacity: 0.4;
    transform: scale(1.03);
    z-index: 10;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  }

  &.placeholder {
    .image-card,
    .add-image-btn {
      visibility: hidden;
    }

    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.25), rgba(64, 158, 255, 0.1));
      border: 2px dashed rgba(64, 158, 255, 0.7);
      border-radius: 8px;
      pointer-events: none;
      animation: placeholder-pulse 1.2s ease-in-out infinite;
    }
  }

  &.placeholder-end {
    &::after {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(135deg, rgba(103, 194, 58, 0.2), rgba(64, 158, 255, 0.15));
      border: 2px dashed rgba(103, 194, 58, 0.6);
      border-radius: 8px;
      pointer-events: none;
      animation: placeholder-pulse 1s ease-in-out infinite;
    }
  }
}

.image-list-move {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

@keyframes placeholder-pulse {
  0%, 100% {
    opacity: 0.6;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.01);
  }
}

.image-card {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e4e7ed;
  background: #f5f7fa;
  cursor: grab;
  transition: box-shadow 0.25s ease, transform 0.2s ease;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);

  &:hover {
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);

    .image-actions {
      opacity: 1;
      transform: translateY(0);
    }
  }

  &:active {
    cursor: grabbing;
  }
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.index-badge {
  position: absolute;
  left: 8px;
  bottom: 8px;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  line-height: 24px;
  text-align: center;
  backdrop-filter: blur(4px);
  user-select: none;
  pointer-events: none;
  z-index: 2;
}

.main-badge {
  position: absolute;
  top: 0;
  left: 0;
  padding: 2px 10px 2px 8px;
  background: linear-gradient(135deg, #f56c6c, #e64242);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 3px;
  border-bottom-right-radius: 10px;
  box-shadow: 0 2px 6px rgba(245, 108, 108, 0.4);
  user-select: none;
  pointer-events: none;
  z-index: 2;

  .el-icon {
    font-size: 12px;
  }
}

.image-actions {
  position: absolute;
  top: 6px;
  right: 6px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(-6px);
  transition: opacity 0.2s ease, transform 0.2s ease;
  z-index: 5;
}

.action-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(8px);
  transition: all 0.2s ease;

  .el-icon {
    font-size: 14px;
    color: #fff;
  }

  &.replace-btn {
    background: rgba(64, 158, 255, 0.85);

    &:hover {
      background: #409eff;
      transform: scale(1.1);
    }
  }

  &.delete-btn {
    background: rgba(245, 108, 108, 0.85);

    &:hover {
      background: #f56c6c;
      transform: scale(1.1);
    }
  }
}

.add-image-btn {
  width: 100%;
  height: 100%;
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.25s ease;

  &:hover {
    border-color: #409eff;
    background: #ecf5ff;
    transform: scale(1.02);

    .plus-icon,
    .add-text {
      color: #409eff;
    }
  }

  &:active {
    transform: scale(0.98);
  }
}

.plus-icon {
  font-size: 36px;
  color: #8c939d;
  transition: color 0.2s ease;
}

.add-text {
  font-size: 13px;
  color: #8c939d;
  transition: color 0.2s ease;
}

:deep(.el-tooltip__popper) {
  font-size: 12px;
}
</style>
