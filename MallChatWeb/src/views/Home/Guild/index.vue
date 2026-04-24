<template>
  <div class="guild-layout">
    <GuildSidebar ref="guildSidebarRef" />
    <ChannelSidebar @invite="showInviteDialog" />
    <div class="main-content">
      <div class="channel-header">
        <span class="channel-name">
          <el-icon v-if="isVoiceChannel" :size="18"><Headset /></el-icon>
          <span v-else>#</span>
          {{ guildStore.currentChannel?.name || '选择频道' }}
        </span>
        <span v-if="guildStore.currentChannel?.topic" class="channel-topic">
          {{ guildStore.currentChannel.topic }}
        </span>
      </div>
      <div class="content-area">
        <template v-if="isVoiceChannel">
          <VoiceChannelRoom :channel-id="guildStore.currentChannel?.id!" />
        </template>
        <template v-else-if="currentRoomId">
          <GuildChatBox />
        </template>
        <template v-else>
          <div class="empty-channel">
            <el-icon :size="48"><ChatDotRound /></el-icon>
            <p>选择一个文字频道开始聊天</p>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Headset, ChatDotRound } from '@element-plus/icons-vue'
import { useGuildStore } from '@/stores/guild'
import { useGlobalStore } from '@/stores/global'
import { useChatStore } from '@/stores/chat'
import { useGroupStore } from '@/stores/group'
import { RoomTypeEnum } from '@/enums'
import { ChannelTypeEnum } from '@/services/guildTypes'
import GuildSidebar from '@/components/Guild/GuildSidebar.vue'
import ChannelSidebar from '@/components/Guild/ChannelSidebar.vue'
import GuildChatBox from './GuildChatBox.vue'
import VoiceChannelRoom from './VoiceChannelRoom.vue'

const guildStore = useGuildStore()
const globalStore = useGlobalStore()
const chatStore = useChatStore()
const groupStore = useGroupStore()
const guildSidebarRef = ref<InstanceType<typeof GuildSidebar>>()

const isVoiceChannel = computed(() => 
  guildStore.currentChannel?.type === ChannelTypeEnum.VOICE
)

const currentRoomId = computed(() => guildStore.currentChannel?.roomId)

watch(currentRoomId, (newRoomId, oldRoomId) => {
  if (newRoomId && !isVoiceChannel.value) {
    globalStore.currentSession.roomId = newRoomId
    globalStore.currentSession.type = RoomTypeEnum.Group
    
    if (!chatStore.messageMap.get(newRoomId)) {
      chatStore.messageMap.set(newRoomId, new Map())
    }
    
    if (oldRoomId !== newRoomId) {
      chatStore.getMsgList()
      groupStore.getGroupUserList(true)
    }
  }
}, { immediate: true })

const showInviteDialog = () => {
  if (guildSidebarRef.value) {
    guildSidebarRef.value.showInviteDialog = true
  }
}

onMounted(() => {
  guildStore.fetchGuilds()
})
</script>

<style scoped lang="scss">
.guild-layout {
  display: flex;
  height: 100%;
  width: 100%;
  background: linear-gradient(135deg, var(--color-surface-0) 0%, var(--color-surface-1) 100%);
  overflow: hidden;
  animation: fadeIn var(--transition-normal) var(--ease-out);
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: var(--color-surface-1);
  border-radius: var(--radius-2xl) 0 0 var(--radius-2xl);
  margin: var(--spacing-2) 0 var(--spacing-2) 0;
  box-shadow: 
    -4px 0 16px rgb(0 0 0 / 10%),
    inset 1px 0 0 rgb(255 255 255 / 5%);
}

.channel-header {
  display: flex;
  align-items: center;
  padding: var(--spacing-4) var(--spacing-5);
  border-bottom: 1px solid var(--color-border-primary);
  background: linear-gradient(180deg, var(--color-surface-2) 0%, var(--color-surface-1) 100%);
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgb(0 0 0 / 5%);
}

.channel-name {
  display: flex;
  align-items: center;
  gap: var(--spacing-2);
  font-weight: var(--font-weight-semibold);
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  letter-spacing: var(--letter-spacing-tight);
  
  .el-icon {
    color: var(--color-primary-400);
  }
}

.channel-topic {
  margin-left: var(--spacing-4);
  font-size: var(--font-size-sm);
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding-left: var(--spacing-4);
  border-left: 1px solid var(--color-border-primary);
}

.content-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: var(--color-surface-0);
}

.empty-channel {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-tertiary);
  gap: var(--spacing-4);
  
  .el-icon {
    color: var(--color-primary-400);
    opacity: 0.6;
    animation: float 3s ease-in-out infinite;
  }
  
  p {
    font-size: var(--font-size-base);
    font-weight: var(--font-weight-medium);
    letter-spacing: var(--letter-spacing-wide);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@media only screen and (max-width: 768px) {
  .main-content {
    border-radius: var(--radius-xl) 0 0 var(--radius-xl);
    margin: var(--spacing-1) 0 var(--spacing-1) 0;
  }
  
  .channel-header {
    padding: var(--spacing-3) var(--spacing-4);
  }
  
  .channel-topic {
    display: none;
  }
}
</style>
