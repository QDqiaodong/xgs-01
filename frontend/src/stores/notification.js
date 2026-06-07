import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/utils/api'
import { useUserStore } from '@/stores/user'

export const NOTIFICATION_TYPES = {
  NEW_OFFER: 'new_offer',
  OFFER_ACCEPTED: 'offer_accepted',
  OFFER_REJECTED: 'offer_rejected'
}

export const NOTIFICATION_TYPE_TEXT = {
  [NOTIFICATION_TYPES.NEW_OFFER]: '新邀约到达',
  [NOTIFICATION_TYPES.OFFER_ACCEPTED]: '邀约被接受',
  [NOTIFICATION_TYPES.OFFER_REJECTED]: '邀约被驳回'
}

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const loading = ref(false)

  const userStore = useUserStore()

  const unreadCount = computed(() =>
    notifications.value.filter(n => !n.readFlag).length
  )

  const unreadNotifications = computed(() =>
    notifications.value.filter(n => !n.readFlag)
  )

  const loadNotifications = async () => {
    if (!userStore.isLoggedIn) {
      notifications.value = []
      return
    }
    loading.value = true
    try {
      const res = await api.get('/notification/list', {
        params: { userId: userStore.userInfo.id }
      })
      if (res.data.success) {
        notifications.value = res.data.data.map(n => ({
          ...n,
          createTime: formatTime(n.createTime)
        }))
      }
    } catch (e) {
      notifications.value = [
        {
          id: 1,
          type: NOTIFICATION_TYPES.NEW_OFFER,
          title: '您收到一条新的互换邀约',
          content: '邻居小王 想用「Kindle 电子书阅读器」换您的「小米空气净化器Pro H」',
          offerId: 1,
          itemId: 1,
          readFlag: false,
          createTime: '2024-01-20 14:30'
        },
        {
          id: 2,
          type: NOTIFICATION_TYPES.OFFER_ACCEPTED,
          title: '邀约已被接受',
          content: '您发出的「用 Switch 换 iPad」邀约已被对方接受',
          offerId: 2,
          itemId: 2,
          readFlag: false,
          createTime: '2024-01-19 10:15'
        },
        {
          id: 3,
          type: NOTIFICATION_TYPES.OFFER_REJECTED,
          title: '邀约被驳回',
          content: '您发出的「用相机换镜头」邀约已被对方驳回',
          offerId: 3,
          itemId: 3,
          readFlag: true,
          createTime: '2024-01-18 16:45'
        }
      ]
    } finally {
      loading.value = false
    }
  }

  const markAsRead = async (id) => {
    const notification = notifications.value.find(n => n.id === id)
    if (notification && !notification.readFlag) {
      try {
        await api.post(`/notification/read/${id}`, null, {
          params: { userId: userStore.userInfo.id }
        })
      } catch (e) {}
      notification.readFlag = true
    }
  }

  const markAllAsRead = async () => {
    try {
      await api.post('/notification/read-all', null, {
        params: { userId: userStore.userInfo.id }
      })
    } catch (e) {}
    notifications.value.forEach(n => {
      n.readFlag = true
    })
  }

  const addNotification = (notification) => {
    notifications.value.unshift({
      ...notification,
      readFlag: false,
      createTime: formatTime(notification.createTime) || formatTime(new Date().toISOString())
    })
  }

  const clear = () => {
    notifications.value = []
  }

  return {
    notifications,
    loading,
    unreadCount,
    unreadNotifications,
    loadNotifications,
    markAsRead,
    markAllAsRead,
    addNotification,
    clear
  }
})
