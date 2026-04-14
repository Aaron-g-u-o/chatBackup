import type { VoiceRoomType, CreateVoiceRoomReq } from './voiceTypes'
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
}
