import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

export function sendMessage(data) {
  // AI 回复可能较慢，单独放宽超时到 60s（共享实例默认 10s 不够）
  return request.post('/chat', data, { timeout: 60000 })
}

// 拉取当前用户最近的对话历史（打开抽屉时回显）
export function getHistory() {
  return request.get('/chat/history')
}

/**
 * 流式对话：通过 SSE 边收边回调 onDelta（每个分片），结束后调 onDone，出错调 onError。
 * 直接用 fetch + ReadableStream 解析 SSE（axios 不便处理流式响应）。
 */
export async function sendMessageStream(data, { onDelta, onDone, onError } = {}) {
  const userStore = useUserStore()
  try {
    const resp = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(userStore.token ? { Authorization: `Bearer ${userStore.token}` } : {}),
      },
      body: JSON.stringify(data),
    })

    // 限流等业务错误：后端返回 JSON（Result）而非 SSE
    const ct = resp.headers.get('content-type') || ''
    if (!resp.ok || !ct.includes('event-stream')) {
      let msg = `回复失败（${resp.status}）`
      try {
        const json = await resp.json()
        msg = json.message || msg
      } catch (_) { /* ignore */ }
      onError?.(new Error(msg))
      return
    }

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buf = ''
    while (true) {
      const { value, done } = await reader.read()
      if (done) break
      buf += decoder.decode(value, { stream: true })
      // SSE 事件以空行分隔
      let idx
      while ((idx = buf.indexOf('\n\n')) >= 0) {
        const event = buf.slice(0, idx)
        buf = buf.slice(idx + 2)
        // 取 data: 行
        const dataLine = event
          .split('\n')
          .find((l) => l.startsWith('data:'))
        if (!dataLine) continue
        const payload = dataLine.replace(/^data:\s*/, '').trim()
        if (!payload || payload === '[DONE]') continue
        try {
          const obj = JSON.parse(payload)
          if (obj.text) onDelta?.(obj.text)
        } catch (_) { /* 跳过无法解析的分片 */ }
      }
    }
    onDone?.()
  } catch (e) {
    onError?.(e)
  }
}
