import request from '@/utils/request'

/** 我的通知列表（登录，分页） */
export function listNotifications(params) {
  return request.get('/notifications', { params })
}

/** 未读通知数（铃铛徽标轮询用） */
export function getUnreadCount() {
  return request.get('/notifications/unread-count')
}

/** 全部标记已读 */
export function markAllRead() {
  return request.post('/notifications/read-all')
}
