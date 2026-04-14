<template>
  <div class="voice-member-manager">
    <div class="member-list-header">
      <span class="title">在线成员</span>
      <span class="count">{{ onlineMembers.length }}/{{ room?.currentUserCount || 0 }}</span>
    </div>
    
    <div class="member-list" v-if="onlineMembers.length > 0">
      <VoiceMemberCard
        v-for="member in onlineMembers"
        :key="member.uid"
        :member="member"
        :voice-room-id="voiceRoomId"
        @context-menu="handleMemberContextMenu"
      />
    </div>
    <el-empty
      v-else
      description="暂无在线成员"
      :image-size="80"
      class="empty-state"
    />

    <VoiceMemberContextMenu
      v-model:show="contextMenuVisible"
      :member="selectedMember"
      :voice-room-id="voiceRoomId"
      :position="contextMenuPosition"
      @member-updated="handleMemberUpdated"
      @member-kicked="handleMemberKicked"
    />

    <el-dialog
      v-model="showKickConfirm"
      title="移出确认"
      width="360px"
    >
      <div class="kick-confirm-content">
        <el-icon :size="40" color="#f56c6c"><Warning /></el-icon>
        <p>确定要将 <strong>{{ selectedMember?.name }}</strong> 移出语音频道吗？</p>
      </div>
      <template #footer>
        <el-button @click="showKickConfirm = false">取消</el-button>
        <el-button type="danger" @click="confirmKick">确定移出</el-button>
      </template>
    </el-dialog>

    <el-alert
      v-if="errorMessage"
      :title="errorMessage"
      type="error"
      show-icon
      closable
      @close="clearError"
      class="error-toast"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { VoiceRoomType, VoiceMemberType } from '@/services/voiceTypes'
import VoiceMemberCard from './VoiceMemberCard.vue'
import VoiceMemberContextMenu from './VoiceMemberContextMenu.vue'
import voiceApis from '@/services/voiceApis'

const props = defineProps<{
  room: VoiceRoomType | null
  voiceRoomId: number
}>()

const emit = defineEmits<{
  membersUpdate: [members: VoiceMemberType[]]
}>()

const contextMenuVisible = ref(false)
const contextMenuPosition = ref({ x: 0, y: 0 })
const selectedMember = ref<VoiceMemberType | null>(null)
const showKickConfirm = ref(false)
const errorMessage = ref('')

const onlineMembers = computed(() => {
  return props.room?.members?.filter(m => m) || []
})

const handleMemberContextMenu = (event: MouseEvent, member: VoiceMemberType) => {
  selectedMember.value = member
  contextMenuPosition.value = { x: event.clientX, y: event.clientY }
  contextMenuVisible.value = true
}

const handleMemberUpdated = (updatedMember: VoiceMemberType) => {
  if (!props.room?.members) return
  
  const index = props.room.members.findIndex(m => m.uid === updatedMember.uid)
  if (index !== -1) {
    props.room.members[index] = updatedMember
    emit('membersUpdate', props.room.members)
  }
}

const handleMemberKicked = (uid: number) => {
  if (!props.room?.members) return
  
  const index = props.room.members.findIndex(m => m.uid === uid)
  if (index !== -1) {
    props.room.members.splice(index, 1)
    props.room.currentUserCount--
    emit('membersUpdate', props.room.members)
  }
}

const confirmKick = async () => {
  if (!selectedMember.value) return
  
  try {
    const res = await voiceApis.kickMember(props.voiceRoomId, selectedMember.value.uid)
    
    if (res?.success) {
      handleMemberKicked(selectedMember.value.uid)
      ElMessage.success(`已将 ${selectedMember.value.name} 移出语音频道`)
    } else {
      showError(res?.message || '移出失败')
    }
  } catch (error) {
    console.error('移出用户失败:', error)
    showError('移出失败，请重试')
  } finally {
    showKickConfirm.value = false
  }
}

const showError = (message: string) => {
  errorMessage.value = message
  setTimeout(() => {
    clearError()
  }, 5000)
}

const clearError = () => {
  errorMessage.value = ''
}

watch(() => props.room, () => {
  selectedMember.value = null
  contextMenuVisible.value = false
}, { deep: true })
</script>

<style scoped lang="scss">
.voice-member-manager {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.member-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);

  .title {
    font-weight: 600;
    color: var(--color-text-1);
    font-size: 14px;
  }

  .count {
    font-size: 12px;
    color: var(--color-text-3);
    background: var(--color-bg-3);
    padding: 2px 8px;
    border-radius: 10px;
  }
}

.member-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kick-confirm-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 20px 0;

  p {
    font-size: 14px;
    color: var(--color-text-2);
    text-align: center;
    margin: 0;

    strong {
      color: var(--color-text-1);
    }
  }
}

.error-toast {
  position: fixed;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  max-width: 400px;
}
</style>
