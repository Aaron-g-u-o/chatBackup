<template>
  <div class="guild-chat-box">
    <div class="chat">
      <ChatList />
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
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  padding: 12px;
  overflow: hidden;
}

.chat {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  background-color: var(--background-secondary);
  border-radius: 8px;
  overflow: hidden;
  
  :deep(.chat-msg-list) {
    flex: 1;
    min-height: 0;
  }
  
  :deep(.chat-box) {
    flex: none;
    padding: 8px 16px;
    align-items: center;
  }
  
  :deep(.chat-wrapper) {
    flex: 1;
    margin-right: 0;
  }
}
</style>
