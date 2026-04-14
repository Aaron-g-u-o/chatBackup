<template>
  <div class="voice-chat-container">
    <div v-if="!isInRoom" class="voice-entry">
      <VoiceRoomList :room-id="roomId" @join="handleJoin" ref="roomListRef" />
    </div>
    <div v-else class="voice-active">
      <VoiceRoomInfo :room="currentRoom" />
      <VoiceControl :voice-room-id="currentRoomId!" @leave="handleLeave" />
      <audio
        v-for="[uid, stream] in remoteStreams"
        :key="uid"
        :ref="(el) => setAudioRef(uid, el as HTMLAudioElement)"
        autoplay
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import VoiceRoomList from './VoiceRoomList.vue'
import VoiceRoomInfo from './VoiceRoomInfo.vue'
import VoiceControl from './VoiceControl.vue'
import type { VoiceRoomType, VoiceSignalType, VoiceRoomUpdateType } from '@/services/voiceTypes'
import { voiceChat } from '@/utils/voiceChat'
import { voiceEventBus } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  roomId: number
}>()

const userStore = useUserStore()
const roomListRef = ref()
const isInRoom = ref(false)
const currentRoom = ref<VoiceRoomType | null>(null)
const currentRoomId = ref<number | null>(null)
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

const handleJoin = async (room: VoiceRoomType) => {
  try {
    await voiceChat.init()
    voiceChat.joinRoom(room.id, userStore.userInfo.uid!)
    
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

    currentRoom.value = room
    currentRoomId.value = room.id
    isInRoom.value = true

    for (const member of room.members || []) {
      if (member.uid !== userStore.userInfo.uid) {
        await voiceChat.createOffer(member.uid)
      }
    }
  } catch (error) {
    console.error('加入语音房间失败', error)
  }
}

const handleLeave = () => {
  isInRoom.value = false
  currentRoom.value = null
  currentRoomId.value = null
  remoteStreams.value.clear()
  audioRefs.value.clear()
  roomListRef.value?.fetchRooms()
}

const handleVoiceRoomUpdate = (update: VoiceRoomUpdateType) => {
  if (!currentRoom.value) return
  
  if (update.action === 'join' && update.member) {
    if (!currentRoom.value.members.find(m => m.uid === update.member!.uid)) {
      currentRoom.value.members.push(update.member)
      currentRoom.value.currentUserCount++
      
      if (update.member.uid !== userStore.userInfo.uid) {
        voiceChat.createOffer(update.member.uid)
      }
    }
  } else if (update.action === 'leave' && update.member) {
    const index = currentRoom.value.members.findIndex(m => m.uid === update.member!.uid)
    if (index !== -1) {
      currentRoom.value.members.splice(index, 1)
      currentRoom.value.currentUserCount--
    }
  } else if (update.action === 'status' && update.member) {
    const member = currentRoom.value.members.find(m => m.uid === update.member!.uid)
    if (member) {
      member.muted = update.member.muted
      member.deafened = update.member.deafened
      member.speaking = update.member.speaking
    }
  }
}

onMounted(() => {
  voiceEventBus.on('voiceRoomUpdate', handleVoiceRoomUpdate)
})

onUnmounted(() => {
  voiceEventBus.off('voiceRoomUpdate', handleVoiceRoomUpdate)
  if (isInRoom.value) {
    voiceChat.leaveRoom()
  }
})
</script>

<style scoped lang="scss">
.voice-chat-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.voice-entry {
  flex: 1;
  overflow-y: auto;
}

.voice-active {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
}
</style>
