import { defineStore } from 'pinia'
import { login as loginApi, getMe, updateAvatar as updateAvatarApi } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
  },

  actions: {
    async login(data) {
      const res = await loginApi(data)
      this.token = res.token
      this.userInfo = res.user
      localStorage.setItem('token', res.token)
    },

    async fetchMe() {
      const info = await getMe()
      this.userInfo = info
    },

    async updateAvatar(avatarUrl) {
      const info = await updateAvatarApi(avatarUrl)
      this.userInfo = info
    },

    logout() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
    },
  },
})