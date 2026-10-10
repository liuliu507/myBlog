import { defineStore } from 'pinia'
import { getUnreadCount } from '@/api/notification'

/** 未读通知数的全局状态：AppHeader 铃铛与通知页共用，避免跨组件同步 */
export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
  }),

  actions: {
    async refresh() {
      try {
        const data = await getUnreadCount()
        this.unreadCount = data.count
      } catch {
        // 静默失败：未读数拉取不影响页面主体
      }
    },

    clear() {
      this.unreadCount = 0
    },
  },
})
