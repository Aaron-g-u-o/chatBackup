package com.abin.mallchat.common.guild.dao;

import com.abin.mallchat.common.guild.domain.entity.Guild;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.abin.mallchat.common.guild.mapper.GuildMemberMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GuildMemberDao extends ServiceImpl<GuildMemberMapper, GuildMember> {
    
    public List<GuildMember> getMembersByGuildId(Long guildId) {
        return lambdaQuery()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getStatus, 1)
                .list();
    }
    
    public GuildMember getMember(Long guildId, Long uid) {
        return lambdaQuery()
                .eq(GuildMember::getGuildId, guildId)
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .one();
    }
    
    public void removeMember(Long channelId, Long uid) {
        lambdaUpdate()
                .eq(GuildMember::getGuildId, channelId)
                .eq(GuildMember::getUid, uid)
                .set(GuildMember::getStatus, 0)
                .update();
    }
}
