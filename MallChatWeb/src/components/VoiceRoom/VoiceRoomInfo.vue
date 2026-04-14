<template>
  <div class="voice-room">
    <div class="voice-room-header">
      <span class="room-name">{{ room?.name || '语音房间' }}</span>
      <span class="member-count">{{ room?.currentUserCount || 0 }}/{{ room?.maxUsers || 10 }}</span>
    </div>
    <div class="voice-members">
      <div v-for="member in room?.members" :key="member.uid" class="voice-member">
        <div class="member-avatar">
          <img :src="member.avatar" :alt="member.name" />
          <div v-if="member.speaking" class="speaking-indicator"></div>
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
  </div>
</template>

<script setup lang="ts">
import type { VoiceRoomType } from '@/services/voiceTypes'
import { Mute, Headset } from '@element-plus/icons-vue'

defineProps<{
  room: VoiceRoomType | null
}>()
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
  transition: background 0.2s;

  &:hover {
    background: var(--color-bg-4);
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
}

.member-info {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-name {
  font-size: 14px;
  color: var(--color-text-1);
}

.member-status {
  display: flex;
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
