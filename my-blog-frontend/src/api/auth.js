import request from '@/utils/request'

export function register(data) {
  return request.post('/auth/register', data)
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function getMe() {
  return request.get('/auth/me')
}

export function sendResetCode(data) {
  return request.post('/auth/send-reset-code', data)
}

export function resetPassword(data) {
  return request.post('/auth/reset-password', data)
}