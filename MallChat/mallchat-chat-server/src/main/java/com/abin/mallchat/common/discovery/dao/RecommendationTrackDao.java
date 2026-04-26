package com.abin.mallchat.common.discovery.dao;

import com.abin.mallchat.common.discovery.domain.entity.RecommendationTrack;
import com.abin.mallchat.common.discovery.mapper.RecommendationTrackMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RecommendationTrackDao extends ServiceImpl<RecommendationTrackMapper, RecommendationTrack> {
    
    public List<RecommendationTrack> getByUidAndType(Long uid, Integer recommendType) {
        return lambdaQuery()
                .eq(RecommendationTrack::getUid, uid)
                .eq(RecommendationTrack::getRecommendType, recommendType)
                .orderByDesc(RecommendationTrack::getCreateTime)
                .last("LIMIT 100")
                .list();
    }
    
    public RecommendationTrack getByUidAndGuildId(Long uid, Long guildId) {
        return lambdaQuery()
                .eq(RecommendationTrack::getUid, uid)
                .eq(RecommendationTrack::getGuildId, guildId)
                .orderByDesc(RecommendationTrack::getCreateTime)
                .last("LIMIT 1")
                .one();
    }
    
    public Long countClicksByUid(Long uid) {
        return lambdaQuery()
                .eq(RecommendationTrack::getUid, uid)
                .eq(RecommendationTrack::getIsClicked, 1)
                .count()
                .longValue();
    }
    
    public Long countJoinsByUid(Long uid) {
        return lambdaQuery()
                .eq(RecommendationTrack::getUid, uid)
                .eq(RecommendationTrack::getIsJoined, 1)
                .count()
                .longValue();
    }
    
    public Long countTotalByUid(Long uid) {
        return lambdaQuery().eq(RecommendationTrack::getUid, uid).count().longValue();
    }
}
