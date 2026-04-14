<template>
  <div class="voice-room">
    <div class="voice-room-header">
      <span class="room-name">{{ room?.name || '语音房间' }}</span>
      <span class="member-count">{{ room?.currentUserCount || 0 }}/{{ room?.maxUsers || 10 }}</span>
    </div>
    
    <div class="voice-members">
      <div
        v-for="member in room?.members"
        :key="member.uid"
        class="voice-member"
        :class="{
          'is-speaking': member.speaking,
          'is-muted': member.muted,
          'is-deafened': member.deafened
        }"
        @contextmenu.prevent="handleContextMenu($event, member)"
      >
        <div class="member-avatar">
          <img :src="member.avatar" :alt="member.name" />
          <div v-if="member.speaking" class="speaking-indicator"></div>
          <div class="status-badge" :class="getStatusClass(member)">
            <el-icon :size="10">
              <component :is="getStatusIcon(member)" />
            </el-icon>
          </div>
        </div>
        <div class="member-info">
          <span class="member-name">{{ member.name }}</span>
          <div class="member-status">
            <el-icon v-if="member.muted" :size="14" color="#f56c6c"><Mute /></el-icon>
            <el-icon v-if="member.deafened" :size="14" color="#909399"><Headset /></el-icon>
            <span class="status-text">{{ getStatusText(member) }}</span>
          </div>
        </div>
        <div class="volume-indicator" v-if="!member.muted && member.speaking">
          <div class="volume-bar" :style="{ width: getVolumeLevel(member.uid) + '%' }"></div>
        </div>
      </div>
    </div>

    <VoiceMemberContextMenu
      v-model:show="contextMenuVisible"
      :member="selectedMember"
      :voice-room-id="room?.id || 0"
      :position="contextMenuPosition"
      @member-updated="handleMemberUpdated"
      @member-kicked="handleMemberKicked"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { VoiceMemberType, VoiceRoomType } from '@/services/voiceTypes'
import { Mute, Headset, Microphone, Warning } from '@element-plus/icons-vue'
import VoiceMemberContextMenu from './VoiceMemberContextMenu.vue'
import { useVoiceMemberStore } from '@/stores/voiceMember'

const props = defineProps<{
  room: VoiceRoomType | null
}>()

const emit = defineEmits<{
  membersUpdate: [members: VoiceMemberType[]]
}>()

const voiceMemberStore = useVoiceMemberStore()

const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedMember = ref<VoiceMemberType | null>(null)

const getStatusClass = (member: VoiceMemberType) => {
  if (member.deafened) return 'deafened'
  if (member.muted) return 'muted'
  if (member.speaking) return 'speaking'
  return 'online'
}

const getStatusIcon = (member: VoiceMemberType) => {
  if (member.deafened) return Headset
  if (member.muted) return Mute
  if (member.speaking) return Microphone
  return Microphone
}

const getStatusText = (member: VoiceMemberType) => {
  if (member.deafened) return '闭麦中'
  if (member.muted) return '静音中'
  if (member.speaking) return '正在说话'
  return '在线'
}

const getVolumeLevel = (uid: number) => {
  return voiceMemberStore.getMemberVolume(uid) || 50
}

const handleContextMenu = (event: MouseEvent, member: VoiceMemberType) => {
  selectedMember.value = member
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

const handleMemberUpdated = (updatedMember: VoiceMemberType) => {
  if (!props.room?.members) return
  
  const index = props.room.members.findIndex(m => m.uid === updatedMember.uid)
  if (index !== -1) {
    props.room.members[index] = updatedMember
    voiceMemberStore.updateMember(updatedMember.uid, updatedMember)
    emit('membersUpdate', props.room.members)
  }
}

const handleMemberKicked = (uid: number) => {
  if (!props.room?.members) return
  
  const index = props.room.members.findIndex(m => m.uid === uid)
  if (index !== -1) {
    props.room.members.splice(index, 1)
    props.room.currentUserCount--
    voiceMemberStore.removeMember(uid)
    emit('membersUpdate', props.room.members)
  }
}
</script>

<style scoped lang="scss">
.voice-room {
  background: var(--color-bg-2);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.voice-room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--color-border);

  .room-name {
    font-weight: 600;
    color: var(--color-text-1);
  }

  .member-count {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.voice-members {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.voice-member {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 6px;
  background: var(--color-bg-3);
  transition: all 0.2s;
  cursor: pointer;
  user-select: none;

  &:hover {
    background: var(--color-bg-4);
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
  width: 36px;
  height: 36px;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
  }

  .speaking-indicator {
    position: absolute;
    inset: -2px;
    border-radius: 50%;
    border: 2px solid #67c23a;
    animation: pulse 1s infinite;
  }

  .status-badge {
    position: absolute;
    right: -2px;
    bottom: -2px;
    width: 14px;
    height: 14px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 2px solid var(--color-bg-3);

    &.online {
      background: #67c23a;
      color: #fff;
    }

    &.muted {
      background: #f56c6c;
      color: #fff;
    }

    &.deafened {
      background: #909399;
      color: #fff;
    }

    &.speaking {
      background: #67c23a;
      color: #fff;
    }
  }
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  color: var(--color-text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-status {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;

  .status-text {
    font-size: 12px;
    color: var(--color-text-3);
  }
}

.volume-indicator {
  width: 40px;
  height: 4px;
  background: var(--color-bg-4);
  border-radius: 2px;
  overflow: hidden;

  .volume-bar {
    height: 100%;
    background: linear-gradient(90deg, #67c23a, #e6a23c);
    transition: width 0.1s;
  }
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
