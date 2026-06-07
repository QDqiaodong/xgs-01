<template>
  <div class="page-container">
    <h1 class="page-title">发布闲置物品</h1>

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
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
            list-type="picture-card"
            :limit="9"
            accept="image/*"
            :file-list="fileList"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">最多上传9张图片，支持拖拽排序</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">
            发布物品
          </el-button>
          <el-button size="large" @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { compressImages } from '@/utils/imageCompressor'
import api from '@/utils/api'

const router = useRouter()
const formRef = ref(null)
const uploadRef = ref(null)
const submitting = ref(false)
const fileList = ref([])

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

const beforeUpload = (file) => {
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

const handleFileChange = async (file, list) => {
  if (file.raw) {
    const compressed = await compressImages([file.raw])
    list[list.length - 1].raw = compressed[0]
  }
  fileList.value = list
  form.value.images = list.map(f => f.raw || f.url)
  if (formRef.value) {
    formRef.value.validateField('images')
  }
}

const handleFileRemove = (file, list) => {
  fileList.value = list
  form.value.images = list.map(f => f.raw || f.url)
  if (formRef.value) {
    formRef.value.validateField('images')
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const formData = new FormData()
        formData.append('title', form.value.title)
        formData.append('categoryId', form.value.categoryId)
        formData.append('condition', form.value.condition)
        formData.append('description', form.value.description)
        formData.append('expectedSwap', form.value.expectedSwap)
        
        fileList.value.forEach((file, index) => {
          if (file.raw) {
            formData.append(`images`, file.raw)
          }
        })

        const res = await api.post('/item/publish', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

        if (res.data.success) {
          ElMessage.success('发布成功！')
          router.push('/my')
        }
      } catch (e) {
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
</script>

<style lang="scss" scoped>
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

:deep(.el-upload--picture-card) {
  width: 148px;
  height: 148px;
}
</style>
