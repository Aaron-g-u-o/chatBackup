package com.abin.mallchat.common.guild.service;

import com.abin.mallchat.common.guild.domain.vo.request.CreateChannelReq;
import com.abin.mallchat.common.guild.domain.vo.request.CreateGuildReq;
import com.abin.mallchat.common.guild.domain.vo.response.ChannelResp;
import com.abin.mallchat.common.guild.domain.vo.response.GuildResp;

import java.util.List;

public interface GuildService {
    GuildResp createGuild(Long uid, CreateGuildReq req);
    
    GuildResp getGuildDetail(Long guildId);
    
    List<GuildResp> getUserGuilds(Long uid);
    
    List<GuildResp> getPublicGuilds(Long uid, int page, int pageSize);
    
    void joinGuild(Long uid, Long guildId);
    
    GuildResp joinGuildByInviteCode(Long uid, String inviteCode);
    
    void leaveGuild(Long uid, Long guildId);
    
    ChannelResp createChannel(Long uid, CreateChannelReq req);
    
    void deleteChannel(Long uid, Long channelId);
    
    List<ChannelResp> getGuildChannels(Long guildId);
    
    void joinVoiceChannel(Long uid, Long channelId);
    
    void leaveVoiceChannel(Long uid, Long channelId);
    
    List<ChannelResp> getVoiceChannelMembers(Long channelId);
}
