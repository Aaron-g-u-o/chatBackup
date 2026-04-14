<template>
  <div class="voice-room-list">
    <div class="list-header">
      <span>语音房间</span>
      <el-button type="primary" size="small" @click="showCreateDialog = true">
        创建房间
      </el-button>
    </div>
    <div class="room-items">
      <div
        v-for="room in rooms"
        :key="room.id"
        class="room-item"
        :class="{ active: currentRoomId === room.id }"
        @click="joinRoom(room)"
      >
        <div class="room-icon">
          <el-icon :size="20"><Headset /></el-icon>
        </div>
        <div class="room-info">
          <div class="room-name">{{ room.name }}</div>
          <div class="room-meta">
            <span>{{ room.currentUserCount }}/{{ room.maxUsers }}</span>
            <span v-if="room.members?.length" class="avatars">
              <img
                v-for="member in room.members.slice(0, 3)"
                :key="member.uid"
                :src="member.avatar"
                :alt="member.name"
              />
              <span v-if="room.members.length > 3">+{{ room.members.length - 3 }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showCreateDialog" title="创建语音房间" width="400px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="房间名称">
          <el-input v-model="createForm.name" placeholder="请输入房间名称" />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="createForm.maxUsers" :min="2" :max="99" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="createRoom">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Headset } from '@element-plus/icons-vue'
import type { VoiceRoomType } from '@/services/voiceTypes'
import voiceApis from '@/services/voiceApis'

const props = defineProps<{
  roomId: number
}>()

const emit = defineEmits<{
  join: [room: VoiceRoomType]
}>()

const rooms = ref<VoiceRoomType[]>([])
const currentRoomId = ref<number | null>(null)
const showCreateDialog = ref(false)
const createForm = ref({
  name: '',
  maxUsers: 10,
})

const fetchRooms = async () => {
  const res = await voiceApis.getRoomList()
  rooms.value = res.data || []
}

const joinRoom = async (room: VoiceRoomType) => {
  try {
    const res = await voiceApis.joinRoom(room.id)
    currentRoomId.value = room.id
    emit('join', res.data)
  } catch (error) {
    console.error('加入语音房间失败', error)
  }
}

const createRoom = async () => {
  if (!createForm.value.name) return
  
  try {
    const res = await voiceApis.createRoom({
      roomId: props.roomId,
      name: createForm.value.name,
      maxUsers: createForm.value.maxUsers,
    })
    rooms.value.unshift(res.data)
    showCreateDialog.value = false
    createForm.value.name = ''
  } catch (error) {
    console.error('创建语音房间失败', error)
  }
}

onMounted(() => {
  fetchRooms()
})

defineExpose({
  fetchRooms,
})
</script>

<style scoped lang="scss">
.voice-room-list {
  padding: 12px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--color-text-1);
}

.room-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.room-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background: var(--color-bg-2);
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-bg-3);
  }

  &.active {
    background: var(--el-color-primary-light-9);
    border: 1px solid var(--el-color-primary-light-5);
  }
}

.room-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-3);
  border-radius: 8px;
  color: var(--el-color-primary);
}

.room-info {
  flex: 1;
}

.room-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-1);
  margin-bottom: 4px;
}

.room-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--color-text-3);

  .avatars {
    display: flex;
    align-items: center;

    img {
      width: 20px;
      height: 20px;
      border-radius: 50%;
      margin-left: -4px;
      border: 1px solid var(--color-bg-2);

      &:first-child {
        margin-left: 0;
      }
    }

    span {
      margin-left: 4px;
    }
  }
}
</style>
