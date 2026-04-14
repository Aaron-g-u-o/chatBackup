import type { GuildType, ChannelType, CreateGuildReq, CreateChannelReq } from './guildTypes'
import { alovaIns } from './request'

const prefix = import.meta.env.PROD ? import.meta.env.VITE_API_PREFIX : ''

export default {
  createGuild: (data: CreateGuildReq) =>
    alovaIns.Post<GuildType>(`${prefix}/capi/guild/create`, data),
  
  getGuildList: () =>
    alovaIns.Get<GuildType[]>(`${prefix}/capi/guild/list`),
  
  getPublicGuilds: (page = 1, pageSize = 10) =>
    alovaIns.Get<GuildType[]>(`${prefix}/capi/guild/public`, {
      params: { page, pageSize },
    }),
  
  getGuildDetail: (guildId: number) =>
    alovaIns.Get<GuildType>(`${prefix}/capi/guild/detail/${guildId}`),
  
  joinGuild: (guildId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/guild/join/${guildId}`),
  
  joinGuildByInviteCode: (inviteCode: string) =>
    alovaIns.Post<GuildType>(`${prefix}/capi/guild/join/code`, null, {
      params: { inviteCode },
    }),
  
  leaveGuild: (guildId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/guild/leave/${guildId}`),
  
  createChannel: (data: CreateChannelReq) =>
    alovaIns.Post<ChannelType>(`${prefix}/capi/guild/channel/create`, data),
  
  deleteChannel: (channelId: number) =>
    alovaIns.Delete<void>(`${prefix}/capi/guild/channel/${channelId}`),
  
  getGuildChannels: (guildId: number) =>
    alovaIns.Get<ChannelType[]>(`${prefix}/capi/guild/${guildId}/channels`),
  
  joinVoiceChannel: (channelId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/guild/channel/voice/join/${channelId}`),
  
  leaveVoiceChannel: (channelId: number) =>
    alovaIns.Post<void>(`${prefix}/capi/guild/channel/voice/leave/${channelId}`),
  
  getVoiceChannelMembers: (channelId: number) =>
    alovaIns.Get<ChannelType[]>(`${prefix}/capi/guild/channel/voice/members/${channelId}`),
}
