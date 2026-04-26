package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.GuildTag;
import com.abin.mallchat.common.discovery.mapper.GuildTagMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class GuildTagDao extends ServiceImpl<GuildTagMapper, GuildTag> {
    
    public List<GuildTag> getByCategory(String category) {
        return lambdaQuery().eq(GuildTag::getCategory, category).list();
    }
    
    public List<GuildTag> getAllTags() {
        return lambdaQuery().orderByAsc(GuildTag::getCategory).orderByDesc(GuildTag::getWeight).list();
    }
    
    public GuildTag getByName(String name) {
        return lambdaQuery().eq(GuildTag::getName, name).one();
    }
    
    public List<String> getAllCategories() {
        return lambdaQuery()
                .select(GuildTag::getCategory)
                .groupBy(GuildTag::getCategory)
                .list()
                .stream()
                .map(GuildTag::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
}
