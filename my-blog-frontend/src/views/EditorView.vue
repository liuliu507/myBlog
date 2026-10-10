<template>
  <div class="editor">
    <div class="page-title">{{ isEdit ? '编辑文章' : '写文章' }}</div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入标题" maxlength="200" show-word-limit />
      </el-form-item>

      <el-form-item label="摘要（可选）" prop="summary">
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="2"
          placeholder="一句话概括，列表页展示用"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="分类（可选）" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 240px">
          <el-option
            v-for="c in categories"
            :key="c.id"
            :label="c.name"
            :value="c.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="标签（可选，最多 10 个）">
        <el-select
          v-model="form.tags"
          multiple
          filterable
          allow-create
          default-first-option
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择已有标签，或直接输入新标签后回车"
          style="width: 100%"
          @change="onTagsChange"
        >
          <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>

      <el-form-item label="正文" prop="content">
        <MdEditor
          v-model="form.content"
          :on-upload-img="onUploadImg"
          :style="{ height: '500px' }"
          language="zh-CN"
          preview-theme="github"
          :toolbars-exclude="['github', 'save', 'htmlPreview', 'catalog']"
        />
      </el-form-item>

      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">草稿</el-radio>
          <el-radio :value="1">发布</el-radio>
        </el-radio-group>
      </el-form-item>

      <div class="actions">
        <el-button @click="$router.back()">取消</el-button>
        <el-button type="primary" :loading="saving" @click="onSave">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArticle, createArticle, updateArticle } from '@/api/article'
import { listCategories } from '@/api/category'
import { listTags } from '@/api/tag'
import { uploadArticleImage } from '@/api/upload'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)

const formRef = ref()
const saving = ref(false)

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null,
  tags: [],
  status: 1,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入正文', trigger: 'blur' }],
}

const categories = ref([])
const tagOptions = ref([])

const MAX_TAGS = 10

/** 标签数上限提示并截断（后端也会兜底） */
function onTagsChange(val) {
  if (val.length > MAX_TAGS) {
    ElMessage.warning(`最多只能添加 ${MAX_TAGS} 个标签`)
    form.tags = val.slice(0, MAX_TAGS)
  }
}

/** md-editor-v3 图片上传回调：files → 后端转发到 COS → 返回 URL 数组 */
async function onUploadImg(files, callback) {
  const res = await Promise.all(
    files.map((file) => {
      return uploadArticleImage(file).then((data) => data.url)
    })
  )
  callback(res)
}

async function loadArticle() {
  if (!isEdit.value) return
  const data = await getArticle(route.params.id)
  form.title = data.title
  form.summary = data.summary || ''
  form.categoryId = data.categoryId || null
  form.content = data.content
  form.status = data.status
  // 后端返回的标签是 {id,name} 对象，编辑器只提交名字符串
  form.tags = (data.tags || []).map((t) => t.name)
}

async function onSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (isEdit.value) {
      await updateArticle(route.params.id, form)
      ElMessage.success('保存成功')
    } else {
      const res = await createArticle(form)
      ElMessage.success('创建成功')
      router.replace(`/editor/${res.id}`)
      return
    }
    router.push('/mine')
  } finally {
    saving.value = false
  }
}

function resetForm() {
  form.title = ''
  form.summary = ''
  form.content = ''
  form.categoryId = null
  form.tags = []
  form.status = 1
}

// 路由从 /editor/8 切到 /editor（或切换编辑对象）时组件会被复用，
// onMounted 不会重新触发，因此监听 id 变化来重置/加载，避免残留上一篇数据
watch(
  () => route.params.id,
  async (id) => {
    resetForm()
    if (id) {
      await loadArticle()
    }
  },
  { immediate: true }
)

onMounted(async () => {
  categories.value = await listCategories()
  // 标签云即全部"有已发布文章"的标签，作为下拉建议；未列出的标签仍可直接输入创建
  try {
    const tags = await listTags()
    tagOptions.value = tags.map((t) => t.name)
  } catch {
    tagOptions.value = []
  }
})
</script>

<style scoped>
.editor {
  max-width: 800px;
  margin: 0 auto;
  background: var(--surface);
  padding: 36px;
  border: 1px solid var(--line);
  border-radius: 12px;
}
.page-title {
  margin: 0 0 26px;
  color: var(--ink);
  font-size: 25px;
  font-weight: 720;
}
.editor :deep(.el-form-item__label) {
  color: #47564d;
  font-weight: 620;
}
.editor :deep(.md-editor) {
  border: 1px solid var(--line);
  border-radius: 8px;
  overflow: hidden;
}
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 767px) {
  .editor {
    padding: 22px 16px;
  }
}
</style>