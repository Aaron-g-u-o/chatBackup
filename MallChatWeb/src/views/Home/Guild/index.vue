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
  height: 100vh;
  width: 100vw;
  background-color: var(--color-bg-1);
  overflow: hidden;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.channel-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  background-color: var(--color-bg-2);
  flex-shrink: 0;
}

.channel-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text-1);
}

.channel-topic {
  margin-left: 16px;
  font-size: 13px;
  color: var(--color-text-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-area {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.empty-channel {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--color-text-3);
  
  p {
    margin-top: 16px;
    font-size: 14px;
  }
}
</style>
