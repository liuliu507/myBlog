<template>
  <div class="category-page">
    <div class="header">
      <div class="page-title">分类管理</div>
      <el-button type="primary" @click="openCreate">新建分类</el-button>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="还没有分类" />

    <el-table v-loading="loading" :data="list" style="width: 100%">
      <el-table-column label="分类名" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" underline="never" @click="goArticles(row)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="文章数" width="120">
        <template #default="{ row }">{{ row.articleCount }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">重命名</el-button>
          <el-button size="small" type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing ? '重命名分类' : '新建分类'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules">
        <el-form-item prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入分类名"
            maxlength="50"
            @keyup.enter="onSubmit"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="onSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from '@/api/category'
import { formatTime } from '@/utils/format'

const list = ref([])
const loading = ref(false)

const router = useRouter()

function goArticles(row) {
  router.push({
    path: '/mine',
    query: { categoryId: row.id, name: row.name },
  })
}

const dialogVisible = ref(false)
const editing = ref(null)
const submitting = ref(false)

const formRef = ref()
const form = reactive({ name: '' })
const rules = {
  name: [{ required: true, message: '请输入分类名', trigger: 'blur' }],
}

async function loadList() {
  loading.value = true
  try {
    list.value = await listCategories()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.name = row.name
  dialogVisible.value = true
}

async function onSubmit() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (editing.value) {
      await updateCategory(editing.value.id, { name: form.name })
      ElMessage.success('重命名成功')
    } else {
      await createCategory({ name: form.name })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row) {
  await ElMessageBox.confirm(
    `删除分类「${row.name}」后，该分类下的文章会变成「未分类」，确定吗？`,
    '提示',
    { type: 'warning' }
  )
  await deleteCategory(row.id)
  ElMessage.success('已删除')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.category-page {
  max-width: 900px;
  margin: 0 auto;
  background: var(--surface);
  padding: 28px;
  border: 1px solid var(--line);
  border-radius: 12px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  color: var(--ink);
  font-size: 24px;
  font-weight: 720;
}

@media (max-width: 767px) {
  .category-page {
    padding: 20px 14px;
  }
  .header {
    align-items: flex-start;
    gap: 12px;
  }
}
</style>