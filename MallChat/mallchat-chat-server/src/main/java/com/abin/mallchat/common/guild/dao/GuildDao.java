package com.abin.mallchat.common.guild.dao;

import com.abin.mallchat.common.guild.domain.entity.Guild;
import com.abin.mallchat.common.guild.domain.entity.GuildMember;
import com.abin.mallchat.common.guild.mapper.GuildMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class GuildDao extends ServiceImpl<GuildMapper, Guild> {
    
    @Autowired
    private GuildMemberDao guildMemberDao;
    
    public List<Guild> getGuildsByUid(Long uid) {
        List<GuildMember> members = guildMemberDao.lambdaQuery()
                .eq(GuildMember::getUid, uid)
                .eq(GuildMember::getStatus, 1)
                .list();
        
        if (members.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Long> guildIds = members.stream()
                .map(GuildMember::getGuildId)
                .collect(Collectors.toList());
        
        return lambdaQuery()
                .eq(Guild::getStatus, 1)
                .in(Guild::getId, guildIds)
                .orderByDesc(Guild::getCreateTime)
                .list();
    }
}
