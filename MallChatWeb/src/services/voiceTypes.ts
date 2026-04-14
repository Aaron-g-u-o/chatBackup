export type VoiceRoomType = {
  id: number
  name: string
  roomId: number
  creatorUid: number
  maxUsers: number
  currentUserCount: number
  members: VoiceMemberType[]
}

export type VoiceMemberType = {
  uid: number
  name: string
  avatar: string
  muted: number
  deafened: number
  speaking: number
}

export type VoiceSignalType = {
  type: number
  voiceRoomId: number
  fromUid?: number
  targetUid?: number
  sdp?: string
  candidate?: string
}

export type VoiceRoomUpdateType = {
  voiceRoomId: number
  action: 'join' | 'leave' | 'status'
  member: VoiceMemberType
  members?: VoiceMemberType[]
}

export enum VoiceSignalTypeEnum {
  OFFER = 1,
  ANSWER = 2,
  CANDIDATE = 3,
}

export type CreateVoiceRoomReq = {
  roomId: number
  name: string
  maxUsers?: number
}
