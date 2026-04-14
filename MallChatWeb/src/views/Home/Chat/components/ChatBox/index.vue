<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGlobalStore } from '@/stores/global'
import { RoomTypeEnum } from '@/enums'
import { Headset } from '@element-plus/icons-vue'

import UserList from '../UserList/index.vue'
import ChatList from '../ChatList/index.vue'
import SendBar from './SendBar/index.vue'
import VoiceRoom from '@/components/VoiceRoom/index.vue'

const isSelect = ref(false)
const showVoicePanel = ref(false)
const globalStore = useGlobalStore()
const currentSession = computed(() => globalStore.currentSession)
</script>

<template>
  <div class="chat-box">
    <div class="chat-wrapper">
      <template v-if="isSelect">
        <ElIcon :size="160" color="var(--font-light)"><IEpChatDotRound /></ElIcon>
      </template>
      <div v-else class="chat">
        <ChatList />
        <SendBar />
      </div>
    </div>
    <div class="voice-entry-btn" @click="showVoicePanel = true">
      <el-icon :size="20"><Headset /></el-icon>
      <span>语音</span>
    </div>
    <UserList v-show="currentSession.type === RoomTypeEnum.Group" />
    
    <el-drawer
      v-model="showVoicePanel"
      title="语音聊天"
      direction="rtl"
      size="320px"
      :with-header="true"
    >
      <VoiceRoom :room-id="currentSession.roomId" />
    </el-drawer>
  </div>
</template>

<style lang="scss" src="./styles.scss" scoped />
