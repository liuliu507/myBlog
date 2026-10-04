<template>
  <div class="mine">
    <div class="page-title">我的文章</div>

    <div v-if="categoryId" class="filter-bar">
      <el-tag closable type="primary" @close="clearFilter">
        分类：{{ categoryName || '筛选中' }}
      </el-tag>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="还没有写过文章">
      <el-button type="primary" @click="$router.push('/editor')">去写一篇</el-button>
    </el-empty>

    <el-table v-loading="loading" :data="list" style="width: 100%">
      <el-table-column label="标题" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" underline="never" @click="$router.push(`/article/${row.id}`)">
            {{ row.title }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '未分类' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/article/${row.id}`)">查看</el-button>
          <el-button size="small" @click="$router.push(`/editor/${row.id}`)">编辑</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMyArticles, deleteArticle } from '@/api/article'
import { formatTime } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const list = ref([])
const loading = ref(false)
const categoryId = ref(null)
const categoryName = ref('')

async function loadList() {
  categoryId.value = route.query.categoryId || null
  categoryName.value = route.query.name || ''
  loading.value = true
  try {
    list.value = await listMyArticles(
      categoryId.value ? { categoryId: categoryId.value } : {}
    )
  } finally {
    loading.value = false
  }
}

function clearFilter() {
  router.replace({ path: '/mine' })
}

async function onDelete(row) {
  await ElMessageBox.confirm(`确定删除《${row.title}》吗？`, '提示', {
    type: 'warning',
  })
  await deleteArticle(row.id)
  ElMessage.success('已删除')
  loadList()
}

watch(() => route.query.categoryId, loadList, { immediate: true })
</script>

<style scoped>
.mine {
  max-width: 900px;
  margin: 0 auto;
  background: #fff;
  padding: 24px;
  border-radius: 6px;
}
.page-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 20px;
}
.filter-bar {
  margin-bottom: 12px;
}
</style>