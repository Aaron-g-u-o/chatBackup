package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.GuildTagRelation;
import com.abin.mallchat.common.discovery.mapper.GuildTagRelationMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class GuildTagRelationDao extends ServiceImpl<GuildTagRelationMapper, GuildTagRelation> {
    
    public List<GuildTagRelation> getByGuildId(Long guildId) {
        return lambdaQuery().eq(GuildTagRelation::getGuildId, guildId).list();
    }
    
    public List<Long> getGuildIdsByTagId(Long tagId) {
        return lambdaQuery()
                .eq(GuildTagRelation::getTagId, tagId)
                .list()
                .stream()
                .map(GuildTagRelation::getGuildId)
                .collect(Collectors.toList());
    }
    
    public List<Long> getGuildIdsByTagIds(List<Long> tagIds) {
        return lambdaQuery()
                .in(GuildTagRelation::getTagId, tagIds)
                .list()
                .stream()
                .map(GuildTagRelation::getGuildId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public void removeByGuildId(Long guildId) {
        lambdaUpdate().eq(GuildTagRelation::getGuildId, guildId).remove();
    }
}
