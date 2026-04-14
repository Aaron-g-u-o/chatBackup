import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface ChannelRoom {
  id?: number | string
  remote_id?: number | string
  type?: string
}

export const useChannelStore = defineStore('channel', () => {
  const selectedRoom = ref<ChannelRoom | null>(null)

  const setSelectedRoom = (room: ChannelRoom | null) => {
    selectedRoom.value = room
  }

  const clearSelectedRoom = () => {
    selectedRoom.value = null
  }

  return {
    selectedRoom,
    setSelectedRoom,
    clearSelectedRoom,
  }
})
