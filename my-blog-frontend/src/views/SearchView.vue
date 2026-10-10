<template>
  <div class="search-page">
    <header class="search-heading">
      <div class="eyebrow">SEARCH</div>
      <h1 class="page-title">搜索：{{ keyword }}</h1>
      <p v-if="!loading" class="heading-note">共找到 {{ total }} 篇相关文章</p>
    </header>

    <el-empty v-if="!loading && list.length === 0" description="没有找到相关文章，换个关键词试试" />

    <div v-loading="loading" class="article-list">
      <el-card
        v-for="item in list"
        :key="item.id"
        class="article-card"
        shadow="hover"
        @click="goDetail(item.id)"
      >
        <div class="article-topline">
          <el-tag v-if="item.categoryName" size="small" effect="plain">{{ item.categoryName }}</el-tag>
          <span v-else class="article-label">文章</span>
          <span>{{ formatTime(item.createdAt) }}</span>
        </div>
        <h2 class="title" v-html="highlight(item.title)"></h2>
        <p class="summary" v-html="highlight(item.summary || '暂无摘要')"></p>
        <div v-if="item.tags?.length" class="tag-row">
          <router-link
            v-for="t in item.tags"
            :key="t.id"
            :to="`/tag/${t.id}`"
            class="tag-chip"
            @click.stop
          >
            # {{ t.name }}
          </router-link>
        </div>
        <div class="meta">
          <el-avatar :size="22" :src="item.authorAvatar">
            {{ item.authorNickname?.slice(0, 1) || '作' }}
          </el-avatar>
          <span>{{ item.authorNickname || '匿名作者' }}</span>
          <span class="meta-views">{{ item.viewCount ?? 0 }} 次浏览</span>
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
import { ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchArticles } from '@/api/article'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const list = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const keyword = ref(String(route.query.keyword || ''))

/**
 * 关键词高亮（防 XSS）：
 * 文章标题/摘要来自其他用户输入，必须先整体 HTML 转义，再给命中词包 <mark>；
 * 不能直接把原文塞进 v-html，否则 <script>/<img onerror> 会被执行
 */
function escapeHtml(text) {
  return String(text).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[c]))
}

function highlight(text) {
  const safe = escapeHtml(text || '')
  const kw = keyword.value.trim()
  if (!kw) return safe
  // 转义正则元字符，避免用户输入 ()* 等改变正则语义
  const pattern = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return safe.replace(new RegExp(`(${pattern})`, 'gi'), '<mark>$1</mark>')
}

async function loadList() {
  const kw = String(route.query.keyword || '').trim()
  if (!kw) {
    list.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const data = await searchArticles({ keyword: kw, page: page.value, size: size.value })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goDetail(id) {
  router.push(`/article/${id}`)
}

// 顶部搜索框在本页继续搜索时，query 变化、组件复用，需重置分页重新拉取
watch(
  () => route.query.keyword,
  (v) => {
    keyword.value = String(v || '')
    page.value = 1
    loadList()
  }
)

onMounted(loadList)
</script>

<style scoped>
.search-page {
  max-width: 850px;
  margin: 0 auto;
}
.search-heading {
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
  word-break: break-word;
}
.heading-note {
  margin: 10px 0 0;
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
    border-color 160ms ease;
}
.article-card:hover {
  border-color: #c5d4c9;
  transform: translateY(-2px);
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
.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin: -4px 0 14px;
}
.tag-chip {
  color: var(--accent);
  font-size: 12.5px;
  text-decoration: none;
}
.tag-chip:hover {
  text-decoration: underline;
}
.meta {
  color: var(--muted);
  font-size: 13px;
  display: flex;
  gap: 8px;
  align-items: center;
}
.meta-views {
  margin-left: auto;
  font-size: 12px;
}
:deep(mark) {
  background: #fde9a8;
  color: inherit;
  padding: 0 2px;
  border-radius: 2px;
}
.pagination {
  display: flex;
  justify-content: center;
  margin: 32px 0;
}

@media (max-width: 767px) {
  .page-title {
    font-size: 24px;
  }
  .article-card {
    --el-card-padding: 18px;
  }
  .title {
    font-size: 18px;
  }
}
</style>
