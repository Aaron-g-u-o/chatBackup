<template>
  <div class="voice-channel-room">
    <div class="voice-header">
      <span class="channel-name">{{ channel?.name || '语音频道' }}</span>
      <el-button type="danger" size="small" @click="leaveChannel" v-if="isJoined">
        离开频道
      </el-button>
      <el-button type="primary" size="small" @click="joinChannel" v-else>
        加入频道
      </el-button>
    </div>
    
    <div class="voice-content">
      <div v-if="!isJoined" class="join-prompt">
        <el-icon :size="48"><Headset /></el-icon>
        <p>点击上方按钮加入语音频道</p>
      </div>
      
      <template v-else>
        <div class="members-grid">
          <div v-for="member in members" :key="member.uid" class="member-card">
            <div class="member-avatar">
              <img :src="member.avatar" :alt="member.name" />
              <div v-if="member.speaking" class="speaking-ring"></div>
            </div>
            <div class="member-info">
              <span class="member-name">{{ member.name }}</span>
              <div class="member-status">
                <el-icon v-if="member.muted" :size="14" color="#f56c6c"><Mute /></el-icon>
                <el-icon v-if="member.deafened" :size="14" color="#909399"><Headset /></el-icon>
              </div>
            </div>
          </div>
        </div>
        
        <VoiceControl 
          :voice-room-id="channelId" 
          :custom-leave="leaveVoiceChannel"
          :use-api="false"
          @leave="handleLeave" 
        />
      </template>
    </div>
    
    <audio
      v-for="[uid, stream] in remoteStreams"
      :key="uid"
      :ref="(el) => setAudioRef(uid, el as HTMLAudioElement)"
      autoplay
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Headset, Mute } from '@element-plus/icons-vue'
import type { ChannelType, ChannelMemberType } from '@/services/guildTypes'
import guildApis from '@/services/guildApis'
import { voiceChat } from '@/utils/voiceChat'
import { voiceEventBus } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import VoiceControl from '@/components/VoiceRoom/VoiceControl.vue'

const props = defineProps<{
  channelId: number
}>()

const userStore = useUserStore()
const channel = ref<ChannelType | null>(null)
const members = ref<ChannelMemberType[]>([])
const isJoined = ref(false)
const remoteStreams = ref<Map<number, MediaStream>>(new Map())
const audioRefs = ref<Map<number, HTMLAudioElement>>(new Map())

const setAudioRef = (uid: number, el: HTMLAudioElement | null) => {
  if (el) {
    audioRefs.value.set(uid, el)
    const stream = remoteStreams.value.get(uid)
    if (stream) {
      el.srcObject = stream
    }
  }
}

const fetchChannelInfo = async () => {
  if (!props.channelId) return
  const res = await guildApis.getVoiceChannelMembers(props.channelId)
  if (res.data && res.data.length > 0) {
    channel.value = res.data[0]
  }
}

const joinChannel = async () => {
  try {
    await guildApis.joinVoiceChannel(props.channelId)
    await voiceChat.init()
    voiceChat.joinRoom(props.channelId, userStore.userInfo.uid!)
    
    voiceChat.setCallbacks(
      (uid, stream) => {
        remoteStreams.value.set(uid, stream)
        setTimeout(() => {
          const audioEl = audioRefs.value.get(uid)
          if (audioEl) {
            audioEl.srcObject = stream
          }
        }, 0)
      },
      (uid) => {
        remoteStreams.value.delete(uid)
        audioRefs.value.delete(uid)
      },
    )
    
    isJoined.value = true
    
    for (const member of members.value) {
      if (member.uid !== userStore.userInfo.uid) {
        await voiceChat.createOffer(member.uid)
      }
    }
  } catch (error) {
    console.error('加入语音频道失败', error)
  }
}

const leaveChannel = async () => {
  await guildApis.leaveVoiceChannel(props.channelId)
  voiceChat.leaveRoom()
  isJoined.value = false
  remoteStreams.value.clear()
  audioRefs.value.clear()
}

const leaveVoiceChannel = async () => {
  await guildApis.leaveVoiceChannel(props.channelId)
}

const handleLeave = () => {
  leaveChannel()
}

watch(() => props.channelId, () => {
  if (isJoined.value) {
    leaveChannel()
  }
  fetchChannelInfo()
})

onMounted(() => {
  fetchChannelInfo()
})

onUnmounted(() => {
  if (isJoined.value) {
    leaveChannel()
  }
})
</script>

<style scoped lang="scss">
.voice-channel-room {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-1);
}

.voice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
}

.channel-name {
  font-weight: 600;
  font-size: 16px;
  color: var(--color-text-1);
}

.voice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.join-prompt {
  flex: 1;
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

.members-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 12px;
  padding: 16px;
  overflow-y: auto;
}

.member-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background-color: var(--color-bg-2);
  border-radius: 8px;
}

.member-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  margin-bottom: 8px;
  
  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }
  
  .speaking-ring {
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid var(--el-color-success);
    animation: pulse 1s infinite;
  }
}

.member-info {
  text-align: center;
}

.member-name {
  font-size: 13px;
  color: var(--color-text-1);
  margin-bottom: 4px;
}

.member-status {
  display: flex;
  justify-content: center;
  gap: 4px;
}

@keyframes pulse {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}
</style>
