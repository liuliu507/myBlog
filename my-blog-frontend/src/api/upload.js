import request from '@/utils/request'

/** 上传头像图片 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}

/** 上传文章内嵌图片 */
export function uploadArticleImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/article-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000,
  })
}
