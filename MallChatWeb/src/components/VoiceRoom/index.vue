<template>
  <div class="voice-chat-container">
    <div v-if="!isInRoom" class="voice-entry">
      <VoiceRoomList :room-id="roomId" @join="handleJoin" ref="roomListRef" />
    </div>
    <div v-else class="voice-active">
      <div class="voice-room-header">
        <div class="room-info">
          <span class="room-name">{{ currentRoom?.name || '语音房间' }}</span>
          <span class="member-count">{{ currentRoom?.currentUserCount || 0 }}/{{ currentRoom?.maxUsers || 10 }}</span>
        </div>
        <div class="room-actions">
          <el-button size="small" @click="refreshRoomMembers" :loading="refreshing">
            <el-icon><Refresh /></el-icon>
            刷新成员
          </el-button>
        </div>
      </div>
      
      <div class="voice-members-list" v-if="displayMembers.length > 0">
        <div
          v-for="member in displayMembers"
          :key="member.uid"
          class="voice-member-card"
          :class="{
            'is-speaking': member.speaking,
            'is-muted': member.muted,
            'is-deafened': member.deafened,
            'is-self': member.uid === userStore.userInfo?.uid
          }"
          @contextmenu.prevent="handleContextMenu($event, member)"
        >
          <div class="member-avatar">
            <img 
              :src="member.avatar || defaultAvatar" 
              :alt="member.name"
              @error="handleAvatarError($event)"
            />
            <div v-if="member.speaking" class="speaking-ring"></div>
            <div class="status-badge" :class="getStatusClass(member)">
              <el-icon :size="8">
                <component :is="getStatusIcon(member)" />
              </el-icon>
            </div>
          </div>
          <div class="member-info">
            <div class="member-name">
              {{ member.name }}
              <span v-if="member.uid === userStore.userInfo?.uid" class="self-tag">(你)</span>
            </div>
            <div class="member-status">
              <el-icon v-if="member.muted" :size="12" color="#f56c6c"><Mute /></el-icon>
              <el-icon v-else-if="member.deafened" :size="12" color="#909399"><Headset /></el-icon>
              <span class="status-text">{{ getStatusText(member) }}</span>
            </div>
          </div>
          <div class="member-volume" v-if="!member.muted && member.speaking">
            <div class="volume-bars">
              <div
                v-for="i in 5"
                :key="i"
                class="volume-bar"
                :class="{ active: getVolumeLevel(member.uid) >= i * 20 }"
              ></div>
            </div>
          </div>
        </div>
      </div>
      
      <el-empty
        v-else
        description="暂无在线成员"
        :image-size="80"
        class="empty-state"
      />

      <VoiceControl :voice-room-id="currentRoomId!" @leave="handleLeave" />
      
      <audio
        v-for="[uid, stream] in remoteStreams"
        :key="uid"
        :ref="(el) => setAudioRef(uid, el as HTMLAudioElement)"
        autoplay
      />
    </div>

    <VoiceMemberContextMenu
      v-model:show="contextMenuVisible"
      :member="selectedMember"
      :voice-room-id="currentRoomId || 0"
      :position="contextMenuPosition"
      @member-updated="handleMemberUpdated"
      @member-kicked="handleMemberKicked"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed, nextTick } from 'vue'
import { Refresh, Mute, Headset, Microphone, Warning } from '@element-plus/icons-vue'
import VoiceRoomList from './VoiceRoomList.vue'
import VoiceControl from './VoiceControl.vue'
import VoiceMemberContextMenu from './VoiceMemberContextMenu.vue'
import type { VoiceRoomType, VoiceRoomUpdateType, VoiceMemberType } from '@/services/voiceTypes'
import { voiceChat } from '@/utils/voiceChat'
import { voiceEventBus } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import { useVoiceMemberStore } from '@/stores/voiceMember'
import voiceApis from '@/services/voiceApis'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  roomId: number
}>()

const userStore = useUserStore()
const voiceMemberStore = useVoiceMemberStore()
const roomListRef = ref()
const isInRoom = ref(false)
const currentRoom = ref<VoiceRoomType | null>(null)
const currentRoomId = ref<number | null>(null)
const remoteStreams = ref<Map<number, MediaStream>>(new Map())
const audioRefs = ref<Map<number, HTMLAudioElement>>(new Map())
const memberVolumes = ref<Map<number, number>>(new Map())
const refreshing = ref(false)
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedMember = ref<VoiceMemberType | null>(null)

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIxOCIgZmlsbD0iI2UzZTNlMyIvPjxwYXRoIGQ9Ik0yMCAyN2M1LjUgMCAxMCA0LjUgMTAgMTBjMCA1LjUtNC41IDEwLTEwIDEwcy0xMC00LjUtMTAtMTBjMC01LjUgNC41LTEwIDEwLTEweiIgZmlsbD0iI2IzYjNiMyIvPjwvc3ZnPg=='

