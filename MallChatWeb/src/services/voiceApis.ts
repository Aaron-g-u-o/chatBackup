import type { VoiceRoomType, CreateVoiceRoomReq, VoiceMemberVolumeType, VoiceConnectionStateType, VoiceMemberManageResultType } from './voiceTypes'
import { alovaIns } from './request'

const prefix = import.meta.env.PROD ? import.meta.env.VITE_API_PREFIX : ''

export default {
  createRoom: (data: CreateVoiceRoomReq) =>
    alovaIns.Post<VoiceRoomType>(`${prefix}/capi/voice/room/create`, data),
  
  joinRoom: (voiceRoomId: number) =>
    alovaIns.Post<VoiceRoomType>(`${prefix}/capi/voice/room/join/${voiceRoomId}`),
  
  leaveRoom: (voiceRoomId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/voice/room/leave/${voiceRoomId}`),
  
  getRoomList: () =>
    alovaIns.Get<VoiceRoomType[]>(`${prefix}/capi/voice/room/list`),
  
  getRoomDetail: (voiceRoomId: number) =>
    alovaIns.Get<VoiceRoomType>(`${prefix}/capi/voice/room/detail/${voiceRoomId}`),
  
  updateStatus: (voiceRoomId: number, params: { muted?: boolean; deafened?: boolean; speaking?: boolean }) =>
    alovaIns.Post<void>(`${prefix}/capi/voice/room/status`, null, {
      params: { voiceRoomId, ...params },
    }),

  setMemberVolume: (voiceRoomId: number, targetUid: number, volume: number) =>
    alovaIns.Post<VoiceMemberManageResultType>(`${prefix}/capi/voice/room/member/volume`, null, {
      params: { voiceRoomId, targetUid, volume },
    }),

  muteMember: (voiceRoomId: number, targetUid: number) =>
    alovaIns.Post<VoiceMemberManageResultType>(`${prefix}/capi/voice/room/member/mute`, null, {
      params: { voiceRoomId, targetUid },
    }),

  unmuteMember: (voiceRoomId: number, targetUid: number) =>
    alovaIns.Post<VoiceMemberManageResultType>(`${prefix}/capi/voice/room/member/unmute`, null, {
      params: { voiceRoomId, targetUid },
    }),

  kickMember: (voiceRoomId: number, targetUid: number) =>
    alovaIns.Post<VoiceMemberManageResultType>(`${prefix}/capi/voice/room/member/kick`, null, {
      params: { voiceRoomId, targetUid },
    }),

  getMemberConnectionStatus: (voiceRoomId: number, targetUid: number) =>
    alovaIns.Get<VoiceConnectionStateType>(`${prefix}/capi/voice/room/member/status`, {
      params: { voiceRoomId, targetUid },
    }),

  getMemberVolumes: (voiceRoomId: number) =>
    alovaIns.Get<VoiceMemberVolumeType[]>(`${prefix}/capi/voice/room/member/volumes`, {
      params: { voiceRoomId },
    }),
}
