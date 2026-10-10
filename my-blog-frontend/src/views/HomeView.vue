<template>
  <div class="home">
    <header class="home-heading">
      <div>
        <div class="eyebrow">BLOG · NOTES</div>
        <h1 class="page-title">最新文章</h1>
      </div>
      <p class="heading-note">记录想法，也分享值得回看的内容。</p>
    </header>

    <el-empty v-if="!loading && list.length === 0" description="还没有文章" />

    <div v-loading="loading" class="article-list">
      <el-card
        v-for="item in list"
        :key="item.id"
        :class="['article-card', { featured: list[0]?.id === item.id }]"
        shadow="hover"
        @click="goDetail(item.id)"
      >
        <div class="article-topline">
          <router-link
            v-if="item.categoryName"
            :to="`/authors/${item.userId}/categories/${item.categoryId}`"
            class="category-link"
            @click.stop
          >
            <el-tag size="small" effect="plain">{{ item.categoryName }}</el-tag>
          </router-link>
          <span v-else class="article-label">文章</span>
          <span>{{ formatTime(item.createdAt) }}</span>
        </div>
        <h2 class="title">{{ item.title }}</h2>
        <p class="summary">{{ item.summary || '暂无摘要' }}</p>
        <div class="meta">
          <el-avatar :size="22" :src="item.authorAvatar">
            {{ item.authorNickname?.slice(0, 1) || '作' }}
          </el-avatar>
          <span>{{ item.authorNickname || '匿名作者' }}</span>
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
  max-width: 850px;
  margin: 0 auto;
}
.home-heading {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 24px;
  margin: 12px 0 28px;
}
.eyebrow {
  margin-bottom: 8px;
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}
.page-title {
  margin: 0;
  color: var(--ink);
  font-size: 30px;
  font-weight: 720;
  line-height: 1.2;
}
.heading-note {
  margin: 0 0 2px;
  color: var(--muted);
  font-size: 14px;
}
.article-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
}
.article-card {
  --el-card-padding: 22px 24px;
  cursor: pointer;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}
.article-card:hover {
  border-color: #c5d4c9;
  transform: translateY(-2px);
}
.article-card.featured {
  border-left: 3px solid var(--accent);
  background: linear-gradient(110deg, #fff 0%, #f8fbf8 100%);
}
.article-topline {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 24px;
  color: var(--muted);
  font-size: 12px;
}
.article-label {
  color: var(--warm-accent);
  font-weight: 650;
}
.title {
  margin: 14px 0 8px;
  color: var(--ink);
  font-size: 20px;
  font-weight: 680;
  line-height: 1.45;
}
.featured .title {
  font-size: 25px;
}
.summary {
  color: #56635b;
  font-size: 14px;
  line-height: 1.75;
  margin: 0 0 18px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.meta {
  color: var(--muted);
  font-size: 13px;
  display: flex;
  gap: 8px;
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
  margin: 32px 0;
}

@media (max-width: 767px) {
  .home-heading {
    display: block;
    margin: 4px 0 22px;
  }
  .page-title {
    font-size: 26px;
  }
  .heading-note {
    margin-top: 10px;
    font-size: 13px;
  }
  .article-card {
    --el-card-padding: 18px;
  }
  .title {
    font-size: 18px;
  }
  .featured .title {
    font-size: 21px;
  }
}
</style>