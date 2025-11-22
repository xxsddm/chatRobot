<template>
  <div class="chat-container">
    <!-- 侧边栏 -->
    <el-aside width="280px" class="sidebar">
      <div class="sidebar-header">
        <el-button type="primary" :icon="Plus" @click="createNewChat" class="new-chat-btn">
          新建会话
        </el-button>
      </div>
      
      <div class="session-list">
        <div
          v-for="session in chatStore.sessions"
          :key="session.id"
          :class="['session-item', { active: session.id === chatStore.currentSession?.id }]"
          @click="switchSession(session.id)"
        >
          <div class="session-title">{{ session.title }}</div>
          <div class="session-time">
            <span v-if="chatStore.isSessionLoading(session.id)" class="loading-indicator">
              <el-icon class="is-loading">
                <Loading />
              </el-icon>
            </span>
            <span v-else>{{ formatTime(session.updatedAt) }}</span>
          </div>
          <el-button
            type="danger"
            :icon="Delete"
            size="small"
            circle
            class="delete-btn"
            @click.stop="deleteSession(session.id)"
          />
        </div>
      </div>
    </el-aside>

    <!-- 主聊天区域 -->
    <el-container class="chat-main">
      <el-header class="chat-header">
        <div class="header-left">
          <h2>{{ chatStore.currentSession?.title || '聊天助手' }}</h2>
        </div>
        <div class="header-right">
          <el-switch
            v-model="currentSession.enableThinking"
            active-text="思维链"
            inactive-text="普通模式"
            @change="handleThinkingToggle"
            class="thinking-switch"
          />
          <el-button
            type="warning"
            :icon="Delete"
            @click="clearCurrentChat"
            :disabled="!chatStore.currentMessages.length"
          >
            清除历史
          </el-button>
        </div>
      </el-header>

      <el-main class="chat-messages">
        <div class="messages-container" ref="messagesContainer">
          <div
            v-for="message in chatStore.currentMessages"
            :key="message.id"
            :class="['message-wrapper', message.role]"
          >
            <div class="message-avatar">
              <el-avatar :icon="message.role === 'user' ? User : ChatLineRound" />
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <MarkdownRenderer :content="message.content" />
              </div>
              <div class="message-time">{{ formatTime(message.timestamp) }}</div>
            </div>
          </div>
          
          <!-- 流式消息 -->
          <div v-if="chatStore.streamingMessage" class="message-wrapper assistant">
            <div class="message-avatar">
              <el-avatar :icon="ChatLineRound" />
            </div>
            <div class="message-content">
              <div class="message-bubble">
                <MarkdownRenderer :content="chatStore.streamingMessage" />
              </div>
              <div class="message-time">正在输入...</div>
            </div>
          </div>
          
          <!-- 加载状态 -->
          <div v-if="chatStore.isLoading && !chatStore.streamingMessage" class="loading-message">
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            AI正在思考中...
          </div>
        </div>
      </el-main>

      <el-footer class="chat-input-area">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="2"
            placeholder="输入您的消息..."
            @keyup.enter.prevent="handleSendMessage"
            :disabled="chatStore.isLoading"
            class="message-input"
            resize="none"
          />
          <el-button
            type="primary"
            :icon="Position"
            @click="handleSendMessage"
            :disabled="!inputMessage.trim() || chatStore.isLoading"
            class="send-button"
          >
            发送
          </el-button>
        </div>
      </el-footer>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, watch, computed } from 'vue'
import { useChatStore } from '@/stores/chat'
import MarkdownRenderer from './MarkdownRenderer.vue'
import {
  Plus,
  Delete,
  User,
  ChatLineRound,
  Loading,
  Position
} from '@element-plus/icons-vue'

const chatStore = useChatStore()
const inputMessage = ref('')
const messagesContainer = ref<HTMLElement>()

const currentSession = computed(() => chatStore.currentSession || { enableThinking: false })

