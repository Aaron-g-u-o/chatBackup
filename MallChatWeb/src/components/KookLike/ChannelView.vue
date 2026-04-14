<template>
  <div class="kook-channel-view">
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
.kook-channel-view {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 20px;
  height: 100%;
  box-sizing: border-box;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background-color: var(--background-secondary);
  border-radius: 8px;
}

:deep(.chat-msg-list) {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

:deep(.chat-edit) {
  flex-shrink: 0;
  box-sizing: border-box;
  padding: 8px 16px;
}

:deep(.room-name) {
  flex-shrink: 0;
}
</style>
