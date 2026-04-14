package com.abin.mallchat.common.guild.dao;

import com.abin.mallchat.common.guild.domain.entity.Channel;
import com.abin.mallchat.common.guild.mapper.ChannelMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ChannelDao extends ServiceImpl<ChannelMapper, Channel> {
    
    public List<Channel> getChannelsByGuildId(Long guildId) {
        return lambdaQuery()
                .eq(Channel::getGuildId, guildId)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getPosition)
                .list();
    }
    
    public List<Channel> getVoiceChannels(Long guildId) {
        return lambdaQuery()
                .eq(Channel::getGuildId, guildId)
                .eq(Channel::getType, 2)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getPosition)
                .list();
    }
    
    public List<Channel> getTextChannels(Long guildId) {
        return lambdaQuery()
                .eq(Channel::getGuildId, guildId)
                .eq(Channel::getType, 1)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getPosition)
                .list();
    }
}
