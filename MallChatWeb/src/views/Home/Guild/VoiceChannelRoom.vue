<template>
  <div class="voice-channel-room">
    <div class="voice-content">
      <div v-if="!isJoined" class="join-prompt">
        <div class="join-card">
          <el-icon :size="64" color="var(--el-color-primary)"><Headset /></el-icon>
          <h3>{{ channel?.name || '语音频道' }}</h3>
          <p>点击下方按钮加入语音频道，与其他成员实时通话</p>
          <el-button type="primary" size="large" @click="joinChannel" :loading="joining">
            <el-icon><Headset /></el-icon>
            加入语音频道
          </el-button>
        </div>
      </div>

      <template v-else>
        <div class="voice-room-header">
          <div class="room-info">
            <el-icon :size="20" color="var(--el-color-primary)"><Headset /></el-icon>
            <span class="room-name">{{ channel?.name || '语音频道' }}</span>
            <span class="member-count">{{ displayMembers.length }} 人在线</span>
          </div>
          <div class="room-actions">
            <el-button size="small" @click="refreshMembers" :loading="refreshing">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button type="danger" size="small" @click="leaveChannel">
              <el-icon><SwitchButton /></el-icon>
              离开频道
            </el-button>
          </div>
        </div>

        <div class="members-list" v-if="displayMembers.length > 0">
          <div
            v-for="member in displayMembers"
            :key="member.uid"
            class="member-card"
            :class="{
              'is-speaking': member.speaking,
              'is-muted': member.muted,
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
                <el-icon :size="10">
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
          </div>
        </div>

        <el-empty v-else description="暂无在线成员" :image-size="80" class="empty-state" />

        <div class="voice-control-bar">
          <VoiceControl
            :voice-room-id="channelId"
            :use-api="true"
            @leave="handleVoiceControlLeave"
          />
        </div>
      </template>
    </div>

    <audio
      v-for="[uid, stream] in remoteStreams"
      :key="uid"
      :ref="(el) => setAudioRef(uid, el as HTMLAudioElement)"
      autoplay
    />

    <VoiceMemberContextMenu
      v-model:show="contextMenuVisible"
      :member="selectedMember"
      :voice-room-id="channelId"
      :position="contextMenuPosition"
      @member-updated="handleMemberUpdated"
      @member-kicked="handleMemberKicked"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Headset, Mute, Microphone, Refresh, SwitchButton } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { ChannelType, ChannelMemberType } from '@/services/guildTypes'
import type { VoiceMemberType, VoiceRoomUpdateType } from '@/services/voiceTypes'
import guildApis from '@/services/guildApis'
import { voiceChat } from '@/utils/voiceChat'
import { voiceEventBus } from '@/utils/websocket'
import { useUserStore } from '@/stores/user'
import VoiceControl from '@/components/VoiceRoom/VoiceControl.vue'
import VoiceMemberContextMenu from '@/components/VoiceRoom/VoiceMemberContextMenu.vue'

const props = defineProps<{
  channelId: number
}>()

const userStore = useUserStore()
const channel = ref<ChannelType | null>(null)
const members = ref<ChannelMemberType[]>([])
const isJoined = ref(false)
const joining = ref(false)
const refreshing = ref(false)
const remoteStreams = ref<Map<number, MediaStream>>(new Map())
const audioRefs = ref<Map<number, HTMLAudioElement>>(new Map())
const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedMember = ref<VoiceMemberType | null>(null)
let isLeaving = false

const defaultAvatar = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48Y2lyY2xlIGN4PSIyMCIgY3k9IjIwIiByPSIxOCIgZmlsbD0iI2UzZTNlMyIvPjxwYXRoIGQ9Ik0yMCAyN2M1LjUgMCAxMCA0LjUgMTAgMTBjMCA1LjUtNC41IDEwLTEwIDEwcy0xMC00LjUtMTAtMTBjMC01LjUgNC41LTEwIDEwLTEweiIgZmlsbD0iI2IzYjNiMyIvPjwvc3ZnPg=='

const displayMembers = computed(() => {
  const result: VoiceMemberType[] = []
  const seenUids = new Set<number>()

  members.value.forEach(m => {
    if (!seenUids.has(m.uid)) {
      seenUids.add(m.uid)
      result.push({
        uid: m.uid,
        name: m.name,
        avatar: m.avatar,
        muted: m.muted ? 1 : 0,
        deafened: m.deafened ? 1 : 0,
        speaking: m.speaking ? 1 : 0
      })
    }
  })

  const currentUserUid = userStore.userInfo?.uid
  if (currentUserUid && !seenUids.has(currentUserUid)) {
    result.push({
      uid: currentUserUid,
      name: userStore.userInfo?.name || '我',
      avatar: userStore.userInfo?.avatar || '',
      muted: voiceChat.getMuted() ? 1 : 0,
      deafened: voiceChat.getDeafened() ? 1 : 0,
      speaking: 0
    })
  }

  return result
})

