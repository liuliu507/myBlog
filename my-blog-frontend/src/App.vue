<template>
  <el-container class="app-container" direction="vertical">
    <AppHeader />
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import AppHeader from '@/components/layout/AppHeader.vue'

const userStore = useUserStore()

onMounted(() => {
  // 刷新后如果有 token 但没用户信息，重新拉一次
  if (userStore.isLoggedIn && !userStore.userInfo) {
    userStore.fetchMe().catch(() => {
      userStore.logout()
    })
  }
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: var(--page-bg);
}

@media (max-width: 767px) {
  .app-main {
    padding: 12px;
  }
}
</style>
