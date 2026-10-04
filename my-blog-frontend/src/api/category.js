import request from '@/utils/request'

export function listCategories() {
  return request.get('/categories')
}

// 个人专栏公开详情（无需登录）
export function getCategory(id) {
  return request.get(`/categories/${id}`)
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}