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
          <span>·</span>
          <span>{{ article.viewCount ?? 0 }} 次浏览</span>
          <el-tag v-if="article.status === 0" type="warning" size="small">草稿</el-tag>
        </div>

        <div v-if="article.tags?.length" class="article-tags">
          <router-link
            v-for="t in article.tags"
            :key="t.id"
            :to="`/tag/${t.id}`"
            class="article-tag"
          >
            # {{ t.name }}
          </router-link>
        </div>

        <div v-if="isAuthor" class="actions">
          <el-button size="small" @click="$router.push(`/editor/${article.id}`)">
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="onDelete">删除</el-button>
        </div>
      </div>

      <el-divider />

      <MdPreview :model-value="article.content" preview-theme="github" />

      <!-- 点赞区 -->
      <div class="like-bar">
        <button
          :class="['like-btn', { liked: article.likedByMe }]"
          :disabled="likeLoading"
          @click="onToggleLike"
        >
          <svg class="heart" viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
            <path
              d="M12 21s-7.5-4.9-10-9.5C.5 8 2 4.5 5.5 4c2-.3 3.8.7 4.9 2.3h1.2C12.7 4.7 14.5 3.7 16.5 4c3.5.5 5 4 3.5 7.5C19.5 16.1 12 21 12 21Z"
              :fill="article.likedByMe ? 'currentColor' : 'none'"
              stroke="currentColor"
              stroke-width="1.8"
              stroke-linejoin="round"
            />
          </svg>
          <span>{{ article.likedByMe ? '已点赞' : '点赞' }}</span>
          <span class="like-count">{{ article.likeCount ?? 0 }}</span>
        </button>
        <span v-if="!userStore.isLoggedIn" class="like-tip">登录后可点赞</span>
      </div>

      <el-divider />

      <!-- 评论区 -->
      <section class="comments">
        <h3 class="comments-title">评论 {{ commentCount }}</h3>

        <div v-if="userStore.isLoggedIn" class="comment-input">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="写下你的想法…"
          />
          <div class="comment-input-actions">
            <el-button
              type="primary"
              size="small"
              :loading="commentPosting"
              @click="onPostComment()"
            >
              发表评论
            </el-button>
          </div>
        </div>
        <p v-else class="comment-login-tip">
          <router-link to="/login">登录</router-link> 后可发表评论
        </p>

        <el-empty v-if="!commentsLoading && comments.length === 0" description="还没有评论，来抢沙发" />

        <div v-loading="commentsLoading" class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <el-avatar :size="34" :src="c.avatar">{{ c.nickname?.slice(0, 1) || '?' }}</el-avatar>
            <div class="comment-body">
              <div class="comment-head">
                <span class="comment-nick">{{ c.nickname || '匿名用户' }}</span>
                <span class="comment-time">{{ formatTime(c.createdAt) }}</span>
              </div>
              <p class="comment-content">{{ c.content }}</p>
              <div class="comment-ops">
                <span v-if="userStore.isLoggedIn" class="comment-op" @click="replyTarget = replyTarget === c.id ? null : c.id">
                  回复
                </span>
                <span v-if="canDeleteComment(c)" class="comment-op danger" @click="onDeleteComment(c)">删除</span>
              </div>

              <!-- 回复输入框 -->
              <div v-if="replyTarget === c.id" class="reply-input">
                <el-input
                  v-model="replyText"
                  type="textarea"
                  :rows="2"
                  maxlength="500"
                  show-word-limit
                  :placeholder="`回复 ${c.nickname || '匿名用户'}…`"
                />
                <div class="comment-input-actions">
                  <el-button size="small" @click="replyTarget = null">取消</el-button>
                  <el-button
                    type="primary"
                    size="small"
                    :loading="commentPosting"
                    @click="onPostComment(c.id)"
                  >
                    回复
                  </el-button>
                </div>
              </div>

              <!-- 二级回复 -->
              <div v-for="r in c.replies" :key="r.id" class="comment-reply">
                <el-avatar :size="26" :src="r.avatar">{{ r.nickname?.slice(0, 1) || '?' }}</el-avatar>
                <div class="comment-body">
                  <div class="comment-head">
                    <span class="comment-nick">{{ r.nickname || '匿名用户' }}</span>
                    <span class="comment-time">{{ formatTime(r.createdAt) }}</span>
                  </div>
                  <p class="comment-content">{{ r.content }}</p>
                  <div class="comment-ops">
                    <span v-if="canDeleteComment(r)" class="comment-op danger" @click="onDeleteComment(r)">删除</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <el-empty v-else-if="!loading" description="文章不存在或无权查看" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticle, deleteArticle } from '@/api/article'
import { listComments, createComment, deleteComment, toggleLike, recordView } from '@/api/comment'
import { formatTime } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import { useAiStore } from '@/stores/ai'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const aiStore = useAiStore()

