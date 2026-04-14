<template>
  <ContextMenu
    v-model:show="visible"
    :options="menuOptions"
    @close="handleClose"
  >
    <ContextMenuItem @click="handleAdjustVolume">
      <template #icon>
        <el-icon><Headset /></el-icon>
      </template>
      <span>调整音量</span>
    </ContextMenuItem>

    <ContextMenuItem @click="handleToggleMute">
      <template #icon>
        <el-icon>
          <component :is="member?.muted ? Microphone : Mute" />
        </el-icon>
      </template>
      <span>{{ member?.muted ? '取消静音' : '静音' }}</span>
    </ContextMenuItem>

    <ContextMenuItem @click="handleViewStatus">
      <template #icon>
        <el-icon><Connection /></el-icon>
      </template>
      <span>查看连接状态</span>
    </ContextMenuItem>

    <ContextMenuItem @click="handleKickMember" :disabled="isSelf">
      <template #icon>
        <el-icon><Remove /></el-icon>
      </template>
      <span>移出语音频道</span>
    </ContextMenuItem>
  </ContextMenu>

  <el-dialog
    v-model="showVolumeDialog"
    title="调整用户音量"
    width="360px"
    :close-on-click-modal="false"
  >
    <div class="volume-dialog-content">
      <div class="user-info">
        <img :src="member?.avatar || defaultAvatar" :alt="member?.name" class="avatar" />
        <span class="name">{{ member?.name }}</span>
      </div>
      <div class="volume-control">
        <el-icon :size="20"><Mute /></el-icon>
        <el-slider
          v-model="volumeValue"
          :min="0"
          :max="200"
          :format-tooltip="(val: number) => `${val}%`"
          @change="handleVolumeChange"
        />
        <el-icon :size="20"><Microphone /></el-icon>
      </div>
      <div class="volume-label">当前音量: {{ volumeValue }}%</div>
    </div>
  </el-dialog>

  <el-dialog
    v-model="showStatusDialog"
    title="语音连接状态"
    width="400px"
  >
    <div class="status-dialog-content" v-if="connectionStatus">
      <div class="user-info">
        <img :src="member?.avatar || defaultAvatar" :alt="member?.name" class="avatar" />
        <span class="name">{{ member?.name }}</span>
      </div>
      <div class="status-details">
        <div class="status-item">
          <span class="label">连接状态</span>
          <span class="value" :class="connectionStatus.connectionState">
            {{ connectionStateText }}
          </span>
        </div>
        <div class="status-item">
          <span class="label">网络延迟</span>
          <span class="value">{{ connectionStatus.latency }}ms</span>
        </div>
        <div class="status-item">
          <span class="label">通话质量</span>
          <span class="value" :class="connectionStatus.quality">
            {{ qualityText }}
          </span>
        </div>
        <div class="status-item">
          <span class="label">最后更新</span>
          <span class="value">{{ formatTime(connectionStatus.lastUpdateTime) }}</span>
        </div>
      </div>
      <el-button type="primary" @click="refreshStatus" :loading="loadingStatus">
        刷新状态
      </el-button>
    </div>
    <el-empty v-else description="无法获取连接状态" />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Microphone, Mute, Headset, Connection, Remove } from '@element-plus/icons-vue'
import { ContextMenu, ContextMenuItem, type MenuOptions } from '@imengyu/vue3-context-menu'
import type { VoiceMemberType, VoiceConnectionStateType } from '@/services/voiceTypes'
import voiceApis from '@/services/voiceApis'
import { useUserStore } from '@/stores/user'

const props = defineProps<{
  show: boolean
  member: VoiceMemberType | null
  voiceRoomId: number
  position: { x: number; y: number }
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  'memberUpdated': [member: VoiceMemberType]
  'memberKicked': [uid: number]
}>()

const userStore = useUserStore()
const visible = computed({
  get: () => props.show,
  set: (val) => emit('update:show', val)
})

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIxOCIgZmlsbD0iI2UzZTNlMyIvPjxwYXRoIGQ9Ik0yMCAyN2M1LjUgMCAxMCA0LjUgMTAgMTBjMCA1LjUtNC41IDEwLTEwIDEwcy0xMC00LjUtMTAtMTBjMC01LjUgNC41LTEwIDEwLTEweiIgZmlsbD0iI2IzYjNiMyIvPjwvc3ZnPg=='

const isSelf = computed(() => props.member?.uid === userStore.userInfo?.uid)

const menuOptions = computed<MenuOptions>(() => ({
  theme: 'dark',
  x: props.position.x,
  y: props.position.y,
  zIndex: 9999
}))

const showVolumeDialog = ref(false)
const volumeValue = ref(100)
const showStatusDialog = ref(false)
const connectionStatus = ref<VoiceConnectionStateType | null>(null)
const loadingStatus = ref(false)

const connectionStateText = computed(() => {
  if (!connectionStatus.value) return ''
  const stateMap: Record<string, string> = {
    'new': '新建连接',
    'connecting': '连接中',
    'connected': '已连接',
    'disconnected': '已断开',
    'failed': '连接失败',
    'closed': '已关闭'
  }
  return stateMap[connectionStatus.value.connectionState] || connectionStatus.value.connectionState
})

