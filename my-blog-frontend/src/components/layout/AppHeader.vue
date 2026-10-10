<template>
  <el-header class="app-header">
    <div class="logo" @click="$router.push('/')">我的博客</div>
    <div class="nav">
      <el-button text @click="$router.push('/')">首页</el-button>
      <el-button text @click="$router.push('/tags')">标签</el-button>
      <!-- 搜索：桌面端内联输入框 -->
      <div class="header-search">
        <el-button text class="search-toggle" aria-label="搜索" @click="mobileSearchOpen = true">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.2-3.2" />
          </svg>
        </el-button>
        <el-input
          v-model="keyword"
          class="search-input"
          placeholder="搜索文章…"
          clearable
          @keyup.enter="doSearch"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <path d="m20 20-3.2-3.2" />
            </svg>
          </template>
        </el-input>
      </div>
      <template v-if="userStore.isLoggedIn">
        <!-- 消息通知铃铛：30s 轮询未读数 -->
        <el-badge
          :value="notifStore.unreadCount"
          :hidden="notifStore.unreadCount === 0"
          :max="99"
          class="bell-badge"
        >
          <el-button text class="bell-btn" aria-label="消息通知" @click="$router.push('/notifications')">
            <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 9a6 6 0 1 0-12 0c0 6-2.5 7.5-2.5 7.5h17S18 15 18 9" />
              <path d="M10 20a2.2 2.2 0 0 0 4 0" />
            </svg>
          </el-button>
        </el-badge>
        <el-button text @click="$router.push('/mine')">我的文章</el-button>
        <el-button text @click="$router.push('/categories')">分类</el-button>
        <el-button type="primary" @click="$router.push('/editor')">写文章</el-button>
        <el-button text @click="aiStore.openDrawer()">AI 助手</el-button>
        <el-dropdown @command="handleCommand">
          <span class="user-trigger" v-loading="uploading">
            <el-avatar :size="32" :src="userStore.userInfo?.avatar">
              {{ (userStore.userInfo?.nickname || '我')[0] }}
            </el-avatar>
            <span class="user-name">{{ userStore.userInfo?.nickname || '我' }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="avatar">更换头像</el-dropdown-item>
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
    <!-- 手机端展开的搜索条：吸顶 header 正下方整条展开，不占导航横向空间 -->
    <div v-if="mobileSearchOpen" class="mobile-search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索文章…"
        clearable
        autofocus
        @keyup.enter="doSearch"
      />
      <el-button text @click="mobileSearchOpen = false">取消</el-button>
    </div>
    <!-- 隐藏的文件选择器，点击"更换头像"时触发 -->
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      hidden
      @change="onFileChange"
    />
  </el-header>
</template>

<script setup>
import { ref, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'
import { useNotificationStore } from '@/stores/notification'
import { uploadAvatar } from '@/api/upload'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const aiStore = useAiStore()
const notifStore = useNotificationStore()
const fileInput = ref(null)
const uploading = ref(false)
const keyword = ref('')
const mobileSearchOpen = ref(false)

// 未读通知数轮询：登录后立即拉一次并每 30s 刷新，登出时清零停表
let pollTimer = null
watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
    if (loggedIn) {
      notifStore.refresh()
      pollTimer = setInterval(() => notifStore.refresh(), 30000)
    } else {
      notifStore.clear()
    }
  },
  { immediate: true }
)
onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return
  router.push({ path: '/search', query: { keyword: kw } })
  mobileSearchOpen.value = false
}

// 进入/停留搜索页时，输入框与 URL 查询词保持同步
watch(
  () => route.query.keyword,
  (v) => {
    if (route.name === 'search') keyword.value = v || ''
  }
)

function handleCommand(cmd) {
  if (cmd === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (cmd === 'avatar') {
    fileInput.value.click()
  }
}

async function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    // 1. 上传图片到 COS → 拿到 URL
    const data = await uploadAvatar(file)
    // 2. 调后端写入数据库 + 更新 store
    await userStore.updateAvatar(data.url)
    ElMessage.success('头像更新成功')
  } catch {
    // 错误已在 request 拦截器统一提示
  } finally {
    uploading.value = false
    // 重置 value 允许重复选同一文件
    e.target.value = ''
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
.user-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  margin-left: 8px;
}
.user-name {
  color: var(--ink);
  font-size: 14px;
}

/* 搜索 */
.header-search {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
/* 通知铃铛 */
.bell-badge {
  display: inline-flex;
  flex-shrink: 0;
}
.bell-btn {
  padding: 8px 4px;
}
.search-toggle {
  display: none;
}
.search-input {
  width: 190px;
}
.mobile-search-bar {
  display: none;
}

@media (max-width: 767px) {
  .app-header {
    height: 60px;
    padding: 0 14px;
    max-width: 100%;
  }
  .logo {
    font-size: 16px;
    flex-shrink: 0;
  }
  /* 手机端：内联输入框隐藏，只留放大镜；展开的整条搜索框浮在 header 下方 */
  .search-input {
    display: none;
  }
  .search-toggle {
    display: inline-flex;
  }
  .mobile-search-bar {
    display: flex;
    align-items: center;
    gap: 8px;
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    z-index: 9;
    padding: 10px 14px;
    background: rgb(255 255 255 / 98%);
    border-bottom: 1px solid var(--line);
  }
  .nav {
    /* flex:1 + min-width:0 是关键：允许导航栏被压缩到剩余宽度，
       超出部分走内部横向滚动，而不是把整个页面撑宽（真手机 WebKit 必需） */
    flex: 1 1 auto;
    min-width: 0;
    max-width: none;
    gap: 0;
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
  .user-trigger {
    margin-left: 2px;
    gap: 4px;
    flex-shrink: 0;
  }
  .user-name {
    display: inline-block;
    max-width: 60px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
