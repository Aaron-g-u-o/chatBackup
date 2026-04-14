<template>
  <div class="voice-control">
    <div class="control-buttons">
      <el-tooltip :content="muted ? '取消静音' : '静音'" placement="top">
        <el-button
          :type="muted ? 'danger' : 'default'"
          circle
          :icon="muted ? Mute : Microphone"
          @click="toggleMute"
        />
      </el-tooltip>
      <el-tooltip :content="deafened ? '取消闭麦' : '闭麦'" placement="top">
        <el-button
          :type="deafened ? 'warning' : 'default'"
          circle
          :icon="Headset"
          @click="toggleDeafen"
        />
      </el-tooltip>
      <el-tooltip content="离开语音" placement="top">
        <el-button type="danger" circle :icon="PhoneFilled" @click="handleLeave" />
      </el-tooltip>
    </div>
    <div class="volume-indicator">
      <div class="volume-bar" :style="{ width: volumeLevel + '%' }"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Microphone, Mute, Headset, PhoneFilled } from '@element-plus/icons-vue'
import { voiceChat } from '@/utils/voiceChat'
import voiceApis from '@/services/voiceApis'

const props = defineProps<{
  voiceRoomId: number
  useApi?: boolean
}>()

const emit = defineEmits<{
  leave: []
}>()

const muted = ref(false)
const deafened = ref(false)
const volumeLevel = ref(0)
let audioContext: AudioContext | null = null
let analyser: AnalyserNode | null = null
let animationId: number | null = null

onMounted(() => {
  startVolumeMonitor()
})

onUnmounted(() => {
  stopVolumeMonitor()
})

const toggleMute = async () => {
  muted.value = !muted.value
  voiceChat.setMuted(muted.value)
  if (props.useApi !== false && props.voiceRoomId) {
    try {
      await voiceApis.updateStatus(props.voiceRoomId, { muted: muted.value })
    } catch (error) {
      console.error('更新静音状态失败:', error)
    }
  }
}

const toggleDeafen = async () => {
  deafened.value = !deafened.value
  voiceChat.setDeafened(deafened.value)
  if (props.useApi !== false && props.voiceRoomId) {
    try {
      await voiceApis.updateStatus(props.voiceRoomId, { deafened: deafened.value })
    } catch (error) {
      console.error('更新闭麦状态失败:', error)
    }
  }
}

const handleLeave = () => {
  console.log('VoiceControl: 触发离开事件')
  emit('leave')
}

const startVolumeMonitor = () => {
  const stream = voiceChat.getLocalStream()
  if (!stream) return

  try {
    audioContext = new AudioContext()
    analyser = audioContext.createAnalyser()
    const source = audioContext.createMediaStreamSource(stream)
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
    console.error('启动音量监控失败:', error)
  }
}

const stopVolumeMonitor = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
  analyser = null
}
</script>

<style scoped lang="scss">
.voice-control {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--color-bg-2);
  border-radius: 8px;
}

.control-buttons {
  display: flex;
  gap: 12px;
}

.volume-indicator {
  width: 100%;
  height: 4px;
  background: var(--color-bg-3);
  border-radius: 2px;
  overflow: hidden;

  .volume-bar {
    height: 100%;
    background: linear-gradient(90deg, #67c23a, #e6a23c);
    transition: width 0.1s;
  }
}
</style>
