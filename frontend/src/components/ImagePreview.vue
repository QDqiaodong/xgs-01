<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="image-preview-mask" @click.self="handleClose">
        <div class="preview-header">
          <div class="preview-index">{{ currentIndex + 1 }} / {{ images.length }}</div>
          <button class="close-btn" @click="handleClose">
            <el-icon :size="24"><Close /></el-icon>
          </button>
        </div>

        <div 
          class="preview-container"
          ref="containerRef"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @dblclick="handleDoubleClick"
        >
          <div 
            class="image-wrapper"
            :style="wrapperStyle"
          >
            <img 
              :src="currentImage" 
              class="preview-image"
              :style="imageStyle"
              @load="handleImageLoad"
              draggable="false"
            />
          </div>
        </div>

        <div class="preview-footer">
          <button class="action-btn" @click="handlePrev">
            <el-icon :size="20"><ArrowLeft /></el-icon>
          </button>
          <button class="action-btn download-btn" @click="handleDownload">
            <el-icon :size="20"><Download /></el-icon>
            <span>下载</span>
          </button>
          <button class="action-btn" @click="handleNext">
            <el-icon :size="20"><ArrowRight /></el-icon>
          </button>
        </div>

        <div class="thumbnails-wrapper" v-if="images.length > 1">
          <div class="thumbnails-container">
            <div 
              v-for="(img, index) in images" 
              :key="index"
              class="thumbnail-item"
              :class="{ active: index === currentIndex }"
              @click="handleThumbnailClick(index)"
            >
              <img :src="img" />
            </div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { Close, ArrowLeft, ArrowRight, Download } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  images: {
    type: Array,
    default: () => []
  },
  initialIndex: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['update:visible', 'close'])

const containerRef = ref(null)
const currentIndex = ref(0)
const scale = ref(1)
const translateX = ref(0)
const translateY = ref(0)
const imageLoaded = ref(false)

const startX = ref(0)
const startY = ref(0)
const startTranslateX = ref(0)
const startTranslateY = ref(0)
const startDistance = ref(0)
const startScale = ref(1)
const isDragging = ref(false)
const isPinching = ref(false)
const lastTapTime = ref(0)

const MIN_SCALE = 1
const MAX_SCALE = 3
const SWIPE_THRESHOLD = 50

const currentImage = computed(() => {
  if (props.images.length === 0) return ''
  const idx = Math.max(0, Math.min(currentIndex.value, props.images.length - 1))
  return props.images[idx]
})

const wrapperStyle = computed(() => ({
  transform: `translate(${translateX.value}px, ${translateY.value}px) scale(${scale.value})`,
  transition: isDragging.value || isPinching.value ? 'none' : 'transform 0.3s ease-out'
}))

const imageStyle = computed(() => ({
  opacity: imageLoaded.value ? 1 : 0
}))

const resetTransform = () => {
  scale.value = 1
  translateX.value = 0
  translateY.value = 0
}

const handleClose = () => {
  emit('update:visible', false)
  emit('close')
  resetTransform()
}

const handlePrev = () => {
  if (currentIndex.value > 0) {
    currentIndex.value--
    resetTransform()
    imageLoaded.value = false
  }
}

const handleNext = () => {
  if (currentIndex.value < props.images.length - 1) {
    currentIndex.value++
    resetTransform()
    imageLoaded.value = false
  }
}

const handleThumbnailClick = (index) => {
  if (index !== currentIndex.value) {
    currentIndex.value = index
    resetTransform()
    imageLoaded.value = false
  }
}

const handleImageLoad = () => {
  imageLoaded.value = true
}

const getDistance = (touches) => {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.sqrt(dx * dx + dy * dy)
}

const handleTouchStart = (e) => {
  if (e.touches.length === 1) {
    const now = Date.now()
    if (now - lastTapTime.value < 300) {
      handleDoubleClick()
      lastTapTime.value = 0
      return
    }
    lastTapTime.value = now

    startX.value = e.touches[0].clientX
    startY.value = e.touches[0].clientY
    startTranslateX.value = translateX.value
    startTranslateY.value = translateY.value
    isDragging.value = true
    isPinching.value = false
  } else if (e.touches.length === 2) {
    isDragging.value = false
    isPinching.value = true
    startDistance.value = getDistance(e.touches)
    startScale.value = scale.value
  }
}

