import { defineStore } from 'pinia'
import { sendMessageStream } from '@/api/chat'
import { getHistory } from '@/api/chat'

export const useAiStore = defineStore('ai', {
  state: () => ({
    open: false,
    loading: false,
    // 消息列表：{ role: 'user' | 'assistant', content: '...' }
    messages: [],
    // 文章阅读上下文（文章页注入，离开后清除）
    contextArticle: null,
    // 当前会话是否已从后端加载过历史（避免每次打开抽屉都重拉，覆盖进行中的对话）
    historyLoaded: false,
  }),

  actions: {
    async openDrawer() {
      this.open = true
      // 首次打开且本地无消息：从后端拉取该用户最近历史，实现跨设备/刷新后回显
      if (!this.historyLoaded && this.messages.length === 0) {
        await this.loadHistory()
      }
    },
    closeDrawer() {
      this.open = false
    },
    toggle() {
      this.open = !this.open
    },

    async loadHistory() {
      try {
        const list = await getHistory()
        this.messages = (list || []).map((m) => ({ role: m.role, content: m.content }))
      } catch (e) {
        // 拉取历史失败不阻塞当前对话
      } finally {
        this.historyLoaded = true
      }
    },

    // 文章页进入时调用：注入当前阅读上下文（正文截断防 token 溢出）
    setContext({ id, title, content }) {
      const truncated = content && content.length > 4000
        ? content.slice(0, 4000) + '…（已截断）'
        : content
      this.contextArticle = { id, title, content: truncated }
    },
    clearContext() {
      this.contextArticle = null
    },
    clearMessages() {
      this.messages = []
      // 清空同时重置历史标记，下次打开抽屉会重新拉当前用户的历史
      this.historyLoaded = false
    },

    async sendMessage(text) {
      const trimmed = (text || '').trim()
      if (!trimmed || this.loading) return

      // 先把用户消息推入列表
      this.messages.push({ role: 'user', content: trimmed })
      // 占位一条 AI 回复，流式分片逐字累加进去（触发 UI 打字机效果）
      const placeholderIndex = this.messages.push({ role: 'assistant', content: '' }) - 1
      this.loading = true

      let acc = ''
      try {
        await sendMessageStream(
          {
            message: trimmed,
            articleTitle: this.contextArticle?.title || '',
            articleContent: this.contextArticle?.content || '',
            articleId: this.contextArticle?.id || null,
          },
          {
            onDelta: (delta) => {
              acc += delta
              this.messages[placeholderIndex].content = acc
            },
            onDone: () => {
              if (!acc) this.messages[placeholderIndex].content = '(无回复)'
            },
            onError: (e) => {
              // 限流/服务异常：把后端给的 message 显示在气泡里
              this.messages[placeholderIndex].content = e.message || '回复失败，请稍后重试'
            },
          }
        )
      } catch (e) {
        this.messages[placeholderIndex].content = '回复失败，请稍后重试'
      } finally {
        this.loading = false
      }
    },
  },
})
