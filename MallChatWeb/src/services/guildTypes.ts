export type GuildType = {
  id: number
  name: string
  icon: string | null
  description: string | null
  ownerUid: number
  memberCount: number
  isPublic: number
  inviteCode: string
  channels: ChannelType[]
}

export type ChannelType = {
  id: number
  guildId: number
  parentId: number | null
  name: string
  type: ChannelTypeEnum
  topic: string | null
  position: number
  maxUsers: number
  roomId: number | null
  members: ChannelMemberType[]
  children: ChannelType[]
}

export type ChannelMemberType = {
  uid: number
  name: string
  avatar: string
  muted: number
  deafened: number
  speaking: number
}

export enum ChannelTypeEnum {
  CATEGORY = 0,
  TEXT = 1,
  VOICE = 2,
}

export type CreateGuildReq = {
  name: string
  icon?: string
  description?: string
  isPublic?: number
}

export type CreateChannelReq = {
  guildId: number
  parentId?: number
  name: string
  type: ChannelTypeEnum
  topic?: string
  maxUsers?: number
}
