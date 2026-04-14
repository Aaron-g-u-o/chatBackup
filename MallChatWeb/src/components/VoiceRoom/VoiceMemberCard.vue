<template>
  <div
    class="voice-member-card"
    :class="{
      'is-speaking': member.speaking,
      'is-muted': member.muted,
      'is-deafened': member.deafened,
      'is-offline': isOffline
    }"
    @contextmenu.prevent="handleContextMenu"
  >
    <div class="member-avatar">
      <img :src="member.avatar" :alt="member.name" />
      <div v-if="member.speaking" class="speaking-ring"></div>
      <div class="status-badge" :class="statusClass">
        <el-icon :size="10">
          <component :is="statusIcon" />
        </el-icon>
      </div>
    </div>
    <div class="member-info">
      <div class="member-name">{{ member.name }}</div>
      <div class="member-status-text">
        <template v-if="isOffline">离线</template>
        <template v-else-if="member.deafened">闭麦中</template>
        <template v-else-if="member.muted">静音中</template>
        <template v-else-if="member.speaking">正在说话</template>
        <template v-else>在线</template>
      </div>
    </div>
    <div class="member-volume" v-if="!isOffline && !member.muted">
      <div class="volume-bars">
        <div
          v-for="i in 5"
          :key="i"
          class="volume-bar"
          :class="{ active: volumeLevel >= i * 20 }"
        ></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { Microphone, Mute, Headset, Warning } from '@element-plus/icons-vue'
import type { VoiceMemberType } from '@/services/voiceTypes'
import { voiceChat } from '@/utils/voiceChat'

const props = defineProps<{
  member: VoiceMemberType
  voiceRoomId: number
}>()

const emit = defineEmits<{
  contextMenu: [event: MouseEvent, member: VoiceMemberType]
}>()

const volumeLevel = ref(0)
let audioContext: AudioContext | null = null
let analyser: AnalyserNode | null = null
let animationId: number | null = null

const isOffline = computed(() => {
  const connections = voiceChat.getPeerConnections()
  if (props.member.uid === voiceChat['uid']) return false
  const pc = connections.get(props.member.uid)
  return pc?.connectionState === 'disconnected' || pc?.connectionState === 'failed'
})

const statusClass = computed(() => {
  if (isOffline.value) return 'offline'
  if (member.deafened) return 'deafened'
  if (member.muted) return 'muted'
  if (member.speaking) return 'speaking'
  return 'online'
})

const statusIcon = computed(() => {
  if (isOffline.value) return Warning
  if (member.deafened) return Headset
  if (member.muted) return Mute
  return Microphone
})

const member = computed(() => props.member)

const handleContextMenu = (event: MouseEvent) => {
  emit('contextMenu', event, props.member)
}

const startVolumeMonitor = () => {
  const connections = voiceChat.getPeerConnections()
  const stream = connections.get(props.member.uid)
  if (!stream) return

  const remoteStream = voiceChat['remoteStreams']?.get(props.member.uid)
  if (!remoteStream) return

  try {
    audioContext = new AudioContext()
    analyser = audioContext.createAnalyser()
    const source = audioContext.createMediaStreamSource(remoteStream)
    source.connect(analyser)
    analyser.fftSize = 256

    const dataArray = new Uint8Array(analyser.frequencyBinCount)

    const updateVolume = () => {
      if (!analyser) return
      analyser.getByteFrequencyData(dataArray)
      const average = dataArray.reduce((a, b) => a + b, 0) / dataArray.length
      volumeLevel.value = Math.min(100, (average / 128) * 100)
      animationId = requestAnimationFrame(updateVolume)
    }

    updateVolume()
  } catch (error) {
    console.error('音量监控启动失败:', error)
  }
}

const stopVolumeMonitor = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  if (audioContext) {
    audioContext.close()
  }
}

onMounted(() => {
  if (!props.member.muted && props.member.uid !== voiceChat['uid']) {
    startVolumeMonitor()
  }
})

onUnmounted(() => {
  stopVolumeMonitor()
})
</script>

<style scoped lang="scss">
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
    transform: translateX(2px);
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

  &.is-offline {
    opacity: 0.4;
    filter: grayscale(1);
  }
}

.member-avatar {
  position: relative;
  width: 40px;
  height: 40px;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
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

    &.offline {
      background: #c0c4cc;
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
  font-weight: 500;
  color: var(--color-text-1);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-status-text {
  font-size: 12px;
  color: var(--color-text-3);
  margin-top: 2px;
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
  &:nth-child(3) { height: 12px; }
  &:nth-child(4) { height: 14px; }
  &:nth-child(5) { height: 16px; }

  &.active {
    background: #67c23a;
  }
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
