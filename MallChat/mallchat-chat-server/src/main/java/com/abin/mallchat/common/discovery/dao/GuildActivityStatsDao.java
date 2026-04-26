package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.GuildActivityStats;
import com.abin.mallchat.common.discovery.mapper.GuildActivityStatsMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class GuildActivityStatsDao extends ServiceImpl<GuildActivityStatsMapper, GuildActivityStats> {
    
    public GuildActivityStats getByGuildId(Long guildId) {
        return lambdaQuery().eq(GuildActivityStats::getGuildId, guildId).one();
    }
    
    public List<GuildActivityStats> getTrendingGuilds(int limit) {
        return lambdaQuery()
                .orderByDesc(GuildActivityStats::getTrendingScore)
                .last("LIMIT " + limit)
                .list();
    }
    
    public List<GuildActivityStats> getPopularGuilds(int limit) {
        return lambdaQuery()
                .orderByDesc(GuildActivityStats::getWeeklyActiveUsers)
                .last("LIMIT " + limit)
                .list();
    }
}
