import request from '@/utils/request'

/** 查文章评论树（公开） */
export function listComments(articleId) {
  return request.get('/comments', { params: { articleId } })
}

/** 发表评论/回复（登录） */
export function createComment(data) {
  return request.post('/comments', data)
}

/** 删除评论 */
export function deleteComment(id) {
  return request.delete(`/comments/${id}`)
}

/** 点赞/取消点赞切换（登录） */
export function toggleLike(articleId) {
  return request.post(`/articles/${articleId}/like`)
}

/** 记录浏览（公开，前端进入文章页时调用一次） */
export function recordView(articleId) {
  return request.post(`/articles/${articleId}/view`)
}
