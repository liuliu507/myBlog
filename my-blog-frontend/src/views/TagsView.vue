<template>
  <div v-loading="loading" class="tags-page">
    <header class="tags-heading">
      <div class="eyebrow">TAGS</div>
      <h1 class="page-title">标签云</h1>
      <p class="heading-note">通过标签发现感兴趣的话题</p>
    </header>

    <el-empty v-if="!loading && tags.length === 0" description="还没有标签，写文章时添加标签吧" />

    <div class="cloud">
      <router-link
        v-for="t in tags"
        :key="t.id"
        :to="`/tag/${t.id}`"
        class="cloud-tag"
        :style="{ fontSize: tagFontSize(t.articleCount) + 'px' }"
      >
        # {{ t.name }}
        <span class="count">{{ t.articleCount }}</span>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listTags } from '@/api/tag'

const tags = ref([])
const loading = ref(false)
const maxCount = ref(1)

/** 标签字号随文章数在 15~32px 之间线性放大 */
function tagFontSize(count) {
  const ratio = maxCount.value > 1 ? (count - 1) / (maxCount.value - 1) : 1
  return (15 + Math.max(0, ratio) * 17).toFixed(1)
}

onMounted(async () => {
  loading.value = true
  try {
    tags.value = await listTags()
    maxCount.value = Math.max(1, ...tags.value.map((t) => t.articleCount || 0))
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tags-page {
  max-width: 850px;
  margin: 0 auto;
}
.tags-heading {
  margin: 12px 0 32px;
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
}
.heading-note {
  margin: 10px 0 0;
  color: var(--muted);
  font-size: 14px;
}
.cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 22px;
  align-items: center;
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--surface);
}
.cloud-tag {
  color: var(--accent);
  font-weight: 600;
  line-height: 1.4;
  text-decoration: none;
  transition: color 120ms ease;
}
.cloud-tag:hover {
  color: var(--warm-accent);
}
.cloud-tag .count {
  margin-left: 3px;
  font-size: 12px;
  font-weight: 400;
  color: var(--muted);
}

@media (max-width: 767px) {
  .cloud {
    padding: 20px 16px;
    gap: 10px 16px;
  }
  .page-title {
    font-size: 26px;
  }
}
</style>
