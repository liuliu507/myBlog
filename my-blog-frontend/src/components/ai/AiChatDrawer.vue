<template>
  <el-drawer
    v-model="aiStore.open"
    title="AI 助手"
    direction="rtl"
    size="400px"
    :close-on-press-escape="true"
  >
    <div class="chat-panel">
      <div v-if="aiStore.contextArticle" class="context-tip">
        正在阅读：《{{ aiStore.contextArticle.title }}》
      </div>

      <div ref="msgListRef" class="messages">
        <div v-if="!aiStore.messages.length" class="empty">
          有什么想问的？比如“总结一下重点”。
        </div>
        <div
          v-for="(msg, i) in aiStore.messages"
          :key="i"
          class="bubble"
          :class="msg.role"
        >
          <template v-if="msg.content">{{ msg.content }}</template>
          <span
            v-else-if="aiStore.loading && i === aiStore.messages.length - 1"
            class="thinking"
          >正在思考…</span>
        </div>
      </div>

      <div class="input-area">
        <el-input
          v-model="input"
          type="textarea"
          :rows="2"
          resize="none"
          placeholder="输入问题，Enter 发送"
          @keydown.enter.exact.prevent="onSend"
        />
        <el-button
          type="primary"
          :loading="aiStore.loading"
          :disabled="!input.trim()"
          @click="onSend"
        >发送</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { useAiStore } from '@/stores/ai'

const aiStore = useAiStore()
const input = ref('')
const msgListRef = ref(null)

async function scrollToBottom() {
  await nextTick()
  const el = msgListRef.value
  if (el) el.scrollTop = el.scrollHeight
}

// 消息数量或最后一条内容变化时自动滚到底
watch(() => aiStore.messages.length, scrollToBottom)
watch(
  () => aiStore.messages[aiStore.messages.length - 1]?.content,
  scrollToBottom
)

async function onSend() {
  const text = input.value
  if (!text.trim() || aiStore.loading) return
  input.value = ''
  await aiStore.sendMessage(text)
}
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.context-tip {
  padding: 8px 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--muted, #6b7280);
  background: var(--surface-muted, #f3f4f6);
  border-radius: 6px;
}
.messages {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 4px 0 12px;
}
.empty {
  padding: 48px 0;
  color: var(--muted, #9ca3af);
  font-size: 13px;
  text-align: center;
}
.bubble {
  max-width: 85%;
  margin: 8px 0;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}
.bubble.user {
  margin-left: auto;
  background: var(--el-color-primary, #409eff);
  color: #fff;
}
.bubble.assistant {
  margin-right: auto;
  background: var(--surface-muted, #f3f4f6);
  color: var(--ink, #1f2937);
}
.thinking {
  color: var(--muted, #9ca3af);
}
.input-area {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding-top: 8px;
  border-top: 1px solid var(--line, #e5e7eb);
}
.input-area .el-button {
  flex-shrink: 0;
}

@media (max-width: 767px) {
  :deep(.el-drawer.rtl) {
    width: 100% !important;
  }
}
</style>
