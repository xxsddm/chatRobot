<template>
  <div class="chat-app">
    <ChatInterface />
  </div>
</template>

<script setup lang="ts">
import ChatInterface from '@/components/ChatInterface.vue'
</script>

<style scoped>
.chat-app {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}
</style>