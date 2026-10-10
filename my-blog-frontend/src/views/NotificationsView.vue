<template>
  <div class="notif-page">
    <header class="page-heading">
      <div>
        <div class="eyebrow">NOTIFICATIONS</div>
        <h1 class="page-title">消息通知</h1>
      </div>
      <p class="heading-note">评论、回复与点赞都会出现在这里。</p>
    </header>

    <el-empty v-if="!loading && list.length === 0" description="还没有收到任何通知" />

    <div v-loading="loading" class="notif-list">
      <div v-for="n in list" :key="n.id" class="notif-item" @click="goArticle(n)">
        <el-avatar :size="38" :src="n.senderAvatar">
          {{ n.senderNickname?.slice(0, 1) || '?' }}
        </el-avatar>
        <div class="notif-body">
          <p class="notif-line">
            <span class="notif-sender">{{ n.senderNickname || '有人' }}</span>
            <span>{{ actionText(n.type) }}</span>
            <span class="notif-article">《{{ n.articleTitle || '已删除的文章' }}》</span>
          </p>
          <p v-if="n.commentText" class="notif-quote">{{ n.commentText }}</p>
        </div>
        <span class="notif-time">{{ formatTime(n.createdAt) }}</span>
      </div>
    </div>

    <div v-if="total > list.length" class="load-more">
      <el-button :loading="loading" @click="loadMore">加载更多</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, markAllRead } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import { formatTime } from '@/utils/format'

const router = useRouter()
const notifStore = useNotificationStore()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

function actionText(type) {
  if (type === 'like') return '赞了你的文章'
  if (type === 'reply') return '回复了你的评论'
  return '评论了你的文章'
}

async function loadList() {
  loading.value = true
  try {
    const data = await listNotifications({ page: page.value, size: size.value })
    if (page.value === 1) {
      list.value = data.records
    } else {
      list.value = list.value.concat(data.records)
    }
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value += 1
  loadList()
}

function goArticle(n) {
  router.push(`/article/${n.articleId}`)
}

onMounted(async () => {
  await loadList()
  // 列表已展示给用户，清零未读（新通知若在加载后到达，轮询会重新拉高徽标）
  try {
    await markAllRead()
    notifStore.clear()
  } catch {
    // 已读标记失败不影响列表展示
  }
})
</script>

<style scoped>
.notif-page {
  max-width: var(--content-width);
  margin: 0 auto;
  padding: 32px 24px 60px;
}
.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 28px;
}
.eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--accent);
  font-weight: 700;
}
.page-title {
  margin: 4px 0 0;
  font-size: 26px;
  color: var(--ink);
}
.heading-note {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted, #909399);
}

.notif-list {
  display: flex;
  flex-direction: column;
}
.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px 4px;
  border-bottom: 1px solid var(--line);
  cursor: pointer;
  transition: background 0.15s ease;
}
.notif-item:hover {
  background: rgb(0 0 0 / 3%);
}
.notif-body {
  flex: 1;
  min-width: 0;
}
.notif-line {
  margin: 0;
  font-size: 14px;
  color: var(--ink);
  line-height: 1.6;
}
.notif-sender {
  font-weight: 700;
  margin-right: 4px;
}
.notif-article {
  color: var(--accent);
}
.notif-quote {
  margin: 6px 0 0;
  font-size: 13px;
  color: var(--text-muted, #909399);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.notif-time {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-muted, #909399);
  padding-top: 2px;
}
.load-more {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 767px) {
  .notif-page {
    padding: 20px 14px 40px;
  }
  .page-heading {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    margin-bottom: 18px;
  }
  .notif-item {
    gap: 10px;
    padding: 14px 2px;
  }
}
</style>
