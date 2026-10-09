<template>
  <el-container class="app-container" direction="vertical">
    <AppHeader />
    <el-main class="app-main">
      <router-view />
    </el-main>
    <AiChatDrawer />
  </el-container>
</template>

<script setup>
import { onMounted, watch } from 'vue'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'
import AppHeader from '@/components/layout/AppHeader.vue'
import AiChatDrawer from '@/components/ai/AiChatDrawer.vue'

const userStore = useUserStore()
const aiStore = useAiStore()

// 用户切换或登出时清空 AI 对话，避免下一个登录用户看到上一个用户的记录
// 覆盖手动登出（AppHeader）与 401 自动登出（request 拦截器）两条路径
watch(
  () => userStore.userInfo?.id,
  (newId, oldId) => {
    if (oldId && newId !== oldId) {
      aiStore.clearMessages()
      aiStore.clearContext()
      aiStore.closeDrawer()
    }
  }
)

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
