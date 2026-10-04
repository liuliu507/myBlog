<template>
  <el-header class="app-header">
    <div class="logo" @click="$router.push('/')">我的博客</div>
    <div class="nav">
      <el-button text @click="$router.push('/')">首页</el-button>
      <template v-if="userStore.isLoggedIn">
        <el-button text @click="$router.push('/mine')">我的文章</el-button>
        <el-button text @click="$router.push('/categories')">分类</el-button>
        <el-button type="primary" @click="$router.push('/editor')">写文章</el-button>
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

const router = useRouter()
const userStore = useUserStore()

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
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  color: #409eff;
}
.nav {
  display: flex;
  align-items: center;
  gap: 8px;
}
.user-name {
  cursor: pointer;
  margin-left: 8px;
  color: #333;
}

@media (max-width: 767px) {
  .app-header {
    padding: 0 12px;
  }
  .logo {
    font-size: 18px;
    white-space: nowrap;
    flex-shrink: 0;
  }
  .nav {
    gap: 2px;
  }
  .nav :deep(.el-button) {
    padding-left: 6px;
    padding-right: 6px;
  }
  .user-name {
    display: inline-block;
    margin-left: 4px;
    max-width: 72px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
  }
}
</style>
