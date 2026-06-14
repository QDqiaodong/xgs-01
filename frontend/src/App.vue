<template>
  <div id="app">
    <el-container class="app-container">
      <el-header class="app-header">
        <div class="header-content">
          <div class="logo" @click="$router.push('/')">
            <el-icon size="32"><Goods /></el-icon>
            <span>闲置好物置换市集</span>
          </div>
          <nav class="nav-menu">
            <el-menu :default-active="activeMenu" mode="horizontal" @select="handleMenuSelect">
              <el-menu-item index="/">首页</el-menu-item>
              <el-menu-item index="/market">物品市集</el-menu-item>
              <el-menu-item index="/ranking">点赞排行</el-menu-item>
              <el-menu-item index="/reports">举报管理</el-menu-item>
              <el-menu-item index="/publish">发布闲置</el-menu-item>
              <el-menu-item index="/my">我的库房</el-menu-item>
            </el-menu>
          </nav>
          <div class="header-right">
            <el-popover
              v-if="userStore.isLoggedIn"
              placement="bottom-end"
              :width="380"
              trigger="click"
              popper-class="notification-popover"
              @show="handlePopoverShow"
            >
              <template #reference>
                <div class="notification-bell">
                  <el-icon :size="22"><Bell /></el-icon>
                  <el-badge
                    v-if="notificationStore.unreadCount > 0"
                    :value="notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount"
                    :max="99"
                    class="bell-badge"
                  />
                </div>
              </template>

              <div class="notification-panel">
                <div class="notification-header">
                  <span class="notification-title">消息通知</span>
                  <el-button
                    v-if="notificationStore.unreadCount > 0"
                    type="text"
                    size="small"
                    @click="handleMarkAllRead"
                  >
                    全部标记已读
                  </el-button>
                </div>

                <div class="notification-list" v-loading="notificationStore.loading">
                  <div
                    v-for="item in notificationStore.notifications"
                    :key="item.id"
                    class="notification-item"
                    :class="{ unread: !item.readFlag }"
                  >
                    <div
                      class="notification-icon-wrap"
                      :class="getIconClass(item.type)"
                    >
                      <el-icon :size="18">
                        <component :is="getIconName(item.type)" />
                      </el-icon>
                    </div>
                    <div class="notification-content" @click="handleNotificationClick(item)">
                      <div class="notification-top">
                        <span class="notification-item-title">{{ item.title }}</span>
                        <span class="notification-time">{{ item.createTime }}</span>
                      </div>
                      <div class="notification-item-content">{{ item.content }}</div>
                    </div>
                    <div class="notification-actions">
                      <el-button
                        v-if="!item.readFlag"
                        type="text"
                        size="small"
                        @click.stop="handleMarkRead(item.id)"
                      >
                        标记已读
                      </el-button>
                      <span v-else class="read-mark">已读</span>
                    </div>
                    <span v-if="!item.readFlag" class="unread-dot" />
                  </div>
                  <el-empty
                    v-if="notificationStore.notifications.length === 0 && !notificationStore.loading"
                    description="暂无消息"
                    :image-size="60"
                  />
                </div>
              </div>
            </el-popover>

            <div class="user-area">
              <el-button type="primary" @click="showLogin = true" v-if="!userStore.isLoggedIn">
                登录
              </el-button>
              <el-dropdown v-else @command="handleUserCommand">
                <span class="user-name">
                  <el-icon><User /></el-icon>
                  {{ userStore.userInfo.nickname || '用户' }}
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                    <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
        </div>
      </el-header>
      <el-main class="app-main">
        <router-view />
      </el-main>
      <el-footer class="app-footer">
        <p>闲置好物置换市集 - 让闲置流动起来</p>
      </el-footer>
    </el-container>

    <el-dialog v-model="showLogin" title="用户登录" width="400px">
      <el-form :model="loginForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLogin = false">取消</el-button>
        <el-button type="primary" @click="handleLogin">登录</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useFavoriteStore } from '@/stores/favorite'
import { useLikeStore } from '@/stores/like'
import { useNotificationStore, NOTIFICATION_TYPES } from '@/stores/notification'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const favoriteStore = useFavoriteStore()
const likeStore = useLikeStore()
const notificationStore = useNotificationStore()

const showLogin = ref(false)
const loginForm = ref({
  username: '',
  password: ''
})

const activeMenu = computed(() => route.path)

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  const success = await userStore.login(loginForm.value)
  if (success) {
    showLogin.value = false
    ElMessage.success('登录成功')
    favoriteStore.loadFavorites()
    likeStore.clearLikes()
    notificationStore.loadNotifications()
  } else {
    ElMessage.error('登录失败，用户名或密码错误')
  }
}

