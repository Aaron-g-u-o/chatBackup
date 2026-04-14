<script setup lang="ts">
import { ref, computed } from 'vue'
import { useGlobalStore } from '@/stores/global'
import { RoomTypeEnum } from '@/enums'

import UserList from '../UserList/index.vue'
import ChatList from '../ChatList/index.vue'
import SendBar from './SendBar/index.vue'
import KookChannelView from '@/components/KookLike/ChannelView.vue'
import { useChannelStore } from '@/stores/channel'

const isSelect = ref(false)
const globalStore = useGlobalStore()
const currentSession = computed(() => globalStore.currentSession)
const channelStore = useChannelStore()

const selectedChannelRoom = computed(() => channelStore.selectedRoom)
</script>

<template>
  <div class="chat-box">
    <div class="chat-wrapper">
      <template v-if="selectedChannelRoom && selectedChannelRoom.type === 'text'">
        <KookChannelView :channelId="Number(selectedChannelRoom.remote_id ?? selectedChannelRoom.id)" />
      </template>

      <template v-else>
        <div class="chat">
          <ChatList />
          <SendBar />
        </div>
      </template>
    </div>
    <UserList v-show="currentSession.type === RoomTypeEnum.Group" />
  </div>
</template>

<style lang="scss" src="./styles.scss" scoped />
