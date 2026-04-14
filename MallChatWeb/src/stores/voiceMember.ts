import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { VoiceMemberType, VoiceRoomType, VoiceConnectionStateType } from '@/services/voiceTypes'
import voiceApis from '@/services/voiceApis'

export const useVoiceMemberStore = defineStore('voiceMember', () => {
  const currentRoom = ref<VoiceRoomType | null>(null)
  const members = ref<Map<number, VoiceMemberType>>(new Map())
  const memberVolumes = ref<Map<number, number>>(new Map())
  const memberConnectionStates = ref<Map<number, VoiceConnectionStateType>>(new Map())
  const loading = ref(false)
  const error = ref<string | null>(null)
  const lastUpdateTime = ref<number>(Date.now())

  const onlineMembers = computed(() => {
    return Array.from(members.value.values())
  })

  const memberCount = computed(() => members.value.size)

  const setCurrentRoom = (room: VoiceRoomType | null) => {
    currentRoom.value = room
    if (room?.members) {
      members.value.clear()
      room.members.forEach(member => {
        members.value.set(member.uid, member)
      })
    }
    lastUpdateTime.value = Date.now()
  }

  const updateMember = (uid: number, updates: Partial<VoiceMemberType>) => {
    const member = members.value.get(uid)
    if (member) {
      members.value.set(uid, { ...member, ...updates })
      lastUpdateTime.value = Date.now()
    }
  }

  const addMember = (member: VoiceMemberType) => {
    members.value.set(member.uid, member)
    if (currentRoom.value) {
      currentRoom.value.currentUserCount = members.value.size
    }
    lastUpdateTime.value = Date.now()
  }

  const removeMember = (uid: number) => {
    members.value.delete(uid)
    memberVolumes.value.delete(uid)
    memberConnectionStates.value.delete(uid)
    if (currentRoom.value) {
      currentRoom.value.currentUserCount = members.value.size
    }
    lastUpdateTime.value = Date.now()
  }

  const setMemberVolume = (uid: number, volume: number) => {
    memberVolumes.value.set(uid, volume)
  }

  const getMemberVolume = (uid: number): number => {
    return memberVolumes.value.get(uid) ?? 100
  }

  const setMemberConnectionState = (uid: number, state: VoiceConnectionStateType) => {
    memberConnectionStates.value.set(uid, state)
  }

  const getMemberConnectionState = (uid: number): VoiceConnectionStateType | undefined => {
    return memberConnectionStates.value.get(uid)
  }

  const fetchMemberVolumes = async (voiceRoomId: number) => {
    try {
      const res = await voiceApis.getMemberVolumes(voiceRoomId)
      if (res) {
        res.forEach(item => {
          memberVolumes.value.set(item.uid, item.volume)
        })
      }
    } catch (err) {
      console.error('获取成员音量失败:', err)
    }
  }

  const fetchMemberConnectionStatus = async (voiceRoomId: number, uid: number) => {
    try {
      const res = await voiceApis.getMemberConnectionStatus(voiceRoomId, uid)
      if (res) {
        memberConnectionStates.value.set(uid, res)
        return res
      }
    } catch (err) {
      console.error('获取成员连接状态失败:', err)
    }
    return undefined
  }

  const adjustMemberVolume = async (voiceRoomId: number, uid: number, volume: number) => {
    loading.value = true
    error.value = null
    try {
      const res = await voiceApis.setMemberVolume(voiceRoomId, uid, volume)
      if (res?.success) {
        setMemberVolume(uid, volume)
        return true
      } else {
        error.value = res?.message || '调整音量失败'
        return false
      }
    } catch (err) {
      error.value = '调整音量失败，请重试'
      return false
    } finally {
      loading.value = false
    }
  }

  const muteMember = async (voiceRoomId: number, uid: number) => {
    loading.value = true
    error.value = null
    try {
      const res = await voiceApis.muteMember(voiceRoomId, uid)
      if (res?.success) {
        updateMember(uid, { muted: 1 })
        if (res.member) {
          updateMember(uid, res.member)
        }
        return true
      } else {
        error.value = res?.message || '静音失败'
        return false
      }
    } catch (err) {
      error.value = '静音失败，请重试'
      return false
    } finally {
      loading.value = false
    }
  }

  const unmuteMember = async (voiceRoomId: number, uid: number) => {
    loading.value = true
    error.value = null
    try {
      const res = await voiceApis.unmuteMember(voiceRoomId, uid)
      if (res?.success) {
        updateMember(uid, { muted: 0 })
        if (res.member) {
          updateMember(uid, res.member)
        }
        return true
      } else {
        error.value = res?.message || '取消静音失败'
        return false
      }
    } catch (err) {
      error.value = '取消静音失败，请重试'
      return false
    } finally {
      loading.value = false
    }
  }

  const kickMember = async (voiceRoomId: number, uid: number) => {
    loading.value = true
    error.value = null
    try {
      const res = await voiceApis.kickMember(voiceRoomId, uid)
      if (res?.success) {
        removeMember(uid)
        return true
      } else {
        error.value = res?.message || '移出失败'
        return false
      }
    } catch (err) {
      error.value = '移出失败，请重试'
      return false
    } finally {
      loading.value = false
    }
  }

  const clearError = () => {
    error.value = null
  }

  const reset = () => {
    currentRoom.value = null
    members.value.clear()
    memberVolumes.value.clear()
    memberConnectionStates.value.clear()
    loading.value = false
    error.value = null
    lastUpdateTime.value = Date.now()
  }

  return {
    currentRoom,
    members,
    memberVolumes,
    memberConnectionStates,
    loading,
    error,
    lastUpdateTime,
    onlineMembers,
    memberCount,
    setCurrentRoom,
    updateMember,
    addMember,
    removeMember,
    setMemberVolume,
    getMemberVolume,
    setMemberConnectionState,
    getMemberConnectionState,
    fetchMemberVolumes,
    fetchMemberConnectionStatus,
    adjustMemberVolume,
    muteMember,
    unmuteMember,
    kickMember,
    clearError,
    reset
  }
})
