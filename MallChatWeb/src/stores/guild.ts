import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { GuildType, ChannelType } from '@/services/guildTypes'
import guildApis from '@/services/guildApis'

export const useGuildStore = defineStore('guild', () => {
  const guilds = ref<GuildType[]>([])
  const currentGuild = ref<GuildType | null>(null)
  const currentChannel = ref<ChannelType | null>(null)
  
  const fetchGuilds = async () => {
    try {
      const data = await guildApis.getGuildList()
      guilds.value = data || []
      if (guilds.value.length > 0 && !currentGuild.value) {
        currentGuild.value = guilds.value[0]
        const textChannel = findFirstTextChannel(guilds.value[0].channels)
        if (textChannel) {
          currentChannel.value = textChannel
        }
      }
    } catch (error) {
      console.error('获取服务器列表失败', error)
      guilds.value = []
    }
  }
  
  const setCurrentGuild = (guild: GuildType) => {
    currentGuild.value = guild
    const textChannel = findFirstTextChannel(guild.channels)
    if (textChannel) {
      currentChannel.value = textChannel
    }
  }
  
  const setCurrentChannel = (channel: ChannelType) => {
    currentChannel.value = channel
  }
  
  const createGuild = async (name: string, icon?: string, description?: string, isPublic?: number) => {
    try {
      const data = await guildApis.createGuild({ name, icon, description, isPublic })
      if (data) {
        guilds.value.push(data)
        return data
      }
    } catch (error) {
      console.error('创建服务器失败', error)
    }
    return null
  }
  
  const createChannel = async (
    guildId: number,
    name: string,
    type: number,
    parentId?: number,
    topic?: string,
  ) => {
    try {
      const data = await guildApis.createChannel({
        guildId,
        name,
        type,
        parentId,
        topic,
      })
      if (data && currentGuild.value) {
        const guild = guilds.value.find((g) => g.id === guildId)
        if (guild) {
          if (parentId) {
            const parent = findChannelById(guild.channels, parentId)
            if (parent) {
              parent.children = parent.children || []
              parent.children.push(data)
            }
          } else {
            guild.channels.push(data)
          }
        }
      }
      return data
    } catch (error) {
      console.error('创建频道失败', error)
      return null
    }
  }
  
  const findFirstTextChannel = (channels: ChannelType[]): ChannelType | null => {
    if (!channels) return null
    for (const channel of channels) {
      if (channel.type === 1) {
        return channel
      }
      if (channel.children && channel.children.length > 0) {
        const found = findFirstTextChannel(channel.children)
        if (found) return found
      }
    }
    return null
  }
  
  const findChannelById = (channels: ChannelType[], id: number): ChannelType | null => {
    if (!channels) return null
    for (const channel of channels) {
      if (channel.id === id) return channel
      if (channel.children && channel.children.length > 0) {
        const found = findChannelById(channel.children, id)
        if (found) return found
      }
    }
    return null
  }
  
  return {
    guilds,
    currentGuild,
    currentChannel,
    fetchGuilds,
    setCurrentGuild,
    setCurrentChannel,
    createGuild,
    createChannel,
  }
})
