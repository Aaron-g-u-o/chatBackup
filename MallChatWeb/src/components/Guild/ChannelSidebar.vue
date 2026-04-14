<template>
  <div class="channel-sidebar">
    <div class="guild-header">
      <span class="guild-name">{{ guildStore.currentGuild?.name || '选择服务器' }}</span>
      <el-dropdown trigger="click" @command="handleCommand">
        <el-icon :size="18" class="dropdown-icon"><ArrowDown /></el-icon>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="createChannel">创建频道</el-dropdown-item>
            <el-dropdown-item command="invite">邀请成员</el-dropdown-item>
            <el-dropdown-item command="settings">服务器设置</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
    
    <div class="channel-list">
      <template v-for="channel in guildStore.currentGuild?.channels" :key="channel.id">
        <ChannelCategory
          v-if="channel.type === ChannelTypeEnum.CATEGORY"
          :channel="channel"
          @select="selectChannel"
          @create="showCreateChannelDialog"
        />
        <ChannelItem
          v-else
          :channel="channel"
          :active="guildStore.currentChannel?.id === channel.id"
          @select="selectChannel"
        />
      </template>
    </div>
    
    <el-dialog v-model="showCreateChannel" title="创建频道" width="400px">
      <el-form :model="channelForm" label-width="80px">
        <el-form-item label="频道类型">
          <el-radio-group v-model="channelForm.type">
            <el-radio :label="1">文字频道</el-radio>
            <el-radio :label="2">语音频道</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="频道名称">
          <el-input v-model="channelForm.name" placeholder="请输入频道名称" />
        </el-form-item>
        <el-form-item label="频道主题">
          <el-input v-model="channelForm.topic" placeholder="频道主题（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateChannel = false">取消</el-button>
        <el-button type="primary" @click="handleCreateChannel" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { useGuildStore } from '@/stores/guild'
import { ChannelTypeEnum } from '@/services/guildTypes'
import type { ChannelType } from '@/services/guildTypes'
import ChannelCategory from './ChannelCategory.vue'
import ChannelItem from './ChannelItem.vue'

const guildStore = useGuildStore()

const emit = defineEmits<{
  invite: []
}>()

const showCreateChannel = ref(false)
const creating = ref(false)
const channelForm = ref({
  name: '',
  type: 1 as ChannelTypeEnum,
  topic: '',
  parentId: undefined as number | undefined,
})

const selectChannel = (channel: ChannelType) => {
  if (channel.type !== ChannelTypeEnum.CATEGORY) {
    guildStore.setCurrentChannel(channel)
  }
}

const handleCommand = (command: string) => {
  if (command === 'createChannel') {
    channelForm.value = {
      name: '',
      type: ChannelTypeEnum.TEXT,
      topic: '',
      parentId: undefined,
    }
    showCreateChannel.value = true
  } else if (command === 'invite') {
    emit('invite')
  }
}

const showCreateChannelDialog = (parentId: number, type: ChannelTypeEnum) => {
  channelForm.value = {
    name: '',
    type,
    topic: '',
    parentId,
  }
  showCreateChannel.value = true
}

const handleCreateChannel = async () => {
  if (!channelForm.value.name || !guildStore.currentGuild) return
  
  creating.value = true
  try {
    await guildStore.createChannel(
      guildStore.currentGuild.id,
      channelForm.value.name,
      channelForm.value.type,
      channelForm.value.parentId,
      channelForm.value.topic,
    )
    showCreateChannel.value = false
    channelForm.value = {
      name: '',
      type: ChannelTypeEnum.TEXT,
      topic: '',
      parentId: undefined,
    }
  } finally {
    creating.value = false
  }
}
</script>

<style scoped lang="scss">
.channel-sidebar {
  width: 240px;
  height: 100%;
  background-color: var(--color-bg-2);
  display: flex;
  flex-direction: column;
}

.guild-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  cursor: pointer;
  
  &:hover {
    background-color: var(--color-bg-3);
  }
}

.guild-name {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-text-1);
}

.dropdown-icon {
  color: var(--color-text-2);
}

.channel-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
</style>