const formatTime = (date: Date | string) => {
  const d = new Date(date)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  
  return d.toLocaleDateString('zh-CN')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleSendMessage = async () => {
  if (!inputMessage.value.trim() || chatStore.isLoading) return
  
  const message = inputMessage.value.trim()
  inputMessage.value = ''
  
  await chatStore.sendMessage(message)
  scrollToBottom()
}

const createNewChat = () => {
  chatStore.createNewSession()
  scrollToBottom()
}

const switchSession = (sessionId: string) => {
  chatStore.switchSession(sessionId)
  scrollToBottom()
}

const deleteSession = (sessionId: string) => {
  chatStore.deleteSession(sessionId)
}

const clearCurrentChat = () => {
  chatStore.clearCurrentSession()
  scrollToBottom()
}

const handleThinkingToggle = (value: boolean) => {
  chatStore.toggleThinking(value)
}

// 监听消息变化，自动滚动到底部
watch(
  () => chatStore.currentMessages.length,
  () => {
    scrollToBottom()
  }
)

watch(
  () => chatStore.streamingMessage,
  () => {
    scrollToBottom()
  }
)
</script>

<style scoped>
.chat-container {
  display: flex;
  height: 100vh;
  background-color: #f5f5f5;
}

.sidebar {
  background-color: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid #e0e0e0;
}

.new-chat-btn {
  width: 100%;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.session-item {
  position: relative;
  padding: 12px;
  margin-bottom: 4px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.session-item:hover {
  background-color: #f5f5f5;
}

.session-item.active {
  background-color: #e3f2fd;
  border-left: 3px solid #2196f3;
}

.session-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-time {
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 4px;
}

.loading-indicator {
  color: #2196f3;
}

.loading-indicator .el-icon {
  font-size: 14px;
}

.delete-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}

.session-item:hover .delete-btn {
  opacity: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fafafa;
}

.chat-header {
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.thinking-switch {
  margin-right: 12px;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  background-color: #f8f9fa;
}

.messages-container {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 20px;
}
.message-wrapper {
  display: flex;
  margin-bottom: 24px;
  align-items: flex-start;
}

.message-wrapper.user {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 12px;
}

.message-content {
  max-width: 70%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  word-wrap: break-word;
  line-height: 1.5;
}

.message-bubble :deep(.markdown-content) {
  line-height: 1.6;
}



.message-bubble :deep(p) {
  margin: 8px 0;
}

.message-bubble :deep(ul), 
.message-bubble :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.message-bubble :deep(li) {
  margin: 4px 0;
}

/* 表格样式修复 - 确保表格在消息气泡中正确显示 */
.message-bubble :deep(.markdown-table) {
  margin: 16px 0 !important;
  width: 100% !important;
  max-width: 100% !important;
}

.message-bubble :deep(.markdown-table th),
.message-bubble :deep(.markdown-table td) {
  padding: 8px 12px !important;
  word-break: break-word !important;
  max-width: 200px !important; /* 限制单元格最大宽度 */
}

.message-bubble :deep(.table-cell-content) {
  white-space: pre-wrap !important;
  word-wrap: break-word !important;
  overflow-wrap: break-word !important;
  word-break: break-all !important;
  display: block !important;
  line-height: 1.4 !important;
}

.message-wrapper.assistant .message-bubble {
  background-color: #fff;
  border: 1px solid #e0e0e0;
  color: #333;
}

.message-wrapper.user .message-bubble {
  background-color: #2196f3;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  text-align: right;
}

.message-wrapper.user .message-time {
  text-align: left;
}

.loading-message {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #666;
  font-size: 14px;
}

.loading-message .el-icon {
  margin-right: 8px;
}

.chat-input-area {
  background-color: #fff;
  border-top: 1px solid #e0e0e0;
  padding: 12px 20px;
  height: auto;
  min-height: 80px;
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  padding: 8px 0;
}

.message-input {
  flex: 1;
  margin-bottom: 0;
}

.message-input :deep(.el-textarea__inner) {
  padding: 10px 12px;
  font-size: 14px;
  line-height: 1.4;
  border-radius: 8px;
  border: 1px solid #dcdfe6;
  transition: border-color 0.2s;
}

.message-input :deep(.el-textarea__inner:focus) {
  border-color: #2196f3;
  box-shadow: 0 0 0 2px rgba(33, 150, 243, 0.1);
}

.send-button {
  height: 40px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.send-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(33, 150, 243, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -280px;
    z-index: 1000;
    height: 100vh;
    transition: left 0.3s;
  }
  
  .sidebar.mobile-open {
    left: 0;
  }
  
  .message-content {
    max-width: 85%;
  }
}
</style>