const qualityText = computed(() => {
  if (!connectionStatus.value) return ''
  const qualityMap: Record<string, string> = {
    'excellent': '优秀',
    'good': '良好',
    'poor': '较差',
    'unknown': '未知'
  }
  return qualityMap[connectionStatus.value.quality] || connectionStatus.value.quality
})

const handleClose = () => {
  emit('update:show', false)
}

const handleAdjustVolume = () => {
  showVolumeDialog.value = true
  emit('update:show', false)
}

const handleVolumeChange = async (value: number) => {
  if (!props.member) return
  
  try {
    console.log('调整音量:', { voiceRoomId: props.voiceRoomId, targetUid: props.member.uid, volume: value })
    const res = await voiceApis.setMemberVolume(props.voiceRoomId, props.member.uid, value)
    console.log('调整音量响应:', res)
    
    if (res?.success) {
      ElMessage.success('音量已调整')
    } else {
      ElMessage.error(res?.message || '调整音量失败')
    }
  } catch (error) {
    console.error('调整音量失败:', error)
    ElMessage.error('调整音量失败，请重试')
  }
}

const handleToggleMute = async () => {
  if (!props.member) return
  
  try {
    const api = props.member.muted ? voiceApis.unmuteMember : voiceApis.muteMember
    const action = props.member.muted ? '取消静音' : '静音'
    
    console.log(`${action}用户:`, { voiceRoomId: props.voiceRoomId, targetUid: props.member.uid })
    const res = await api(props.voiceRoomId, props.member.uid)
    console.log(`${action}响应:`, res)
    
    if (res?.success) {
      const updatedMember = res.member || {
        ...props.member,
        muted: props.member.muted ? 0 : 1
      }
      emit('memberUpdated', updatedMember)
      ElMessage.success(props.member.muted ? '已取消静音' : '已静音')
    } else {
      ElMessage.error(res?.message || '操作失败')
    }
  } catch (error) {
    console.error('静音操作失败:', error)
    ElMessage.error('操作失败，请重试')
  }
  
  emit('update:show', false)
}

const handleViewStatus = async () => {
  if (!props.member) return
  
  showStatusDialog.value = true
  emit('update:show', false)
  await refreshStatus()
}

const refreshStatus = async () => {
  if (!props.member) return
  
  loadingStatus.value = true
  try {
    console.log('获取连接状态:', { voiceRoomId: props.voiceRoomId, targetUid: props.member.uid })
    const res = await voiceApis.getMemberConnectionStatus(props.voiceRoomId, props.member.uid)
    console.log('连接状态响应:', res)
    connectionStatus.value = res || null
  } catch (error) {
    console.error('获取连接状态失败:', error)
    ElMessage.error('获取连接状态失败')
    connectionStatus.value = null
  } finally {
    loadingStatus.value = false
  }
}

const handleKickMember = async () => {
  if (!props.member || isSelf.value) return
  
  try {
    console.log('移出用户:', { voiceRoomId: props.voiceRoomId, targetUid: props.member.uid })
    const res = await voiceApis.kickMember(props.voiceRoomId, props.member.uid)
    console.log('移出用户响应:', res)
    
    if (res?.success) {
      emit('memberKicked', props.member.uid)
      ElMessage.success('已将用户移出语音频道')
    } else {
      ElMessage.error(res?.message || '移出失败')
    }
  } catch (error) {
    console.error('移出用户失败:', error)
    ElMessage.error('移出用户失败，请重试')
  }
  
  emit('update:show', false)
}

const formatTime = (timestamp: number) => {
  return new Date(timestamp).toLocaleString('zh-CN')
}

watch(() => props.show, (val) => {
  if (val) {
    volumeValue.value = 100
    connectionStatus.value = null
  }
})
</script>

<style scoped lang="scss">
.volume-dialog-content {
  padding: 16px 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;

  .avatar {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    object-fit: cover;
    background: var(--color-bg-3);
  }

  .name {
    font-size: 16px;
    font-weight: 500;
    color: var(--color-text-1);
  }
}

.volume-control {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;

  .el-slider {
    flex: 1;
  }
}

.volume-label {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: var(--color-text-2);
}

.status-dialog-content {
  padding: 16px 0;
}

.status-details {
  margin: 16px 0;
  padding: 16px;
  background: var(--color-bg-2);
  border-radius: 8px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;

  &:not(:last-child) {
    border-bottom: 1px solid var(--color-border);
  }

  .label {
    color: var(--color-text-2);
    font-size: 14px;
  }

  .value {
    font-weight: 500;
    font-size: 14px;

    &.connected {
      color: #67c23a;
    }

    &.disconnected, &.failed, &.closed {
      color: #f56c6c;
    }

    &.connecting, &.new {
      color: #e6a23c;
    }

    &.excellent {
      color: #67c23a;
    }

    &.good {
      color: #409eff;
    }

    &.poor {
      color: #f56c6c;
    }

    &.unknown {
      color: #909399;
    }
  }
}
</style>
