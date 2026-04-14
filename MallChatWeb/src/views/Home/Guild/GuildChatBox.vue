<template>
  <div class="guild-chat-box">
    <div class="message-area">
      <ChatList />
    </div>
    <div class="input-area">
      <SendBar />
    </div>
  </div>
</template>

<script setup lang="ts">
import ChatList from '@/views/Home/Chat/components/ChatList/index.vue'
import SendBar from '@/views/Home/Chat/components/ChatBox/SendBar/index.vue'
</script>

<style scoped lang="scss">
.guild-chat-box {
  flex: 1;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: var(--background-secondary);
  border-radius: 8px;
  overflow: hidden;
}

.message-area {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  padding: 20px;
}

.input-area {
  flex: 0 0 auto;
  border-top: 1px solid var(--color-border);
  background-color: var(--background-secondary);
  padding: 16px 20px;
}

:deep(.chat-msg-list) {
  height: 100%;
  min-height: 0;
}

:deep(.virtual-list) {
  height: 100%;
  min-height: 0;
}

:deep(.chat-edit) {
  background-color: var(--background-secondary);
}

:deep(.msg-input) {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 40px;
  padding: 2px 6px;
  font-size: 14px;
  color: var(--font-main);
  background-color: var(--background-3);
  border-radius: 12px;
}
</style>