const displayMembers = computed(() => {
  if (!currentRoom.value?.members) {
    return []
  }
  
  const members = [...currentRoom.value.members]
  const currentUserUid = userStore.userInfo?.uid
  
  if (currentUserUid && !members.find(m => m.uid === currentUserUid)) {
    members.push({
      uid: currentUserUid,
      name: userStore.userInfo?.name || '我',
      avatar: userStore.userInfo?.avatar || '',
      muted: voiceChat.getMuted() ? 1 : 0,
      deafened: voiceChat.getDeafened() ? 1 : 0,
      speaking: 0
    })
  }
  
  return members
})

const setAudioRef = (uid: number, el: HTMLAudioElement | null) => {
  if (el) {
    audioRefs.value.set(uid, el)
    const stream = remoteStreams.value.get(uid)
    if (stream) {
      el.srcObject = stream
      const volume = memberVolumes.value.get(uid) ?? 100
      el.volume = volume / 100
    }
  }
}

const handleAvatarError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = defaultAvatar
}

const getStatusClass = (member: VoiceMemberType) => {
  if (member.deafened) return 'deafened'
  if (member.muted) return 'muted'
  if (member.speaking) return 'speaking'
  return 'online'
}

const getStatusIcon = (member: VoiceMemberType) => {
  if (member.deafened) return Headset
  if (member.muted) return Mute
  return Microphone
}

const getStatusText = (member: VoiceMemberType) => {
  if (member.deafened) return '闭麦中'
  if (member.muted) return '静音中'
  if (member.speaking) return '正在说话'
  return '在线'
}

const getVolumeLevel = (uid: number) => {
  return memberVolumes.value.get(uid) || 30
}

const handleContextMenu = (event: MouseEvent, member: VoiceMemberType) => {
  selectedMember.value = member
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

const handleMemberUpdated = (updatedMember: VoiceMemberType) => {
  if (!currentRoom.value?.members) return
  
  const index = currentRoom.value.members.findIndex(m => m.uid === updatedMember.uid)
  if (index !== -1) {
    currentRoom.value.members[index] = updatedMember
    voiceMemberStore.updateMember(updatedMember.uid, updatedMember)
  }
}

const handleMemberKicked = (uid: number) => {
  if (!currentRoom.value?.members) return
  
  const index = currentRoom.value.members.findIndex(m => m.uid === uid)
  if (index !== -1) {
    currentRoom.value.members.splice(index, 1)
    currentRoom.value.currentUserCount--
    voiceMemberStore.removeMember(uid)
  }
}

const handleJoin = async (room: VoiceRoomType) => {
  try {
    ElMessage.info('正在加入语音房间...')
    
    await voiceChat.init()
    voiceChat.joinRoom(room.id, userStore.userInfo.uid!)
    
    voiceChat.setCallbacks(
      (uid, stream) => {
        remoteStreams.value.set(uid, stream)
        nextTick(() => {
          const audioEl = audioRefs.value.get(uid)
          if (audioEl) {
            audioEl.srcObject = stream
            audioEl.play().catch((e: Error) => {
              console.warn(`自动播放被阻止，uid=${uid}:`, e.message)
            })
          }
        })
      },
      (uid) => {
        remoteStreams.value.delete(uid)
        audioRefs.value.delete(uid)
      },
    )

    currentRoom.value = room
    currentRoomId.value = room.id
    isInRoom.value = true

    voiceMemberStore.setCurrentRoom(room)
    loadMemberVolumes(room.id)

    for (const member of room.members || []) {
      if (member.uid !== userStore.userInfo.uid) {
        await voiceChat.createOffer(member.uid)
      }
    }

    ElMessage.success('已加入语音房间')
    
    setTimeout(() => {
      refreshRoomMembers()
    }, 500)
  } catch (error) {
    console.error('加入语音房间失败', error)
    ElMessage.error('加入语音房间失败，请重试')
  }
}

const refreshRoomMembers = async () => {
  if (!currentRoomId.value) return
  
  refreshing.value = true
  try {
    const res = await voiceApis.getRoomDetail(currentRoomId.value)
    if (res) {
      currentRoom.value = res
      voiceMemberStore.setCurrentRoom(res)
      ElMessage.success('成员列表已刷新')
    }
  } catch (error) {
    console.error('刷新成员列表失败', error)
    ElMessage.error('刷新失败，请重试')
  } finally {
    refreshing.value = false
  }
}

const loadMemberVolumes = async (voiceRoomId: number) => {
  try {
    const res = await voiceApis.getMemberVolumes(voiceRoomId)
    if (res) {
      res.forEach(item => {
        memberVolumes.value.set(item.uid, item.volume)
        voiceMemberStore.setMemberVolume(item.uid, item.volume)
        
        const audioEl = audioRefs.value.get(item.uid)
        if (audioEl) {
          audioEl.volume = item.volume / 100
        }
      })
    }
  } catch (error) {
    console.warn('加载成员音量失败，使用默认值', error)
  }
}

const handleLeave = () => {
  isInRoom.value = false
  currentRoom.value = null
  currentRoomId.value = null
  remoteStreams.value.clear()
  audioRefs.value.clear()
  memberVolumes.value.clear()
  voiceMemberStore.reset()
  roomListRef.value?.fetchRooms()
}

const handleVoiceRoomUpdate = (update: VoiceRoomUpdateType) => {
  if (!currentRoom.value) return
  
  if (update.action === 'join' && update.member) {
    if (!currentRoom.value.members.find(m => m.uid === update.member!.uid)) {
      currentRoom.value.members.push(update.member)
      currentRoom.value.currentUserCount++
      voiceMemberStore.addMember(update.member)
      
      if (update.member.uid !== userStore.userInfo.uid) {
        voiceChat.createOffer(update.member.uid)
      }
    }
  } else if (update.action === 'leave' && update.member) {
    const index = currentRoom.value.members.findIndex(m => m.uid === update.member!.uid)
    if (index !== -1) {
      currentRoom.value.members.splice(index, 1)
      currentRoom.value.currentUserCount--
      voiceMemberStore.removeMember(update.member.uid)
      
      voiceChat.closePeerConnection(update.member.uid)
      remoteStreams.value.delete(update.member.uid)
      audioRefs.value.delete(update.member.uid)
    }
  } else if (update.action === 'status' && update.member) {
    const member = currentRoom.value.members.find(m => m.uid === update.member!.uid)
    if (member) {
      member.muted = update.member.muted
      member.deafened = update.member.deafened
      member.speaking = update.member.speaking
      voiceMemberStore.updateMember(member.uid, {
        muted: member.muted,
        deafened: member.deafened,
        speaking: member.speaking
      })
    }
  }
}

watch(memberVolumes, (newVolumes) => {
  newVolumes.forEach((volume, uid) => {
    const audioEl = audioRefs.value.get(uid)
    if (audioEl) {
      audioEl.volume = volume / 100
    }
  })
}, { deep: true })

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
  overflow: hidden;
}

