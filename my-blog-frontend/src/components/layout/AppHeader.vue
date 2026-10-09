<template>
  <el-header class="app-header">
    <div class="logo" @click="$router.push('/')">我的博客</div>
    <div class="nav">
      <el-button text @click="$router.push('/')">首页</el-button>
      <template v-if="userStore.isLoggedIn">
        <el-button text @click="$router.push('/mine')">我的文章</el-button>
        <el-button text @click="$router.push('/categories')">分类</el-button>
        <el-button type="primary" @click="$router.push('/editor')">写文章</el-button>
        <el-button text @click="aiStore.openDrawer()">AI 助手</el-button>
        <el-dropdown @command="handleCommand">
          <span class="user-name">{{ userStore.userInfo?.nickname || '我' }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <template v-else>
        <el-button text @click="$router.push('/login')">登录</el-button>
        <el-button @click="$router.push('/register')">注册</el-button>
      </template>
    </div>
  </el-header>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'

const router = useRouter()
const userStore = useUserStore()
const aiStore = useAiStore()

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 68px;
  padding: 0 max(24px, calc((100vw - var(--content-width)) / 2));
  background: rgb(255 255 255 / 94%);
  border-bottom: 1px solid var(--line);
  backdrop-filter: blur(12px);
}
.logo {
  position: relative;
  padding-left: 18px;
  font-size: 19px;
  font-weight: 750;
  cursor: pointer;
  color: var(--ink);
  white-space: nowrap;
}
.logo::before {
  position: absolute;
  top: 50%;
  left: 0;
  width: 9px;
  height: 20px;
  border-radius: 3px;
  background: var(--accent);
  content: '';
  transform: translateY(-50%);
}
.nav {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.user-name {
  cursor: pointer;
  margin-left: 8px;
  color: var(--ink);
  font-size: 14px;
}

@media (max-width: 767px) {
  .app-header {
    height: 60px;
    padding: 0 14px;
  }
  .logo {
    font-size: 16px;
    flex-shrink: 0;
  }
  .nav {
    gap: 0;
    max-width: 70%;
    overflow-x: auto;
    scrollbar-width: none;
  }
  .nav::-webkit-scrollbar {
    display: none;
  }
  .nav :deep(.el-button) {
    flex-shrink: 0;
    padding: 8px 7px;
    font-size: 13px;
  }
  .user-name {
    display: inline-block;
    margin-left: 2px;
    max-width: 60px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
  }
}
</style>