const handleUserCommand = (command) => {
  if (command === 'logout') {
    userStore.logout()
    favoriteStore.loadFavorites()
    likeStore.clearLikes()
    notificationStore.clear()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (command === 'profile') {
    router.push('/my')
  }
}

const getIconName = (type) => {
  const map = {
    [NOTIFICATION_TYPES.NEW_OFFER]: 'Suitcase',
    [NOTIFICATION_TYPES.OFFER_ACCEPTED]: 'CircleCheck',
    [NOTIFICATION_TYPES.OFFER_REJECTED]: 'CircleClose'
  }
  return map[type] || 'Bell'
}

const getIconClass = (type) => {
  const map = {
    [NOTIFICATION_TYPES.NEW_OFFER]: 'icon-new',
    [NOTIFICATION_TYPES.OFFER_ACCEPTED]: 'icon-accepted',
    [NOTIFICATION_TYPES.OFFER_REJECTED]: 'icon-rejected'
  }
  return map[type] || ''
}

const handlePopoverShow = () => {
  notificationStore.loadNotifications()
}

const handleMarkRead = async (id) => {
  try {
    await notificationStore.markAsRead(id)
  } catch (e) {
    ElMessage.error('标记已读失败，请稍后重试')
  }
}

const handleMarkAllRead = async () => {
  try {
    await notificationStore.markAllAsRead()
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    ElMessage.error('标记已读失败，请稍后重试')
  }
}

const handleNotificationClick = async (item) => {
  if (!item.readFlag) {
    try {
      await notificationStore.markAsRead(item.id)
    } catch (e) {}
  }
  if (item.offerId) {
    router.push(`/offer/${item.offerId}`)
  } else {
    router.push('/offers')
  }
}

watch(
  () => userStore.isLoggedIn,
  (isLoggedIn) => {
    if (isLoggedIn) {
      notificationStore.loadNotifications()
    } else {
      notificationStore.clear()
    }
  }
)

onMounted(() => {
  if (userStore.isLoggedIn) {
    favoriteStore.loadFavorites()
    notificationStore.loadNotifications()
  }
})
</script>

<style lang="scss">
#app {
  min-height: 100vh;
}

.app-container {
  min-height: 100vh;
}

.app-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 0;
  height: 64px;
  line-height: 64px;

  .header-content {
    max-width: 1200px;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;
  }

  .logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: white;
    font-size: 20px;
    font-weight: bold;
    cursor: pointer;
  }

  .nav-menu {
    :deep(.el-menu) {
      background: transparent;
      border-bottom: none;

      .el-menu-item {
        color: rgba(255, 255, 255, 0.9);

        &:hover,
        &.is-active {
          color: white;
          background: rgba(255, 255, 255, 0.2);
        }
      }
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  .notification-bell {
    color: white;
    cursor: pointer;
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    transition: background 0.3s;

    &:hover {
      background: rgba(255, 255, 255, 0.2);
    }

    .bell-badge {
      :deep(.el-badge__content) {
        border: none;
      }
    }
  }

  .user-area {
    .user-name {
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 5px;
    }
  }
}

.notification-popover {
  padding: 0 !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15) !important;

  .notification-panel {
    .notification-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      border-bottom: 1px solid #ebeef5;

      .notification-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .notification-list {
      max-height: 420px;
      overflow-y: auto;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-thumb {
        background: #dcdfe6;
        border-radius: 3px;
      }
    }

    .notification-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 18px;
      border-bottom: 1px solid #f2f6fc;
      position: relative;
      transition: background 0.2s;

      &:hover {
        background: #fafbfc;
      }

      &:last-child {
        border-bottom: none;
      }

      &.unread {
        background: #f0f9ff;

        &:hover {
          background: #e6f4ff;
        }
      }

      .notification-icon-wrap {
        flex-shrink: 0;
        width: 36px;
        height: 36px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: white;

        &.icon-new {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }

        &.icon-accepted {
          background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
        }

        &.icon-rejected {
          background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%);
        }
      }

      .notification-content {
        flex: 1;
        min-width: 0;
        cursor: pointer;

        .notification-top {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 4px;

          .notification-item-title {
            font-size: 14px;
            font-weight: 500;
            color: #303133;
          }

          .notification-time {
            font-size: 12px;
            color: #909399;
            flex-shrink: 0;
            margin-left: 8px;
          }
        }

        .notification-item-content {
          font-size: 13px;
          color: #606266;
          line-height: 1.5;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
      }

      .notification-actions {
        flex-shrink: 0;
        padding-left: 8px;

        .read-mark {
          font-size: 12px;
          color: #c0c4cc;
        }
      }

      .unread-dot {
        position: absolute;
        top: 18px;
        right: 14px;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: #f56c6c;
      }
    }
  }
}

.app-main {
  background: #f5f7fa;
  padding: 20px;
}

.app-footer {
  text-align: center;
  padding: 20px;
  background: #fff;
  color: #999;
  font-size: 14px;
}
</style>
// test cache