.voice-room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: var(--color-bg-2);
  border-radius: 8px;

  .room-info {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .room-name {
    font-weight: 600;
    color: var(--color-text-1);
    font-size: 16px;
  }

  .member-count {
    font-size: 12px;
    color: var(--color-text-3);
    background: var(--color-bg-3);
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.voice-members-list {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px;
}

.voice-member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--color-bg-3);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;

  &:hover {
    background: var(--color-bg-4);
  }

  &.is-self {
    border: 1px solid var(--el-color-primary-light-5);
    background: var(--el-color-primary-light-9);
  }

  &.is-speaking {
    background: rgba(103, 194, 58, 0.1);
    border: 1px solid rgba(103, 194, 58, 0.3);
  }

  &.is-muted {
    opacity: 0.7;
  }

  &.is-deafened {
    opacity: 0.5;
  }
}

.member-avatar {
  position: relative;
  width: 44px;
  height: 44px;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    background: var(--color-bg-4);
  }

  .speaking-ring {
    position: absolute;
    inset: -3px;
    border-radius: 50%;
    border: 2px solid #67c23a;
    animation: speaking-pulse 1s ease-in-out infinite;
  }

  .status-badge {
    position: absolute;
    right: -2px;
    bottom: -2px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--color-bg-3);

    &.online { background: #67c23a; color: #fff; }
    &.muted { background: #f56c6c; color: #fff; }
    &.deafened { background: #909399; color: #fff; }
    &.speaking { background: #67c23a; color: #fff; }
  }
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;

  .self-tag {
    font-size: 12px;
    color: var(--el-color-primary);
    font-weight: normal;
  }
}

.member-status {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;

  .status-text {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.member-volume {
  flex-shrink: 0;
}

.volume-bars {
  display: flex;
  gap: 2px;
  align-items: flex-end;
  height: 16px;
}

.volume-bar {
  width: 3px;
  background: var(--color-bg-4);
  border-radius: 1px;
  transition: background 0.1s;

  &:nth-child(1) { height: 4px; }
  &:nth-child(2) { height: 8px; }
  &:nth-child(3) { height: 10px; }
  &:nth-child(4) { height: 12px; }
  &:nth-child(5) { height: 16px; }

  &.active {
    background: #67c23a;
  }
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

@keyframes speaking-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(1.1);
  }
}
</style>
