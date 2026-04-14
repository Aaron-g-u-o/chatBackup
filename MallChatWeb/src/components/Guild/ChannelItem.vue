<template>
  <div
    :class="['channel-item', { active, 'voice-channel': isVoice }]"
    @click="$emit('select', channel)"
  >
    <div class="channel-icon">
      <el-icon v-if="isVoice" :size="18"><Headset /></el-icon>
      <span v-else>#</span>
    </div>
    <span class="channel-name">{{ channel.name }}</span>
    <div v-if="isVoice && channel.members?.length" class="voice-members">
      <div
        v-for="member in channel.members.slice(0, 3)"
        :key="member.uid"
        class="member-avatar"
      >
        <img :src="member.avatar" :alt="member.name" />
      </div>
      <span v-if="channel.members.length > 3" class="more-count">
        +{{ channel.members.length - 3 }}
      </span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Headset } from '@element-plus/icons-vue'
import { ChannelTypeEnum } from '@/services/guildTypes'
import type { ChannelType } from '@/services/guildTypes'

const props = defineProps<{
  channel: ChannelType
  active: boolean
}>()

defineEmits<{
  select: [channel: ChannelType]
}>()

const isVoice = computed(() => props.channel.type === ChannelTypeEnum.VOICE)
</script>

<style scoped lang="scss">
.channel-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  margin: 1px 0;
  border-radius: 4px;
  cursor: pointer;
  color: var(--color-text-3);
  font-size: 14px;
  
  &:hover {
    background-color: var(--color-bg-3);
    color: var(--color-text-1);
  }
  
  &.active {
    background-color: var(--color-bg-4);
    color: var(--color-text-1);
  }
  
  &.voice-channel {
    .channel-icon {
      color: var(--el-color-success);
    }
  }
}

.channel-icon {
  width: 20px;
  margin-right: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.channel-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.voice-members {
  display: flex;
  align-items: center;
  
  .member-avatar {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    overflow: hidden;
    margin-left: -4px;
    border: 2px solid var(--color-bg-2);
    
    &:first-child {
      margin-left: 0;
    }
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
  .more-count {
    font-size: 10px;
    color: var(--color-text-3);
    margin-left: 4px;
  }
}
</style>
