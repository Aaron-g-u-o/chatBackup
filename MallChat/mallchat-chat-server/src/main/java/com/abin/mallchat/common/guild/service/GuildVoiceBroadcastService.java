package com.abin.mallchat.common.guild.service;

import com.abin.mallchat.common.guild.domain.vo.response.ChannelMemberResp;

public interface GuildVoiceBroadcastService {
    
    void broadcastVoiceChannelUpdate(Long channelId, String action, ChannelMemberResp member);
    
    void sendToChannelMembers(Long channelId, Object message);
}
