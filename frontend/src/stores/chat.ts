import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
}

export interface ChatSession {
  id: string
  title: string
  messages: Message[]
  createdAt: Date
  updatedAt: Date
  enableThinking?: boolean // 思维链开关，默认为关闭
}

export const useChatStore = defineStore('chat', () => {
  const currentSession = ref<ChatSession | null>(null)
  const sessions = ref<ChatSession[]>([])
  const loadingSessions = ref<Set<string>>(new Set())
  const streamingMessages = ref<Map<string, string>>(new Map())

  const currentMessages = computed(() => currentSession.value?.messages || [])
  const streamingMessage = computed(() => {
    if (!currentSession.value) return ''
    // 严格检查：只有当当前会话确实在加载时才返回流式消息
    const sessionId = currentSession.value.id
    if (!loadingSessions.value.has(sessionId)) return ''
    return streamingMessages.value.get(sessionId) || ''
  })

  const isLoading = computed(() => {
    if (!currentSession.value) return false
    return loadingSessions.value.has(currentSession.value.id)
  })

  const isSessionLoading = (sessionId: string) => {
    return loadingSessions.value.has(sessionId)
  }

  const generateId = () => {
    return Date.now().toString(36) + Math.random().toString(36).substr(2)
  }

  const createNewSession = () => {
    const newSession: ChatSession = {
      id: generateId(),
      title: '新会话',
      messages: [{
        id: generateId(),
        role: 'assistant',
        content: '你好！我是AI助手，有什么可以帮助您的吗？',
        timestamp: new Date()
      }],
      createdAt: new Date(),
      updatedAt: new Date(),
      enableThinking: false // 默认关闭思维链
    }

    // 确保新会话不会继承任何状态
    streamingMessages.value.delete(newSession.id)
    loadingSessions.value.delete(newSession.id)

    // 添加到会话列表开头
    sessions.value.unshift(newSession)

    // 设置为当前会话
    currentSession.value = newSession

    return newSession
  }

  const addMessage = (role: 'user' | 'assistant', content: string, targetSessionId?: string) => {
    let targetSession = currentSession.value

    // 如果指定了目标会话ID，找到对应的会话
    if (targetSessionId) {
      targetSession = sessions.value.find(s => s.id === targetSessionId) || currentSession.value
    }

    if (!targetSession) {
      createNewSession()
      targetSession = currentSession.value
    }

    const message: Message = {
      id: generateId(),
      role,
      content,
      timestamp: new Date()
    }

    targetSession.messages.push(message)
    targetSession.updatedAt = new Date()

    // 如果是用户的第一条消息，更新会话标题
    if (role === 'user' && targetSession.messages.length === 2) {
      targetSession.title = content.slice(0, 30) + (content.length > 30 ? '...' : '')
    }
  }

  const sendMessage = async (content: string) => {
    if (!content.trim() || !currentSession.value) return

    const targetSessionId = currentSession.value.id
    const targetSession = currentSession.value
    loadingSessions.value.add(targetSessionId)
    addMessage('user', content, targetSessionId)

    try {
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          sessionId: targetSessionId,
          message: content,
          enableThinking: targetSession.enableThinking ?? false
        })
      })

      if (!response.ok) {
        throw new Error('网络响应不正常')
      }

      // 处理流式响应
      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('无法获取响应流')
      }

      const decoder = new TextDecoder()
      let fullResponse = ''
      streamingMessages.value.set(targetSessionId, '')
      
      // SSE解析器状态
      let buffer = '' // 累积不完整的SSE数据
      
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          // 处理剩余缓冲区内容
          if (buffer.trim()) {
            const message = buffer.replace(/^data:/, '')
            fullResponse += message
            if (streamingMessages.value.has(targetSessionId)) {
              streamingMessages.value.set(targetSessionId, fullResponse)
            }
          }
          break
        }

        let chunk = decoder.decode(value, { stream: true })
        if (!buffer && chunk.startsWith('data:')) {
          chunk = chunk.slice(5)
        }
        buffer += chunk
         
        // 查找完整的SSE消息
        let messageEndIndex
        while ((messageEndIndex = buffer.indexOf('data:')) !== -1) {
          // 提取完整的SSE消息
          let messageContent = buffer.slice(0, messageEndIndex)
          buffer = buffer.slice(messageEndIndex + 5) // 移除已处理的消息和分隔符
          if (messageContent.endsWith('\n\n')) {
            messageContent = messageContent.slice(0, -2)
          }
          if (messageContent) {
            fullResponse += messageContent
            // 确保只在原始会话中更新流式消息
            if (streamingMessages.value.has(targetSessionId)) {
              streamingMessages.value.set(targetSessionId, fullResponse)
            }
          }
        }
      }

      // 只在原始会话中添加完整的AI回复
      if (fullResponse) {
        // 修复可能的表格格式问题
        // const fixedResponse = fixStreamingTable(fullResponse);
        // if (fixedResponse !== fullResponse) {
        //   console.log('表格格式已修复');
        //   console.log('原始响应:', fullResponse);
        //   console.log('修复后响应:', fixedResponse);
        // }
        addMessage('assistant', fullResponse, targetSessionId)
      }
      streamingMessages.value.delete(targetSessionId)

    } catch (error) {
      // 只在原始会话中添加错误消息
      addMessage('assistant', '抱歉，发生了错误，请稍后再试。', targetSessionId)
      streamingMessages.value.delete(targetSessionId)
    } finally {   loadingSessions.value.delete(targetSessionId)
    }
  }

  const clearCurrentSession = () => {
    if (currentSession.value) {
      currentSession.value.messages = [{
        id: generateId(),
        role: 'assistant',
        content: '历史记录已清除。有什么新的问题吗？',
        timestamp: new Date()
      }]
      currentSession.value.updatedAt = new Date()
    }
  }

  const deleteSession = (sessionId: string) => {
    const index = sessions.value.findIndex(s => s.id === sessionId)
    if (index !== -1) {
      // 从会话数组中删除
      sessions.value.splice(index, 1)

      // 清理删除会话的流式消息和加载状态
      streamingMessages.value.delete(sessionId)
      loadingSessions.value.delete(sessionId)

      // 如果删除的是当前会话，切换到其他会话
      if (currentSession.value?.id === sessionId) {
        // 优先切换到最近更新的会话
        const sortedSessions = [...sessions.value].sort((a, b) =>
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        )
        currentSession.value = sortedSessions[0] || null
      }


    }
  }

  const switchSession = (sessionId: string) => {
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      currentSession.value = session
    }
  }

  const toggleThinking = (enable?: boolean) => {
    if (currentSession.value) {
      currentSession.value.enableThinking = enable !== undefined ? enable : !currentSession.value.enableThinking
      currentSession.value.updatedAt = new Date()
    }
  }

  // 初始化
  if (!currentSession.value && sessions.value.length === 0) {
    createNewSession()
  }

  return {
    currentSession,
    sessions,
    isLoading,
    streamingMessage,
    currentMessages,
    createNewSession,
    sendMessage,
    addMessage,
    clearCurrentSession,
    deleteSession,
    switchSession,
    isSessionLoading,
    toggleThinking
  }
})