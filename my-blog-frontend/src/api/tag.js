import request from '@/utils/request'

/** 标签云：标签 + 已发布文章数，按数量倒序 */
export function listTags() {
  return request.get('/tags')
}
