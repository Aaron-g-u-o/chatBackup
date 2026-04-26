package com.abin.mallchat.common.discovery.service;

import com.abin.mallchat.common.discovery.domain.vo.request.DismissReq;
import com.abin.mallchat.common.discovery.domain.vo.request.DiscoveryReq;
import com.abin.mallchat.common.discovery.domain.vo.request.TrackClickReq;
import com.abin.mallchat.common.discovery.domain.vo.response.DiscoveryPageResp;
import com.abin.mallchat.common.discovery.domain.vo.response.RecommendationMetricsResp;

public interface RecommendationService {
    
    DiscoveryPageResp getRecommendations(Long uid, DiscoveryReq req);
    
    void trackClick(Long uid, TrackClickReq req);
    
    void trackJoin(Long uid, Long guildId);
    
    void dismissRecommendation(Long uid, DismissReq req);
    
    RecommendationMetricsResp getMetrics(Long uid);
    
    void refreshDailyRecommendations();
    
    void refreshTrendingScores();
}
