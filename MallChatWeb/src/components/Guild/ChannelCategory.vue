<template>
  <div class="channel-category">
    <div class="category-header" @click="expanded = !expanded">
      <el-icon :size="12" class="expand-icon" :class="{ expanded }">
        <ArrowRight />
      </el-icon>
      <span class="category-name">{{ channel.name }}</span>
      <el-icon
        :size="14"
        class="add-icon"
        @click.stop="$emit('create', channel.id, ChannelTypeEnum.TEXT)"
      >
        <Plus />
      </el-icon>
    </div>
    <div v-show="expanded" class="category-children">
      <ChannelItem
        v-for="child in channel.children"
        :key="child.id"
        :channel="child"
        :active="false"
        @select="$emit('select', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ArrowRight, Plus } from '@element-plus/icons-vue'
import { ChannelTypeEnum } from '@/services/guildTypes'
import type { ChannelType } from '@/services/guildTypes'

defineProps<{
  channel: ChannelType
}>()

defineEmits<{
  select: [channel: ChannelType]
  create: [parentId: number, type: ChannelTypeEnum]
}>()

const expanded = ref(true)
</script>

<style scoped lang="scss">
.channel-category {
  margin-bottom: 4px;
}

.category-header {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  cursor: pointer;
  color: var(--color-text-3);
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  
  &:hover {
    color: var(--color-text-1);
    
    .add-icon {
      opacity: 1;
    }
  }
}

.expand-icon {
  margin-right: 4px;
  transition: transform 0.2s;
  
  &.expanded {
    transform: rotate(90deg);
  }
}

.category-name {
  flex: 1;
}

.add-icon {
  opacity: 0;
  transition: opacity 0.2s;
}

.category-children {
  padding-left: 8px;
}
</style>