const article = ref(null)
const loading = ref(false)

const comments = ref([])
const commentsLoading = ref(false)
const commentText = ref('')
const replyText = ref('')
const replyTarget = ref(null)
const commentPosting = ref(false)
const likeLoading = ref(false)

const isAuthor = computed(
  () => article.value && userStore.userInfo?.id === article.value.userId
)

const commentCount = computed(() =>
  comments.value.reduce((sum, c) => sum + 1 + (c.replies?.length || 0), 0)
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

async function loadComments() {
  commentsLoading.value = true
  try {
    comments.value = await listComments(route.params.id)
  } finally {
    commentsLoading.value = false
  }
}

async function onPostComment(parentId = null) {
  const content = parentId ? replyText.value.trim() : commentText.value.trim()
  if (!content) {
    ElMessage.warning('请输入评论内容')
    return
  }
  commentPosting.value = true
  try {
    await createComment({ articleId: Number(route.params.id), parentId, content })
    if (parentId) {
      replyText.value = ''
      replyTarget.value = null
    } else {
      commentText.value = ''
    }
    ElMessage.success('评论成功')
    await loadComments()
  } finally {
    commentPosting.value = false
  }
}

function canDeleteComment(c) {
  if (!userStore.userInfo) return false
  return userStore.userInfo.id === c.userId || isAuthor.value
}

async function onDeleteComment(c) {
  await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', { type: 'warning' })
  await deleteComment(c.id)
  ElMessage.success('已删除')
  await loadComments()
}

async function onToggleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  likeLoading.value = true
  try {
    const data = await toggleLike(article.value.id)
    article.value.likeCount = data.likeCount
    article.value.likedByMe = data.likedByMe
  } finally {
    likeLoading.value = false
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

onMounted(async () => {
  await loadDetail()
  loadComments()
  // 浏览计数：后端 Redis 24h 防刷，重复进入不会累加；失败不影响阅读
  if (article.value) {
    recordView(article.value.id).then((count) => {
      article.value.viewCount = count
    }).catch(() => {})
  }
})

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
.article-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-top: 14px;
}
.article-tag {
  color: var(--accent);
  font-size: 13.5px;
  text-decoration: none;
}
.article-tag:hover {
  text-decoration: underline;
}
.actions {
  position: absolute;
  right: 0;
  top: 0;
}

.detail :deep(.el-divider) {
  margin: 28px 0;
  border-color: var(--line);
}
.detail :deep(.md-editor-preview) {
  font-size: 17px;
  line-height: 1.95;
  /* 防止正文图片/表格/长代码在手机上撑破页面 */
  max-width: 100%;
  overflow-x: auto;
}
.detail :deep(.md-editor-preview img) {
  max-width: 100%;
  height: auto;
}
.detail :deep(.md-editor-preview table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
}

/* 点赞 */
.like-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 32px;
}
.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: #fff;
  color: var(--muted);
  font-size: 14px;
  cursor: pointer;
  transition: color 150ms ease, border-color 150ms ease, background 150ms ease;
}
.like-btn:hover {
  color: #d64f4f;
  border-color: #ecc9c9;
}
.like-btn.liked {
  color: #d64f4f;
  border-color: #ecc9c9;
  background: #fdf3f3;
}
.like-btn:disabled {
  cursor: default;
  opacity: 0.7;
}
.like-count {
  font-weight: 650;
}
.like-tip {
  color: var(--muted);
  font-size: 12px;
}

/* 评论区 */
.comments-title {
  margin: 0 0 20px;
  color: var(--ink);
  font-size: 18px;
  font-weight: 680;
}
.comment-input {
  margin-bottom: 24px;
}
.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 10px;
}
.comment-login-tip {
  margin: 0 0 24px;
  color: var(--muted);
  font-size: 14px;
}
.comment-login-tip a {
  color: var(--accent);
  text-decoration: none;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 22px;
  min-height: 60px;
}
.comment-item {
  display: flex;
  gap: 12px;
}
.comment-body {
  flex: 1;
  min-width: 0;
}
.comment-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.comment-nick {
  color: var(--ink);
  font-size: 14px;
  font-weight: 640;
}
.comment-time {
  color: var(--muted);
  font-size: 12px;
}
.comment-content {
  margin: 6px 0 4px;
  color: #35433a;
  font-size: 14px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-ops {
  display: flex;
  gap: 14px;
}
.comment-op {
  color: var(--muted);
  font-size: 12px;
  cursor: pointer;
}
.comment-op:hover {
  color: var(--accent);
}
.comment-op.danger:hover {
  color: #d64f4f;
}
.reply-input {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fafcfa;
}
.comment-reply {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f7faf7;
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
  .detail :deep(.md-editor-preview) {
    font-size: 16px;
    line-height: 1.85;
  }
}
</style>
