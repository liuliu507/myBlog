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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticle, deleteArticle } from '@/api/article'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref(null)
const loading = ref(false)

const isAuthor = computed(
  () => article.value && userStore.userInfo?.id === article.value.userId
)

async function loadDetail() {
  loading.value = true
  try {
    article.value = await getArticle(route.params.id)
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
</script>

<style scoped>
.detail {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 32px;
  border-radius: 6px;
  min-height: 400px;
}
.header {
  position: relative;
}
.category-link {
  text-decoration: none;
}
.category-link :deep(.el-tag) {
  cursor: pointer;
}
.title {
  font-size: 26px;
  margin: 0 0 12px;
}
.meta {
  color: #909399;
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
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 767px) {
  .detail {
    padding: 16px;
  }
  .title {
    font-size: 21px;
  }
  .actions {
    position: static;
    margin-top: 12px;
  }
  .content {
    font-size: 15px;
    line-height: 1.7;
  }
}
</style>