const handleTouchMove = (e) => {
  if (isPinching.value && e.touches.length === 2) {
    e.preventDefault()
    const currentDistance = getDistance(e.touches)
    const ratio = currentDistance / startDistance.value
    let newScale = startScale.value * ratio
    newScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale))
    scale.value = newScale
  } else if (isDragging.value && e.touches.length === 1) {
    const deltaX = e.touches[0].clientX - startX.value
    const deltaY = e.touches[0].clientY - startY.value

    if (scale.value > 1) {
      translateX.value = startTranslateX.value + deltaX
      translateY.value = startTranslateY.value + deltaY
    }
  }
}

const handleTouchEnd = (e) => {
  if (isPinching.value) {
    isPinching.value = false
    if (scale.value < MIN_SCALE) {
      scale.value = MIN_SCALE
    }
    if (scale.value <= MIN_SCALE) {
      resetTransform()
    }
  }

  if (isDragging.value) {
    isDragging.value = false
    const deltaX = e.changedTouches[0].clientX - startX.value
    const deltaY = e.changedTouches[0].clientY - startY.value

    if (scale.value <= 1 && Math.abs(deltaY) < Math.abs(deltaX)) {
      if (deltaX > SWIPE_THRESHOLD && currentIndex.value > 0) {
        handlePrev()
        return
      } else if (deltaX < -SWIPE_THRESHOLD && currentIndex.value < props.images.length - 1) {
        handleNext()
        return
      }
    }

    if (scale.value <= 1) {
      resetTransform()
    }
  }
}

const handleDoubleClick = () => {
  if (scale.value > 1) {
    resetTransform()
  } else {
    scale.value = 2
    translateX.value = 0
    translateY.value = 0
  }
}

const handleDownload = async () => {
  if (!currentImage.value) return

  try {
    const response = await fetch(currentImage.value, { mode: 'cors' })
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `image-${currentIndex.value + 1}.jpg`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (e) {
    const link = document.createElement('a')
    link.href = currentImage.value
    link.target = '_blank'
    link.download = `image-${currentIndex.value + 1}.jpg`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}

watch(() => props.visible, (val) => {
  if (val) {
    currentIndex.value = props.initialIndex
    resetTransform()
    imageLoaded.value = false
    nextTick(() => {
      if (containerRef.value) {
        document.body.style.overflow = 'hidden'
      }
    })
  } else {
    document.body.style.overflow = ''
  }
})

watch(() => props.initialIndex, (val) => {
  if (props.visible) {
    currentIndex.value = val
    resetTransform()
    imageLoaded.value = false
  }
})
</script>

<style lang="scss" scoped>
.image-preview-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: #000;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  user-select: none;
  -webkit-user-select: none;
}

.preview-header {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.6), transparent);
  z-index: 10;
}

.preview-index {
  color: #fff;
  font-size: 16px;
  font-weight: 500;
}

.close-btn {
  background: transparent;
  border: none;
  color: #fff;
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
  }
}

.preview-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  touch-action: none;
  -webkit-touch-callout: none;
}

.image-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  transform-origin: center center;
  will-change: transform;
}

.preview-image {
  max-width: 100vw;
  max-height: 100vh;
  object-fit: contain;
  pointer-events: none;
  transition: opacity 0.3s;
}

.preview-footer {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 20px;
  z-index: 10;
}

.action-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  backdrop-filter: blur(10px);

  &:hover {
    background: rgba(255, 255, 255, 0.25);
  }

  &:active {
    transform: scale(0.95);
  }

  &.download-btn {
    width: auto;
    padding: 0 20px;
    border-radius: 22px;
    gap: 8px;
    font-size: 14px;
  }
}

.thumbnails-wrapper {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  padding: 0 20px;
  z-index: 10;
}

.thumbnails-container {
  display: flex;
  gap: 8px;
  justify-content: center;
  overflow-x: auto;
  padding: 8px 0;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.thumbnail-item {
  width: 60px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  flex-shrink: 0;
  opacity: 0.6;
  transition: all 0.2s;

  &.active {
    border-color: #fff;
    opacity: 1;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .preview-footer {
    bottom: 90px;
    gap: 16px;
  }

  .action-btn {
    width: 40px;
    height: 40px;

    &.download-btn {
      padding: 0 16px;
      font-size: 13px;
    }
  }

  .thumbnail-item {
    width: 50px;
    height: 50px;
  }

  .thumbnails-wrapper {
    bottom: 16px;
  }
}
</style>
