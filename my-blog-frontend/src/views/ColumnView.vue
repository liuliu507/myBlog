<template>
  <div v-loading="loading" class="column">
    <template v-if="detail">
      <div class="head">
        <div class="title">{{ detail.name }}</div>
        <div class="sub">作者：{{ detail.authorNickname || detail.authorId }}</div>
      </div>

      <el-divider />

      <el-empty
        v-if="detail.articles.length === 0"
        description="这个专栏还没有发布文章"
      />

      <div v-else class="article-list">
        <el-card
          v-for="a in detail.articles"
          :key="a.id"
          shadow="hover"
          class="article-card"
          @click="$router.push(`/article/${a.id}`)"
        >
          <div class="article-title">{{ a.title }}</div>
          <div v-if="a.summary" class="article-summary">{{ a.summary }}</div>
          <div class="article-meta">
            <span>{{ formatTime(a.createdAt) }}</span>
          </div>
        </el-card>
      </div>
    </template>

    <el-empty v-else-if="!loading" description="专栏不存在" />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCategory } from '@/api/category'
import { formatTime } from '@/utils/format'

const route = useRoute()
const detail = ref(null)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    detail.value = await getCategory(route.params.categoryId)
  } catch (e) {
    detail.value = null
  } finally {
    loading.value = false
  }
}

watch(() => route.params.categoryId, load, { immediate: true })
</script>

<style scoped>
.column {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  padding: 32px;
  border-radius: 6px;
  min-height: 400px;
}
.head .title {
  font-size: 26px;
  font-weight: bold;
}
.head .sub {
  margin-top: 8px;
  color: #909399;
  font-size: 14px;
}
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.article-card {
  cursor: pointer;
}
.article-title {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}
.article-summary {
  margin-top: 8px;
  color: #606266;
  font-size: 14px;
}
.article-meta {
  margin-top: 12px;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 767px) {
  .column {
    padding: 16px;
  }
  .head .title {
    font-size: 21px;
  }
  .article-list {
    gap: 12px;
  }
  .article-title {
    font-size: 16px;
  }
}
</style>
