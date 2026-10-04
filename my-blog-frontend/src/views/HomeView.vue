<template>
  <div class="home">
    <div class="page-title">最新文章</div>

    <el-empty v-if="!loading && list.length === 0" description="还没有文章" />

    <div v-loading="loading" class="article-list">
      <el-card
        v-for="item in list"
        :key="item.id"
        class="article-card"
        shadow="hover"
        @click="goDetail(item.id)"
      >
        <h3 class="title">{{ item.title }}</h3>
        <div class="meta">
          <router-link
            v-if="item.categoryName"
            :to="`/authors/${item.userId}/categories/${item.categoryId}`"
            class="category-link"
            @click.stop
          >
            <el-tag size="small" type="info">{{ item.categoryName }}</el-tag>
          </router-link>
          <span>{{ item.authorNickname }}</span>
          <span>·</span>
          <span>{{ formatTime(item.createdAt) }}</span>
        </div>
        <p class="summary">{{ item.summary || '暂无摘要' }}</p>
        <div class="meta">
          <span>{{ item.authorNickname }}</span>
          <span>·</span>
          <span>{{ formatTime(item.createdAt) }}</span>
        </div>
      </el-card>
    </div>

    <div v-if="total > 0" class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="loadList"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listArticles } from '@/api/article'
import { formatTime } from '@/utils/format'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function loadList() {
  loading.value = true
  try {
    const data = await listArticles({ page: page.value, size: size.value })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/article/${id}`)
}

onMounted(loadList)
</script>

<style scoped>
.home {
  max-width: 800px;
  margin: 0 auto;
}
.page-title {
  font-size: 22px;
  font-weight: bold;
  margin: 16px 0 24px;
}
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}
.article-card {
  cursor: pointer;
  transition: transform 0.15s;
}
.article-card:hover {
  transform: translateY(-2px);
}
.title {
  margin: 0 0 8px;
  font-size: 18px;
  color: #303133;
}
.summary {
  color: #606266;
  font-size: 14px;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.meta {
  color: #909399;
  font-size: 13px;
  display: flex;
  gap: 6px;
  align-items: center;
}
.category-link {
  text-decoration: none;
}
.category-link :deep(.el-tag) {
  cursor: pointer;
}
.pagination {
  display: flex;
  justify-content: center;
  margin: 24px 0;
}
</style>