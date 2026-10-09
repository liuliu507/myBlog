<template>
  <div v-loading="loading" class="detail">
    <template v-if="article">
      <div class="header">
        <h1 class="title">{{ article.title }}</h1>
        <router-link
          v-if="article.categoryName"
          :to="`/authors/${article.userId}/categories/${article.categoryId}`"
          class="category-link"
        >
          <el-tag size="small" type="info">
            {{ article.categoryName }}
          </el-tag>
        </router-link>
        <div class="meta">
          <span>{{ article.authorNickname }}</span>
          <span>·</span>
          <span>{{ formatTime(article.createdAt) }}</span>
          <el-tag v-if="article.status === 0" type="warning" size="small">草稿</el-tag>
        </div>

        <div v-if="isAuthor" class="actions">
          <el-button size="small" @click="$router.push(`/editor/${article.id}`)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="onDelete">删除</el-button>
        </div>
      </div>

      <el-divider />

      <div class="content">{{ article.content }}</div>
    </template>

    <el-empty v-else-if="!loading" description="文章不存在或无权查看" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticle, deleteArticle } from '@/api/article'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const aiStore = useAiStore()

const article = ref(null)
const loading = ref(false)

const isAuthor = computed(
  () => article.value && userStore.userInfo?.id === article.value.userId
)

async function loadDetail() {
  loading.value = true
  try {
    article.value = await getArticle(route.params.id)
    // 进入文章页时注入阅读上下文，让 AI 助手能“读懂”当前文章
    if (article.value) {
      aiStore.setContext({ id: article.value.id, title: article.value.title, content: article.value.content })
    }
  } catch (e) {
    article.value = null
  } finally {
    loading.value = false
  }
}

async function onDelete() {
  await ElMessageBox.confirm('确定要删除这篇文章吗？', '提示', {
    type: 'warning',
  })
  await deleteArticle(article.value.id)
  ElMessage.success('已删除')
  router.push('/')
}

onMounted(loadDetail)

// 离开文章页时清除阅读上下文，抽屉恢复为通用对话
onUnmounted(() => aiStore.clearContext())
</script>

<style scoped>
.detail {
  max-width: var(--reading-width);
  margin: 0 auto;
  padding: 48px 56px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
  min-height: 440px;
}
.header {
  position: relative;
  padding-bottom: 8px;
}
.category-link {
  text-decoration: none;
}
.category-link :deep(.el-tag) {
  cursor: pointer;
}
.title {
  max-width: 620px;
  margin: 0 0 20px;
  color: var(--ink);
  font-size: 34px;
  font-weight: 740;
  line-height: 1.3;
}
.meta {
  color: var(--muted);
  font-size: 14px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.actions {
  position: absolute;
  right: 0;
  top: 0;
}
.content {
  padding-top: 8px;
  color: #35433a;
  font-size: 17px;
  line-height: 1.95;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail :deep(.el-divider) {
  margin: 28px 0;
  border-color: var(--line);
}

@media (max-width: 767px) {
  .detail {
    padding: 26px 20px;
    min-height: 360px;
  }
  .title {
    font-size: 26px;
  }
  .actions {
    position: static;
    margin-top: 12px;
  }
  .content {
    font-size: 16px;
    line-height: 1.85;
  }
}
</style>