const setAudioRef = (uid: number, el: HTMLAudioElement | null) => {
  if (el) {
    audioRefs.value.set(uid, el)
    const stream = remoteStreams.value.get(uid)
    if (stream) {
      el.srcObject = stream
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

const handleContextMenu = (event: MouseEvent, member: VoiceMemberType) => {
  selectedMember.value = member
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

const handleMemberUpdated = (updatedMember: VoiceMemberType) => {
  const index = members.value.findIndex(m => m.uid === updatedMember.uid)
  if (index !== -1) {
    members.value[index] = {
      ...members.value[index],
      muted: updatedMember.muted ? 1 : 0,
      deafened: updatedMember.deafened ? 1 : 0,
      speaking: updatedMember.speaking ? 1 : 0
    }
  }
}

const handleMemberKicked = (uid: number) => {
  const index = members.value.findIndex(m => m.uid === uid)
  if (index !== -1) {
    members.value.splice(index, 1)
  }
}

const fetchChannelInfo = async () => {
  debugger
  if (!props.channelId) return
  try {
    const res = await guildApis.getVoiceChannelMembers(props.channelId)
    console.log('=== 获取语音频道成员 ===')
    console.log('API返回数据:', JSON.stringify(res, null, 2))
    console.log('当前用户UID:', userStore.userInfo?.uid)

    if (res) {
      if (Array.isArray(res) && res.length > 0) {
        const channelData = res[0]
        channel.value = channelData

        const allMembers: ChannelMemberType[] = []
        const seenUids = new Set<number>()

        res.forEach((ch: ChannelType) => {
          if (ch.members && Array.isArray(ch.members)) {
            ch.members.forEach((m: ChannelMemberType) => {
              if (!seenUids.has(m.uid)) {
                seenUids.add(m.uid)
                allMembers.push(m)
              }
            })
          }
        })

        console.log('合并后成员列表:', allMembers)
        console.log('合并后成员数量:', allMembers.length)

        members.value = allMembers
      } else if (res.members) {
        channel.value = res as unknown as ChannelType
        console.log('频道members字段:', res.members)
        const uniqueMembers: ChannelMemberType[] = []
        const seenUids = new Set<number>()
        ;(res.members || []).forEach((m: ChannelMemberType) => {
          if (!seenUids.has(m.uid)) {
            seenUids.add(m.uid)
            uniqueMembers.push(m)
          }
        })
        members.value = uniqueMembers
      } else {
        console.warn('API返回数据缺少members字段')
        members.value = []
      }
    } else {
      console.warn('API返回数据为空')
      members.value = []
    }
  } catch (error) {
    console.error('获取频道信息失败', error)
    members.value = []
  }
}

const handleVoiceRoomUpdate = (update: VoiceRoomUpdateType) => {
  if (update.voiceRoomId !== props.channelId) return

  console.log('收到语音房间更新:', update)

  if (update.action === 'join' && update.member) {
    const existingIndex = members.value.findIndex(m => m.uid === update.member!.uid)
    if (existingIndex === -1) {
      members.value.push({
        uid: update.member.uid,
        name: update.member.name,
        avatar: update.member.avatar,
        muted: update.member.muted,
        deafened: update.member.deafened,
        speaking: update.member.speaking
      })

      if (update.member.uid !== userStore.userInfo.uid) {
        voiceChat.createOffer(update.member.uid)
      }

      ElMessage.info(`${update.member.name} 加入了语音频道`)
    }
  } else if (update.action === 'leave' && update.member) {
    const index = members.value.findIndex(m => m.uid === update.member!.uid)
    if (index !== -1) {
      const leavingMember = members.value[index]
      members.value.splice(index, 1)
      ElMessage.info(`${leavingMember.name} 离开了语音频道`)
    }
  } else if (update.action === 'status' && update.member) {
    const member = members.value.find(m => m.uid === update.member!.uid)
    if (member) {
      member.muted = update.member.muted
      member.deafened = update.member.deafened
      member.speaking = update.member.speaking
    }
  }

  if (update.members) {
    const uniqueMembers: ChannelMemberType[] = []
    const seenUids = new Set<number>()
    update.members.forEach(m => {
      if (!seenUids.has(m.uid)) {
        seenUids.add(m.uid)
        uniqueMembers.push({
          uid: m.uid,
          name: m.name,
          avatar: m.avatar,
          muted: m.muted,
          deafened: m.deafened,
          speaking: m.speaking
        })
      }
    })
    members.value = uniqueMembers
  }
}

const joinChannel = async () => {
  joining.value = true
  try {
    ElMessage.info('正在加入语音频道...')

    await voiceChat.init()
    voiceChat.joinRoom(props.channelId, userStore.userInfo.uid!)

    voiceChat.setCallbacks(
      (uid, stream) => {
        console.log('收到远程音频流:', uid)
        remoteStreams.value.set(uid, stream)
        setTimeout(() => {
          const audioEl = audioRefs.value.get(uid)
          if (audioEl) {
            audioEl.srcObject = stream
          }
        }, 0)
      },
      (uid) => {
        console.log('远程音频流断开:', uid)
        remoteStreams.value.delete(uid)
        audioRefs.value.delete(uid)
      },
    )

    await guildApis.joinVoiceChannel(props.channelId)
    isJoined.value = true

    await fetchChannelInfo()

    for (const member of members.value) {
      if (member.uid !== userStore.userInfo.uid) {
        console.log('向成员创建Offer:', member.uid, member.name)
        await voiceChat.createOffer(member.uid)
      }
    }

    voiceEventBus.on('voiceRoomUpdate', handleVoiceRoomUpdate)

    ElMessage.success(`已加入语音频道，当前 ${displayMembers.value.length} 人在线`)
  } catch (error) {
    console.error('加入语音频道失败', error)
    ElMessage.error('加入语音频道失败，请重试')
  } finally {
    joining.value = false
  }
}

const refreshMembers = async () => {
  refreshing.value = true
  try {
    await fetchChannelInfo()
    ElMessage.success('成员列表已刷新')
  } catch (error) {
    console.error('刷新成员列表失败', error)
  } finally {
    refreshing.value = false
  }
}

const leaveChannel = async () => {
  if (isLeaving) return
  isLeaving = true

  try {
    console.log('离开语音频道...')

    await guildApis.leaveVoiceChannel(props.channelId)
    voiceChat.leaveRoom()

    voiceEventBus.off('voiceRoomUpdate', handleVoiceRoomUpdate)

    isJoined.value = false
    remoteStreams.value.clear()
    audioRefs.value.clear()
    members.value = []

    ElMessage.success('已离开语音频道')
  } catch (error) {
    console.error('离开语音频道失败', error)
    ElMessage.error('离开语音频道失败')
  } finally {
    isLeaving = false
  }
}

const handleVoiceControlLeave = () => {
  leaveChannel()
}

watch(() => props.channelId, (newId, oldId) => {
  if (oldId && isJoined.value) {
    leaveChannel()
  }
  if (newId) {
    fetchChannelInfo()
  }
}, { immediate: true })

onMounted(() => {
  fetchChannelInfo()
})

onUnmounted(() => {
  voiceEventBus.off('voiceRoomUpdate', handleVoiceRoomUpdate)
  if (isJoined.value) {
    voiceChat.leaveRoom()
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

.voice-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.join-prompt {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.join-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px;
  background: var(--color-bg-2);
  border-radius: 16px;
  max-width: 400px;
  text-align: center;

  h3 {
    margin: 16px 0 8px;
    font-size: 20px;
    color: var(--color-text-1);
  }

  p {
    margin-bottom: 24px;
    font-size: 14px;
    color: var(--color-text-3);
  }
}

.voice-room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-bg-2);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;

  .room-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .room-name {
    font-weight: 600;
    font-size: 16px;
    color: var(--color-text-1);
  }

  .member-count {
    font-size: 12px;
    color: var(--color-text-3);
    background: var(--color-bg-3);
    padding: 2px 8px;
    border-radius: 10px;
  }

  .room-actions {
    display: flex;
    gap: 8px;
  }
}

.members-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  align-content: start;
}

.member-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-2);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    background: var(--color-bg-3);
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
}

.member-avatar {
  position: relative;
  width: 48px;
  height: 48px;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    object-fit: cover;
    background: var(--color-bg-3);
  }

  .speaking-ring {
    position: absolute;
    inset: -3px;
    border-radius: 50%;
    border: 2px solid #67c23a;
    animation: pulse 1s ease-in-out infinite;
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
    border: 2px solid var(--color-bg-2);

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

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-control-bar {
  padding: 12px 16px;
  background: var(--color-bg-2);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

@keyframes pulse {
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
